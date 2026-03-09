package eu.darken.myperm.common.debug.recording.core

import android.content.Context
import android.os.Build
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.InstallId
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.Logging.Priority.INFO
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.DynamicStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.annotation.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecorderModule @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val installId: InstallId,
) {

    private val triggerFile = try {
        File(context.getExternalFilesDir(null), FORCE_FILE)
    } catch (e: Exception) {
        File(
            Environment.getExternalStorageDirectory(),
            "/Android/data/${BuildConfigWrap.APPLICATION_ID}/files/$FORCE_FILE"
        )
    }

    @Volatile
    internal var currentLogDir: File? = null
        private set

    private val internalState = DynamicStateFlow(TAG, appScope + dispatcherProvider.IO) {
        val triggerFileExists = triggerFile.exists()
        State(shouldRecord = triggerFileExists)
    }
    val state: Flow<State> = internalState.flow

    init {
        internalState.flow
            .map { it.shouldRecord }
            .distinctUntilChanged()
            .onEach { shouldRecord ->
                log(TAG) { "shouldRecord changed: $shouldRecord" }

                internalState.updateBlocking {
                    if (shouldRecord && !isRecording) {
                        val existingDir = findExistingSessionDir(getLogDirectories())
                        val sessionDir = existingDir ?: createSessionDir()

                        if (existingDir != null) {
                            log(TAG, INFO) { "Resuming recording in existing session: ${existingDir.name}" }
                        }

                        val logFile = File(sessionDir, "core.log")
                        val newRecorder = Recorder()
                        newRecorder.start(logFile)
                        triggerFile.createNewFile()

                        log(TAG, INFO) { "Build.Fingerprint: ${Build.FINGERPRINT}" }
                        log(TAG, INFO) { "BuildConfig.Versions: ${BuildConfigWrap.VERSION_DESCRIPTION}" }

                        currentLogDir = sessionDir

                        copy(
                            recorder = newRecorder,
                            recordingStartedAt = if (existingDir != null) {
                                existingDir.lastModified()
                            } else {
                                System.currentTimeMillis()
                            },
                            logDir = sessionDir,
                        )
                    } else if (!shouldRecord && isRecording) {
                        recorder?.stop()

                        if (triggerFile.exists() && !triggerFile.delete()) {
                            log(TAG, ERROR) { "Failed to delete trigger file" }
                        }

                        currentLogDir = null

                        copy(
                            recorder = null,
                            recordingStartedAt = 0L,
                            logDir = null,
                        )
                    } else {
                        this
                    }
                }
            }
            .launchIn(appScope)
    }

    private fun createSessionDir(): File {
        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = sdf.format(Date())
        val installIdPrefix = installId.id.take(8)
        val dirName = "myperm_${BuildConfigWrap.VERSION_NAME}_${timestamp}_$installIdPrefix"

        val primaryParent = try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null) {
                val dir = File(externalDir, "debug/logs")
                dir.mkdirs()
                if (dir.canWrite()) dir else null
            } else {
                null
            }
        } catch (e: Exception) {
            log(TAG, WARN) { "External files dir unavailable: $e" }
            null
        }

        val parent = primaryParent ?: File(context.cacheDir, "debug/logs").also { it.mkdirs() }
        val sessionDir = File(parent, dirName)
        if (!sessionDir.mkdirs() && !sessionDir.exists()) {
            throw java.io.IOException("Failed to create session directory: $sessionDir")
        }

        log(TAG) { "Created session dir: $sessionDir" }
        return sessionDir
    }

    internal fun getLogDirectories(): List<File> = listOfNotNull(
        try {
            context.getExternalFilesDir(null)?.let { File(it, "debug/logs") }
        } catch (e: Exception) {
            null
        },
        File(context.cacheDir, "debug/logs"),
    )

    suspend fun startRecorder(): File {
        internalState.updateBlocking {
            copy(shouldRecord = true)
        }
        return internalState.flow.filter { it.isRecording }.first().logDir!!
    }

    sealed class StopResult {
        data object TooShort : StopResult()
        data class Stopped(val logDir: File, val sessionId: String) : StopResult()
        data object NotRecording : StopResult()
    }

    suspend fun requestStopRecorder(): StopResult {
        val currentState = internalState.value()
        if (!currentState.isRecording) return StopResult.NotRecording

        val duration = System.currentTimeMillis() - currentState.recordingStartedAt
        if (duration < MIN_RECORDING_MS) return StopResult.TooShort

        val logDir = stopRecorder() ?: return StopResult.NotRecording
        val sessionId = DebugSessionManager.deriveSessionId(logDir)
        return StopResult.Stopped(logDir, sessionId)
    }

    suspend fun stopRecorder(): File? {
        val dir = currentLogDir ?: return null
        internalState.updateBlocking {
            copy(shouldRecord = false)
        }
        internalState.flow.filter { !it.isRecording }.first()
        return dir
    }

    data class State(
        val shouldRecord: Boolean = false,
        internal val recorder: Recorder? = null,
        val recordingStartedAt: Long = 0L,
        val logDir: File? = null,
    ) {
        val isRecording: Boolean
            get() = recorder != null
    }

    companion object {
        internal val TAG = logTag("Debug", "Log", "Recorder", "Module")
        private const val FORCE_FILE = "myperm_force_debug_run"
        internal const val MIN_RECORDING_MS = 5_000L

        @VisibleForTesting
        internal fun findExistingSessionDir(logDirectories: List<File>): File? {
            for (parent in logDirectories) {
                if (!parent.exists()) continue
                val dirs = parent.listFiles { f -> f.isDirectory && f.name.startsWith("myperm_") }
                    ?: continue
                val mostRecent = dirs.maxByOrNull { it.lastModified() } ?: continue
                val coreLog = File(mostRecent, "core.log")
                if (coreLog.exists()) return mostRecent
            }
            return null
        }
    }
}
