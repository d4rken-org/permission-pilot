package eu.darken.myperm.common.debug.recording.core

import android.net.Uri
import androidx.annotation.VisibleForTesting
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.Logging.Priority.INFO
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.replayingShare
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val pendingAutoZips: MutableSet<String> = java.util.Collections.synchronizedSet(mutableSetOf())

    val recorderState: Flow<RecorderModule.State> get() = recorderModule.state

    val sessions: Flow<List<DebugSession>> = combine(
        recorderModule.state,
        zippingIds,
        failedZipIds,
        refreshTrigger.onStart { emit(Unit) },
    ) { recorderState, zipping, failedZips, _ ->
        val activeDir = recorderModule.currentLogDir
        val scanned = scanSessions(recorderModule.getLogDirectories())
        applyOverlays(scanned, activeDir, recorderState, zipping, failedZips)
    }.replayingShare(appScope)

    init {
        sessions.onEach { allSessions ->
            val orphans = findOrphans(allSessions, zippingIds.value)
            for ((id, dir) in orphans) {
                if (pendingAutoZips.add(id)) {
                    log(TAG, INFO) { "Orphan session detected, auto-zipping: $id" }
                    zipSessionAsync(id, dir)
                }
            }
        }.launchIn(appScope)
    }

    private fun findOrphans(
        sessions: List<DebugSession>,
        zipping: Set<String>,
    ): List<Pair<String, File>> {
        return sessions.filterIsInstance<DebugSession.Ready>()
            .filter { it.logDir != null && it.id !in zipping && it.id !in pendingAutoZips && it.id !in failedZipIds.value }
            .filter { it.zipFile == null || it.compressedSize == 0L }
            .map { it.id to it.logDir!! }
    }

    private fun zipSessionAsync(sessionId: String, logDir: File) {
        zippingIds.update { it + sessionId }
        appScope.launch(dispatcherProvider.IO) {
            try {
                fsMutex.withLock {
                    debugLogZipper.zip(logDir)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log(TAG, ERROR) { "Zipping failed for $sessionId: $e" }
                failedZipIds.update { it + sessionId }
            } finally {
                pendingAutoZips.remove(sessionId)
                zippingIds.update { it - sessionId }
                refresh()
            }
        }
    }

    private fun scanSessions(logDirs: List<File>): List<ScannedEntry> {
        return logDirs.flatMap { dir ->
            if (!dir.exists()) return@flatMap emptyList()
            val entries = dir.listFiles() ?: return@flatMap emptyList()
            entries.mapNotNull { entry ->
                when {
                    entry.isDirectory -> {
                        val id = deriveId(entry)
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
                        val id = deriveId(entry)
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
            val coreLog = File(logDir, "core.log")

            if (!coreLog.exists()) {
                return DebugSession.Failed(
                    id = entry.id,
                    displayName = entry.displayName,
                    createdAt = entry.createdAt,
                    diskSize = computeDiskSize(logDir, zipFile),
                    path = logDir,
                    reason = DebugSession.Failed.Reason.MISSING_LOG,
                )
            }

            if (coreLog.length() == 0L) {
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
                compressedSize = zipFile?.length() ?: 0L,
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

    suspend fun requestStopRecording(): RecorderModule.StopResult {
        val result = recorderModule.requestStopRecorder()
        if (result is RecorderModule.StopResult.Stopped) {
            val sessionId = deriveId(result.logDir)
            zipSessionAsync(sessionId, result.logDir)
            return result.copy(sessionId = sessionId)
        }
        return result
    }

    suspend fun forceStopRecording(): RecorderModule.StopResult.Stopped? {
        val logDir = recorderModule.stopRecorder() ?: return null
        val sessionId = deriveId(logDir)
        zipSessionAsync(sessionId, logDir)
        return RecorderModule.StopResult.Stopped(logDir, sessionId)
    }

    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    private fun deriveId(file: File): String = deriveSessionId(
        file, recorderModule.externalLogDir, recorderModule.cacheLogDir
    )

    private fun activeSessionId(): String? = recorderModule.currentLogDir?.let { deriveId(it) }

    suspend fun zipSession(sessionId: String): File = fsMutex.withLock {
        require(activeSessionId() != sessionId) { "Cannot zip an active recording session" }

        val (dir, existingZip) = findSessionFiles(sessionId)

        if (existingZip != null && existingZip.length() > 0) {
            if (dir == null || existingZip.lastModified() >= dir.lastModified()) {
                return@withLock existingZip
            }
        }

        requireNotNull(dir) { "No log directory found for session $sessionId" }
        withContext(dispatcherProvider.IO) {
            debugLogZipper.zip(dir)
        }
    }

    suspend fun getZipUri(sessionId: String): Uri {
        val zipFile = zipSession(sessionId)
        return debugLogZipper.getUriForZip(zipFile)
    }

    suspend fun deleteSession(sessionId: String) = fsMutex.withLock {
        if (activeSessionId() == sessionId) {
            log(TAG, WARN) { "deleteSession($sessionId): is active recording, skipping" }
            return@withLock
        }
        if (sessionId in zippingIds.value) {
            log(TAG, WARN) { "deleteSession($sessionId): currently zipping, skipping" }
            return@withLock
        }

        withContext(dispatcherProvider.IO) {
            val (dir, zip) = findSessionFiles(sessionId)
            if (dir?.deleteRecursively() == false) {
                log(TAG, WARN) { "Failed to fully delete session dir: ${dir.path}" }
            }
            if (zip?.delete() == false) {
                log(TAG, WARN) { "Failed to delete session zip: ${zip.path}" }
            }
        }
        failedZipIds.update { it - sessionId }

        log(TAG) { "Deleted session: $sessionId" }
        refresh()
    }

    suspend fun deleteAllSessions() = fsMutex.withLock {
        val activeDir = recorderModule.currentLogDir
        val currentlyZipping = zippingIds.value
        withContext(dispatcherProvider.IO) {
            for (dir in recorderModule.getLogDirectories()) {
                if (!dir.exists()) continue
                for (entry in dir.listFiles() ?: emptyArray()) {
                    if (entry == activeDir) {
                        log(TAG) { "Skipping active session dir: $entry" }
                        continue
                    }
                    val entryId = deriveId(entry)
                    if (entryId in currentlyZipping) {
                        log(TAG) { "Skipping zipping session: $entry" }
                        continue
                    }
                    val deleted = if (entry.isDirectory) entry.deleteRecursively() else entry.delete()
                    if (!deleted) log(TAG, WARN) { "Failed to delete: ${entry.path}" }
                }
            }
        }
        failedZipIds.update { emptySet() }
        pendingAutoZips.clear()
        log(TAG) { "All stored logs deleted" }
        refresh()
    }

    private fun findSessionFiles(sessionId: String): Pair<File?, File?> {
        val (prefix, baseName) = parseSessionId(sessionId)

        val targetDir = when (prefix) {
            "ext" -> recorderModule.externalLogDir
            "cache" -> recorderModule.cacheLogDir
            else -> null
        }
        val allLogDirs = recorderModule.getLogDirectories()
        val searchDirs = if (targetDir != null) {
            listOf(targetDir) + allLogDirs.filter { it.absolutePath != targetDir.absolutePath }
        } else {
            allLogDirs
        }

        for (logParent in searchDirs) {
            if (!logParent.exists()) continue
            val dir = File(logParent, baseName)
            val zip = File(logParent, "$baseName.zip")
            val dirExists = dir.exists() && dir.isDirectory
            val zipExists = zip.exists() && zip.isFile
            if (dirExists || zipExists) {
                return Pair(if (dirExists) dir else null, if (zipExists) zip else null)
            }
        }
        return Pair(null, null)
    }

    companion object {
        private val TAG = logTag("Debug", "Log", "Session", "Manager")

        private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC)

        private val TIMESTAMP_REGEX = Regex("(\\d{8}T\\d{6}Z)")

        internal fun deriveBaseName(file: File): String {
            return if (file.extension == "zip") file.nameWithoutExtension else file.name
        }

        @VisibleForTesting
        internal fun deriveSessionId(file: File, externalLogDir: File?, cacheLogDir: File): String {
            val baseName = deriveBaseName(file)
            val parent = file.parentFile ?: return baseName
            val parentPath = parent.absolutePath
            if (externalLogDir != null && externalLogDir.absolutePath == parentPath) return "ext:$baseName"
            if (cacheLogDir.absolutePath == parentPath) return "cache:$baseName"
            return baseName
        }

        internal fun parseSessionId(sessionId: String): Pair<String?, String> {
            val colonIdx = sessionId.indexOf(':')
            return if (colonIdx >= 0) {
                sessionId.substring(0, colonIdx) to sessionId.substring(colonIdx + 1)
            } else {
                null to sessionId
            }
        }

        @VisibleForTesting
        internal fun parseCreatedAt(name: String): Instant {
            // Find timestamp via regex to handle version names with underscores
            val match = TIMESTAMP_REGEX.find(name)
            if (match != null) {
                try {
                    return TIMESTAMP_FORMAT.parse(match.groupValues[1], Instant::from)
                } catch (_: Exception) {
                    // Fall through
                }
            }

            // Legacy: millis-based timestamp in third segment
            val parts = name.split("_")
            if (parts.size >= 3) {
                try {
                    return Instant.ofEpochMilli(parts[2].toLong())
                } catch (_: Exception) {
                    // Fall through
                }
            }
            return Instant.EPOCH
        }

        @VisibleForTesting
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
