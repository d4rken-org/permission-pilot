package eu.darken.myperm.common.debug.recording.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val recorderModule: RecorderModule,
) {

    sealed interface LogSession {
        val path: File
        val size: Long
        val lastModified: Long
        val fileCount: Int

        data class Recording(
            override val path: File,
            override val size: Long,
            override val lastModified: Long,
            override val fileCount: Int,
        ) : LogSession

        data class Ready(
            override val path: File,
            override val size: Long,
            override val lastModified: Long,
            override val fileCount: Int,
        ) : LogSession

        data class Failed(
            override val path: File,
            override val size: Long,
            override val lastModified: Long,
            override val fileCount: Int,
        ) : LogSession
    }

    val recorderState: Flow<RecorderModule.State> = recorderModule.state

    private val _sessions = MutableStateFlow<List<LogSession>>(emptyList())
    val sessions: Flow<List<LogSession>> = _sessions.asStateFlow()

    init {
        recorderModule.state
            .onEach {
                log(TAG) { "Recorder state changed, refreshing sessions" }
                refreshSessions()
            }
            .launchIn(appScope + dispatcherProvider.IO)
    }

    suspend fun startRecorder(): File = recorderModule.startRecorder()

    suspend fun stopRecorder(showResultUi: Boolean = true): File? = recorderModule.stopRecorder(showResultUi)

    suspend fun stopActiveRecording() {
        recorderModule.stopRecorder(showResultUi = false)
    }

    suspend fun deleteLogSession(session: LogSession) = withContext(dispatcherProvider.IO) {
        log(TAG) { "deleteLogSession(${session.path})" }
        val path = session.path
        if (path.isDirectory) {
            path.deleteRecursively()
            val zip = File(path.parentFile, "${path.name}.zip")
            if (zip.exists()) zip.delete()
        } else {
            path.delete()
        }
        refreshSessions()
    }

    suspend fun deleteAllLogs() = withContext(dispatcherProvider.IO) {
        val activeDir = recorderModule.getActiveLogDir()
        recorderModule.getLogDirectories().forEach { dir ->
            if (!dir.exists()) return@forEach
            dir.listFiles()?.forEach { entry ->
                if (entry == activeDir) {
                    log(TAG) { "Skipping active session dir: $entry" }
                    return@forEach
                }
                if (entry.isDirectory) {
                    entry.deleteRecursively()
                } else {
                    entry.delete()
                }
            }
        }
        log(TAG) { "All stored logs deleted" }
        refreshSessions()
    }

    internal suspend fun refreshSessions() = withContext(dispatcherProvider.IO) {
        val activeDir = recorderModule.getActiveLogDir()
        _sessions.value = scanSessions(activeDir)
    }

    private fun scanSessions(activeDir: File?): List<LogSession> {
        return recorderModule.getLogDirectories()
            .flatMap { dir ->
                if (!dir.exists()) return@flatMap emptyList()
                val entries = dir.listFiles() ?: return@flatMap emptyList()
                entries.filter { entry ->
                    entry.isDirectory ||
                            (entry.isFile && (entry.extension == "zip" || entry.extension == "log"))
                }.map { entry ->
                    val (size, fileCount) = measureEntry(entry)
                    when {
                        entry == activeDir -> LogSession.Recording(
                            path = entry,
                            size = size,
                            lastModified = entry.lastModified(),
                            fileCount = fileCount,
                        )

                        isValidSession(entry, size) -> LogSession.Ready(
                            path = entry,
                            size = size,
                            lastModified = entry.lastModified(),
                            fileCount = fileCount,
                        )

                        else -> LogSession.Failed(
                            path = entry,
                            size = size,
                            lastModified = entry.lastModified(),
                            fileCount = fileCount,
                        )
                    }
                }
            }
            .sortedWith(
                compareBy<LogSession> { it !is LogSession.Recording }
                    .thenByDescending { it.lastModified }
            )
    }

    private fun measureEntry(entry: File): Pair<Long, Int> {
        return if (entry.isDirectory) {
            val files = entry.walkTopDown().filter { it.isFile }.toList()
            files.sumOf { it.length() } to files.size
        } else {
            entry.length() to 1
        }
    }

    private fun isValidSession(entry: File, size: Long): Boolean {
        return when {
            entry.isDirectory -> {
                entry.walkTopDown()
                    .filter { it.isFile && it.extension == "log" }
                    .any { it.length() > 0 }
            }

            entry.isFile && entry.extension == "zip" -> size > 0
            entry.isFile && entry.extension == "log" -> size > 0
            else -> false
        }
    }

    companion object {
        private val TAG = logTag("Debug", "Log", "SessionManager")
    }
}
