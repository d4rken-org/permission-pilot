package eu.darken.myperm.common.debug.recording.core

import android.net.Uri
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.replayingShare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugSessionManager @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val recorderModule: RecorderModule,
    private val debugLogZipper: DebugLogZipper,
) {

    private val fsMutex = Mutex()
    private val zippingIds = MutableStateFlow<Set<String>>(emptySet())
    private val failedZipIds = MutableStateFlow<Set<String>>(emptySet())
    private val refreshTrigger = MutableStateFlow(0)

    val recorderState: Flow<RecorderModule.State> = recorderModule.state

    val sessions: Flow<List<DebugSession>> = combine(
        recorderModule.state,
        zippingIds,
        failedZipIds,
        refreshTrigger,
    ) { recorderState, zipping, failedZips, _ ->
        val activeDir = recorderModule.currentLogDir
        val scanned = scanSessions(recorderModule.getLogDirectories())
        applyOverlays(scanned, activeDir, recorderState, zipping, failedZips)
    }.replayingShare(appScope + dispatcherProvider.IO)

    private fun scanSessions(logDirs: List<File>): List<ScannedEntry> {
        return logDirs.flatMap { dir ->
            if (!dir.exists()) return@flatMap emptyList()
            val entries = dir.listFiles() ?: return@flatMap emptyList()
            entries.mapNotNull { entry ->
                when {
                    entry.isDirectory -> {
                        val id = deriveSessionId(entry)
                        val zipFile = File(entry.parentFile, "${entry.name}.zip")
                        ScannedEntry(
                            id = id,
                            displayName = entry.name,
                            createdAt = parseCreatedAt(entry.name),
                            logDir = entry,
                            zipFile = if (zipFile.exists()) zipFile else null,
                        )
                    }
                    entry.isFile && entry.extension == "zip" -> {
                        val baseName = entry.nameWithoutExtension
                        val hasDir = File(entry.parentFile, baseName).isDirectory
                        if (hasDir) return@mapNotNull null // handled by dir entry
                        val id = deriveSessionId(entry)
                        ScannedEntry(
                            id = id,
                            displayName = baseName,
                            createdAt = parseCreatedAt(baseName),
                            logDir = null,
                            zipFile = entry,
                        )
                    }
                    else -> null
                }
            }
        }
    }

    private data class ScannedEntry(
        val id: String,
        val displayName: String,
        val createdAt: Instant,
        val logDir: File?,
        val zipFile: File?,
    )

    private fun applyOverlays(
        scanned: List<ScannedEntry>,
        activeDir: File?,
        recorderState: RecorderModule.State,
        zipping: Set<String>,
        failedZips: Set<String>,
    ): List<DebugSession> {
        return scanned.map { entry ->
            when {
                activeDir != null && entry.logDir == activeDir -> {
                    DebugSession.Recording(
                        id = entry.id,
                        displayName = entry.displayName,
                        createdAt = entry.createdAt,
                        diskSize = computeDiskSize(entry.logDir, entry.zipFile),
                        path = entry.logDir,
                        startedAt = recorderState.recordingStartedAt,
                    )
                }
                entry.id in zipping -> {
                    DebugSession.Compressing(
                        id = entry.id,
                        displayName = entry.displayName,
                        createdAt = entry.createdAt,
                        diskSize = computeDiskSize(entry.logDir, entry.zipFile),
                        path = entry.logDir ?: entry.zipFile!!,
                    )
                }
                entry.id in failedZips -> {
                    DebugSession.Failed(
                        id = entry.id,
                        displayName = entry.displayName,
                        createdAt = entry.createdAt,
                        diskSize = computeDiskSize(entry.logDir, entry.zipFile),
                        path = entry.logDir ?: entry.zipFile!!,
                        reason = DebugSession.Failed.Reason.ZIP_FAILED,
                    )
                }
                else -> classifyWithZip(entry)
            }
        }.sortedWith(
            compareBy<DebugSession> { it !is DebugSession.Recording }
                .thenByDescending { it.createdAt }
        )
    }

    private fun classifyWithZip(entry: ScannedEntry): DebugSession {
        val logDir = entry.logDir
        val zipFile = entry.zipFile

        // Orphan zip (no dir)
        if (logDir == null && zipFile != null) {
            return if (zipFile.length() > 0) {
                DebugSession.Ready(
                    id = entry.id,
                    displayName = entry.displayName,
                    createdAt = entry.createdAt,
                    diskSize = zipFile.length(),
                    zipFile = zipFile,
                    compressedSize = zipFile.length(),
                )
            } else {
                DebugSession.Failed(
                    id = entry.id,
                    displayName = entry.displayName,
                    createdAt = entry.createdAt,
                    diskSize = 0L,
                    path = zipFile,
                    reason = DebugSession.Failed.Reason.CORRUPT_ZIP,
                )
            }
        }

        // Dir exists
        if (logDir != null) {
            val logFiles = logDir.listFiles()?.filter { it.isFile } ?: emptyList()
            val hasContent = logFiles.any { it.extension == "log" && it.length() > 0 }

            if (logFiles.isEmpty()) {
                return DebugSession.Failed(
                    id = entry.id,
                    displayName = entry.displayName,
                    createdAt = entry.createdAt,
                    diskSize = computeDiskSize(logDir, zipFile),
                    path = logDir,
                    reason = DebugSession.Failed.Reason.MISSING_LOG,
                )
            }

            if (!hasContent) {
                return DebugSession.Failed(
                    id = entry.id,
                    displayName = entry.displayName,
                    createdAt = entry.createdAt,
                    diskSize = computeDiskSize(logDir, zipFile),
                    path = logDir,
                    reason = DebugSession.Failed.Reason.EMPTY_LOG,
                )
            }

            return DebugSession.Ready(
                id = entry.id,
                displayName = entry.displayName,
                createdAt = entry.createdAt,
                diskSize = computeDiskSize(logDir, zipFile),
                logDir = logDir,
                zipFile = zipFile,
                compressedSize = zipFile?.length() ?: -1L,
            )
        }

        // Should not happen
        return DebugSession.Failed(
            id = entry.id,
            displayName = entry.displayName,
            createdAt = entry.createdAt,
            diskSize = 0L,
            path = File("unknown"),
            reason = DebugSession.Failed.Reason.MISSING_LOG,
        )
    }

    suspend fun startRecording(): File = recorderModule.startRecorder()

    suspend fun requestStopRecording(): RecorderModule.StopResult = recorderModule.requestStopRecorder()

    suspend fun forceStopRecording(): File? = recorderModule.stopRecorder()

    fun refresh() {
        refreshTrigger.value++
    }

    suspend fun zipSession(sessionId: String) = withContext(dispatcherProvider.IO) {
        val logDir = fsMutex.withLock {
            findSessionLogDir(sessionId)
        }
        if (logDir == null) {
            log(TAG, WARN) { "zipSession($sessionId): logDir not found" }
            return@withContext
        }

        zippingIds.value = zippingIds.value + sessionId

        try {
            debugLogZipper.zip(logDir)
            failedZipIds.value = failedZipIds.value - sessionId
        } catch (e: Exception) {
            log(TAG, ERROR) { "zipSession($sessionId) failed: $e" }
            failedZipIds.value = failedZipIds.value + sessionId
        } finally {
            zippingIds.value = zippingIds.value - sessionId
        }
    }

    suspend fun getZipUri(sessionId: String): Uri? = withContext(dispatcherProvider.IO) {
        fsMutex.withLock {
            val logDir = findSessionLogDir(sessionId)
            val zipFile = if (logDir != null) {
                val existing = File(logDir.parentFile, "${logDir.name}.zip")
                if (existing.exists() && existing.length() > 0) {
                    existing
                } else {
                    try {
                        debugLogZipper.zip(logDir)
                    } catch (e: Exception) {
                        log(TAG, ERROR) { "getZipUri($sessionId) zip failed: $e" }
                        null
                    }
                }
            } else {
                findSessionZipFile(sessionId)
            }

            zipFile?.let { debugLogZipper.getUriForZip(it) }
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(dispatcherProvider.IO) {
        fsMutex.withLock {
            // Don't delete active recording
            if (recorderModule.currentLogDir?.let { deriveSessionId(it) } == sessionId) {
                log(TAG, WARN) { "deleteSession($sessionId): is active recording, skipping" }
                return@withContext
            }
            // Don't delete currently zipping
            if (sessionId in zippingIds.value) {
                log(TAG, WARN) { "deleteSession($sessionId): currently zipping, skipping" }
                return@withContext
            }
            val dirs = recorderModule.getLogDirectories()
            for (dir in dirs) {
                if (!dir.exists()) continue
                dir.listFiles()?.forEach { entry ->
                    if (deriveSessionId(entry) == sessionId) {
                        if (entry.isDirectory) {
                            entry.deleteRecursively()
                            val zip = File(entry.parentFile, "${entry.name}.zip")
                            if (zip.exists()) zip.delete()
                        } else {
                            entry.delete()
                        }
                    }
                }
            }
            failedZipIds.value = failedZipIds.value - sessionId
        }
        refresh()
    }

    suspend fun deleteAllSessions() = withContext(dispatcherProvider.IO) {
        val activeId = recorderModule.currentLogDir?.let { deriveSessionId(it) }
        val currentlyZipping = zippingIds.value

        fsMutex.withLock {
            recorderModule.getLogDirectories().forEach { dir ->
                if (!dir.exists()) return@forEach
                dir.listFiles()?.forEach { entry ->
                    val id = deriveSessionId(entry)
                    if (id == activeId || id in currentlyZipping) {
                        log(TAG) { "deleteAllSessions: skipping $id" }
                        return@forEach
                    }
                    if (entry.isDirectory) {
                        entry.deleteRecursively()
                        val zip = File(entry.parentFile, "${entry.name}.zip")
                        if (zip.exists()) zip.delete()
                    } else {
                        entry.delete()
                    }
                }
            }
            failedZipIds.value = emptySet()
        }
        log(TAG) { "All stored sessions deleted" }
        refresh()
    }

    private fun findSessionLogDir(sessionId: String): File? {
        return recorderModule.getLogDirectories().firstNotNullOfOrNull { dir ->
            if (!dir.exists()) return@firstNotNullOfOrNull null
            dir.listFiles()?.firstOrNull { it.isDirectory && deriveSessionId(it) == sessionId }
        }
    }

    private fun findSessionZipFile(sessionId: String): File? {
        return recorderModule.getLogDirectories().firstNotNullOfOrNull { dir ->
            if (!dir.exists()) return@firstNotNullOfOrNull null
            dir.listFiles()?.firstOrNull {
                it.isFile && it.extension == "zip" && deriveSessionId(it) == sessionId
            }
        }
    }

    companion object {
        private val TAG = logTag("Debug", "Log", "SessionManager")

        fun deriveSessionId(file: File): String {
            val name = if (file.extension == "zip") file.nameWithoutExtension else file.name
            return name
        }

        fun parseCreatedAt(name: String): Instant {
            // Try to parse UTC timestamp from dir name: myperm_VERSION_yyyyMMddTHHmmssZ_INSTALLID
            val parts = name.split("_")
            if (parts.size >= 3) {
                val timestampPart = parts[2]
                try {
                    val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    return sdf.parse(timestampPart)?.toInstant() ?: Instant.EPOCH
                } catch (_: Exception) {
                    // Fall through
                }
                // Legacy: millis-based timestamp
                try {
                    return Instant.ofEpochMilli(timestampPart.toLong())
                } catch (_: Exception) {
                    // Fall through
                }
            }
            return Instant.EPOCH
        }

        internal fun computeDiskSize(logDir: File?, zipFile: File?): Long {
            var size = 0L
            logDir?.let { dir ->
                if (dir.exists()) {
                    size += dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                }
            }
            zipFile?.let { zip ->
                if (zip.exists()) size += zip.length()
            }
            return size
        }
    }
}
