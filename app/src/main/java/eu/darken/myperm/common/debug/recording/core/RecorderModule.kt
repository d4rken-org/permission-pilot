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
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.DynamicStateFlow
import eu.darken.myperm.common.upgrade.UpgradeDiagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    private val upgradeDiagnostics: UpgradeDiagnostics,
) {

    // Test seam: the header read below is bounded on real dispatchers, so a virtual-time test cannot
    // advance the production bound. Same pattern as BillingCache.cacheTimeoutMs.
    internal var headerReadTimeoutMs: Long = HEADER_READ_TIMEOUT_MS

    // Test seams for the two clocks the recording heuristics use. Same pattern as the header bound:
    // the durations are wall-clock/monotonic, so virtual time cannot drive them.
    internal var wallClock: () -> Long = System::currentTimeMillis
    internal var monotonicClock: () -> Long = android.os.SystemClock::elapsedRealtime

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
                            wallClock()
                        }

                        val logFile = File(sessionDir, "core.log")
                        val newRecorder = Recorder()
                        newRecorder.start(logFile)
                        writeTriggerFile(sessionDir, startTime)

                        try {
                            logRecordingHeader()
                        } catch (e: Exception) {
                            // The recorder is already live but not yet committed to the state: an
                            // exception escaping the header would abandon it where stopRecorder()
                            // can't reach it.
                            withContext(NonCancellable) {
                                try {
                                    newRecorder.stop()
                                } catch (stopError: Exception) {
                                    e.addSuppressed(stopError)
                                }
                                currentLogDir = null
                            }
                            throw e
                        }

                        currentLogDir = sessionDir

                        copy(
                            recorder = newRecorder,
                            persistedLogDir = null,
                            recordingStartedAt = startTime,
                            recordingStartedAtMonotonic = if (resumed != null) null else monotonicClock(),
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
                            recordingStartedAtMonotonic = null,
                            logDir = null,
                        )
                    } else {
                        this
                    }
                }
            }
            .launchIn(appScope)
    }

    // Header lines written into a freshly started recording. Runs AFTER the recorder is live, so
    // every read here is diagnostics-only and must never propagate: a failure would abort the state
    // update and leave a RUNNING recorder that the module no longer knows about.
    private suspend fun logRecordingHeader() {
        log(TAG, INFO) { "Build.Fingerprint: ${Build.FINGERPRINT}" }
        log(TAG, INFO) { "BuildConfig.Versions: ${BuildConfigWrap.VERSION_DESCRIPTION}" }

        try {
            // Diagnostics only — a broken read must not stop the recorder from starting. Bounded on
            // top of that: debug recording is what a user reaches for when the app is ALREADY
            // misbehaving, so a source that never answers (a stuck DataStore file lock, a billing
            // store that doesn't respond) must not hold up the start of the recording either.
            val read = withTimeoutOrNull(headerReadTimeoutMs) { HeaderRead(upgradeDiagnostics.debugInfo()) }
            when {
                read == null -> log(TAG, WARN) {
                    "Upgrade diagnostics unavailable, read did not finish within ${headerReadTimeoutMs}ms"
                }
                // Completion is tracked separately from the value: a flavor that legitimately has
                // nothing to report (FOSS) returns null and gets no line at all, not an "unavailable".
                read.value != null -> log(TAG, INFO) { "Upgrade diagnostics: ${read.value}" }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(TAG, WARN) { "Upgrade diagnostics unavailable: ${e.asLog()}" }
        }
    }

    /**
     * Completion marker for a header read: tells a source that legitimately has nothing to report
     * (no diagnostics on FOSS) apart from one that never answered within the deadline.
     */
    private class HeaderRead<T>(val value: T)

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

        val duration = currentState.recordingStartedAtMonotonic
            ?.let { monotonicClock() - it }             // live session: immune to wall-clock adjustments
            ?: (wallClock() - currentState.recordingStartedAt)  // resumed: trigger file persists wall time only
        // Negative = wall clock moved backward across a resume; fail open (no warning) rather than
        // trap the user in TooShort.
        if (duration in 0 until MIN_RECORDING_MS) return StopResult.TooShort

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
        // Monotonic base for the duration heuristic, null when there is none: a resumed session's
        // only start time is the persisted wall clock, and a monotonic value from a previous process
        // or boot is meaningless. Nullable rather than 0L — 0 is a legal elapsedRealtime near boot.
        val recordingStartedAtMonotonic: Long? = null,
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
            parseTriggerContent(content, wallClock())
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

        /**
         * Duration heuristic for "did you forget to reproduce the issue?". A recording stopped
         * this quickly usually contains nothing but the recorder starting and stopping, which
         * costs a support round-trip to re-request.
         *
         * It stays a prompt because short recordings can be perfectly valid: a crash is logged
         * and flushed immediately, so the reproduction is already on disk. "Stop anyway" works —
         * the [StopResult.TooShort] consumers (the Support and ContactForm screens) stop via
         * their direct [stopRecorder] path, which has no duration check.
         */
        internal const val MIN_RECORDING_MS = 10_000L

        // Budget for the header's diagnostics read.
        private const val HEADER_READ_TIMEOUT_MS = 5_000L

        // The trigger file stores wall-clock timestamps: it has to survive reboots, which monotonic
        // time does not. [now] is the module's wall clock, defaulted for direct test calls.
        @VisibleForTesting
        internal fun parseTriggerContent(content: String, now: Long = System.currentTimeMillis()): TriggerInfo? {
            if (content.isBlank()) return null
            val lines = content.trim().lines()
            if (lines.size != 2) return null

            val dir = File(lines[0])
            if (!dir.exists() || !dir.isDirectory) return null

            val timestamp = lines[1].toLongOrNull() ?: return null
            if (timestamp < 1 || timestamp > now + 60_000L) return null

            return TriggerInfo(logDir = dir, startedAt = timestamp)
        }
    }
}
