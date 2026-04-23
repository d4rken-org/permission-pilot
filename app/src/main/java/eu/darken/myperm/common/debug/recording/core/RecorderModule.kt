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

    // Lazy to keep Hilt construction off the filesystem — getExternalFilesDir()
    // does mkdirs and can ANR on main thread during App.onCreate on slow devices.
    private val triggerFile: File by lazy {
        try {
            File(context.getExternalFilesDir(null), FORCE_FILE)
        } catch (e: Exception) {
            File(
                Environment.getExternalStorageDirectory(),
                "/Android/data/${BuildConfigWrap.APPLICATION_ID}/files/$FORCE_FILE"
            )
        }
    }

    @Volatile
    internal var currentLogDir: File? = null
        private set

    private val internalState = DynamicStateFlow(TAG, appScope + dispatcherProvider.IO) {
        val triggerFileExists = triggerFile.exists()
        val persistedInfo = if (triggerFileExists) readTriggerFile() else null
        State(
            shouldRecord = triggerFileExists,
            persistedLogDir = persistedInfo?.logDir,
            recordingStartedAt = persistedInfo?.startedAt ?: 0L,
        )
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
                        val resumed = persistedLogDir?.takeIf { it.exists() && it.isDirectory }
                        val sessionDir = resumed ?: createSessionDir()
                        val startTime = if (resumed != null) {
                            log(TAG, INFO) { "Resuming recording in existing session: ${resumed.name}" }
                            recordingStartedAt
                        } else {
                            System.currentTimeMillis()
                        }

                        val logFile = File(sessionDir, "core.log")
                        val newRecorder = Recorder()
                        newRecorder.start(logFile)
                        writeTriggerFile(sessionDir, startTime)

                        log(TAG, INFO) { "Build.Fingerprint: ${Build.FINGERPRINT}" }
                        log(TAG, INFO) { "BuildConfig.Versions: ${BuildConfigWrap.VERSION_DESCRIPTION}" }

                        currentLogDir = sessionDir

                        copy(
                            recorder = newRecorder,
                            persistedLogDir = null,
                            recordingStartedAt = startTime,
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
                            persistedLogDir = null,
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

    internal val externalLogDir: File? by lazy {
        try {
            context.getExternalFilesDir(null)?.let { File(it, "debug/logs") }
        } catch (e: Exception) {
            null
        }
    }

    internal val cacheLogDir: File = File(context.cacheDir, "debug/logs")

    internal fun getLogDirectories(): List<File> = listOfNotNull(externalLogDir, cacheLogDir)

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
        val sessionId = DebugSessionManager.deriveBaseName(logDir)
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
        internal val persistedLogDir: File? = null,
    ) {
        val isRecording: Boolean
            get() = recorder != null
    }

    data class TriggerInfo(val logDir: File, val startedAt: Long)

    internal fun readTriggerFile(): TriggerInfo? {
        return try {
            val content = triggerFile.readText()
            parseTriggerContent(content)
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to read trigger file: $e" }
            null
        }
    }

    private fun writeTriggerFile(sessionDir: File, startTime: Long) {
        try {
            triggerFile.writeText("${sessionDir.absolutePath}\n$startTime")
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to write trigger file metadata: $e" }
            try {
                triggerFile.createNewFile()
            } catch (e2: Exception) {
                log(TAG, ERROR) { "Failed to create trigger file: $e2" }
            }
        }
    }

    companion object {
        internal val TAG = logTag("Debug", "Log", "Recorder", "Module")
        private const val FORCE_FILE = "myperm_force_debug_run"
        internal const val MIN_RECORDING_MS = 5_000L

        @VisibleForTesting
        internal fun parseTriggerContent(content: String): TriggerInfo? {
            if (content.isBlank()) return null
            val lines = content.trim().lines()
            if (lines.size != 2) return null

            val dir = File(lines[0])
            if (!dir.exists() || !dir.isDirectory) return null

            val timestamp = lines[1].toLongOrNull() ?: return null
            val now = System.currentTimeMillis()
            if (timestamp < 1 || timestamp > now + 60_000L) return null

            return TriggerInfo(logDir = dir, startedAt = timestamp)
        }
    }
}
