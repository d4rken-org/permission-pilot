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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    // Serializes the public start/stop requests: each one publishes its request and then waits for a
    // terminal state, and two overlapping requests would observe each other's outcome.
    private val requestMutex = Mutex()

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
                        // The whole start is one attempt: anything between creating the session and
                        // committing it can fail, and every one of those failures used to abandon a
                        // running recorder, kill this collector and take the process with it.
                        var newRecorder: Recorder? = null
                        var createdSessionDir: File? = null
                        try {
                            val resumed = persistedLogDir?.takeIf { it.exists() && it.isDirectory }
                            val sessionDir = resumed ?: createSessionDir().also { createdSessionDir = it }
                            val startTime = if (resumed != null) {
                                log(TAG, INFO) { "Resuming recording in existing session: ${resumed.name}" }
                                recordingStartedAt
                            } else {
                                wallClock()
                            }

                            val logFile = File(sessionDir, "core.log")
                            val recorder = Recorder()
                            newRecorder = recorder
                            recorder.start(logFile)
                            writeTriggerFile(sessionDir, startTime)
                            logRecordingHeader()

                            currentLogDir = sessionDir

                            copy(
                                recorder = recorder,
                                startFailure = null,
                                persistedLogDir = null,
                                recordingStartedAt = startTime,
                                recordingStartedAtMonotonic = if (resumed != null) null else monotonicClock(),
                                logDir = sessionDir,
                            )
                        } catch (e: Exception) {
                            rollbackFailedStart(e, newRecorder, createdSessionDir)
                            // A cancelled scope still cancels; a CancellationException that merely
                            // escaped a dependency is committed as a failure instead, because
                            // rethrowing it here is what leaves the module wedged.
                            currentCoroutineContext().ensureActive()

                            // Final, non-throwing step: the request is answered with a failure the
                            // caller can surface, and the next start attempt re-arms cleanly.
                            copy(
                                shouldRecord = false,
                                startFailure = asStartFailure(e),
                                recorder = null,
                                persistedLogDir = null,
                                recordingStartedAt = 0L,
                                recordingStartedAtMonotonic = null,
                                logDir = null,
                            )
                        }
                    } else if (!shouldRecord && isRecording) {
                        recorder?.stop()

                        deleteTriggerFile()

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

    /**
     * Undoes a half-finished start. Every step runs even if an earlier one failed, and none of them
     * may replace [cause]: the failure being committed is what the caller gets to see.
     *
     * [createdSessionDir] is only set when THIS attempt created the directory. A resumed session's
     * directory holds an earlier recording and is never deleted; a directory this attempt created is
     * a dead session that the session manager would otherwise list and auto-zip, and that a retry
     * within the same second would collide with.
     */
    private suspend fun rollbackFailedStart(cause: Exception, recorder: Recorder?, createdSessionDir: File?) {
        withContext(NonCancellable) {
            try {
                recorder?.stop()
            } catch (e: Exception) {
                cause.recordSuppressed(e)
            }

            currentLogDir = null

            try {
                // A trigger that survives the failure crash-loops every process start.
                deleteTriggerFile()
            } catch (e: Exception) {
                cause.recordSuppressed(e)
            }

            try {
                if (createdSessionDir != null && !createdSessionDir.deleteRecursively()) {
                    log(TAG, WARN) { "Failed to delete the session dir of a failed start: $createdSessionDir" }
                }
            } catch (e: Exception) {
                cause.recordSuppressed(e)
            }

            try {
                log(TAG, ERROR) { "Failed to start recording: ${cause.asLog()}" }
            } catch (_: Exception) {
                // A logger that throws while we report the failure must not become the failure.
            }
        }
    }

    /**
     * Attaching a rollback failure to the failure being reported. The very same throwable can come
     * back out of the rollback — a logger that fails the start rethrows on the teardown line too —
     * and [Throwable.addSuppressed] rejects self-suppression with an [IllegalArgumentException],
     * which would abort the rollback before the failure state is ever committed.
     */
    private fun Throwable.recordSuppressed(other: Throwable) {
        if (other === this) return
        try {
            addSuppressed(other)
        } catch (_: Throwable) {
            // Bookkeeping only: nothing about the report may replace the failure being reported.
        }
    }

    /**
     * A start failure that arrived as a [CancellationException] while this module's own scope was
     * still alive — a bounded read inside the start work timing out, for example. Storing and
     * rethrowing that unchanged makes every caller treat it as their OWN cancellation: the launch
     * that requested the start ends "normally", and the error handler that would have surfaced the
     * failure never runs.
     */
    class RecordingStartFailedException(cause: Throwable) : IllegalStateException("Failed to start recording", cause)

    private fun asStartFailure(error: Exception): Throwable = when (error) {
        is CancellationException -> RecordingStartFailedException(error)
        else -> error
    }

    private fun deleteTriggerFile() {
        if (triggerFile.exists() && !triggerFile.delete()) {
            log(TAG, ERROR) { "Failed to delete trigger file" }
        }
    }

    // Header lines written into a freshly started recording. Runs AFTER the recorder is live, and
    // every read here is diagnostics-only: an injected source that fails must not deny the user the
    // recording. A failure that DOES escape (the unguarded reads above) aborts the start, which the
    // start branch rolls back and reports.
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
        val timestamp = sdf.format(Date(wallClock()))
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

        // Only a directory this call actually created is returned: mkdirs() reports true exactly
        // when the creation happened, so a name that is already taken (a second recording within
        // the same second) moves on to the next suffix instead of adopting the earlier session's
        // directory — which the rollback of a failed start would then delete.
        var attempt = 1
        while (true) {
            val candidate = File(parent, if (attempt == 1) dirName else "$dirName-$attempt")
            if (candidate.mkdirs()) {
                log(TAG) { "Created session dir: $candidate" }
                return candidate
            }
            if (!candidate.exists()) {
                throw java.io.IOException("Failed to create session directory: $candidate")
            }
            attempt++
            if (attempt > MAX_SESSION_DIR_ATTEMPTS) {
                throw java.io.IOException("Failed to create a unique session directory in: $parent")
            }
        }
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

    /**
     * Publishes the request and waits for a terminal answer: either the recording is live, or the
     * attempt failed and the failure is thrown here. Waiting for [State.isRecording] alone leaves
     * every caller of a failed start suspended forever.
     */
    suspend fun startRecorder(): File = requestMutex.withLock {
        internalState.updateBlocking {
            // Clearing the previous failure is what re-arms the wait: a stale one would answer this
            // request with the error of an attempt that is already over.
            copy(shouldRecord = true, startFailure = null)
        }
        val settled = internalState.flow.first { it.isRecording || it.startFailure != null }
        settled.startFailure?.let { throw it }
        settled.logDir!!
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

    suspend fun stopRecorder(): File? = requestMutex.withLock {
        val dir = currentLogDir ?: return@withLock null
        internalState.updateBlocking {
            copy(shouldRecord = false)
        }
        internalState.flow.first { !it.isRecording }
        dir
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
        // The failure of the LAST start attempt, cleared by the next one. A Throwable reference is
        // deliberate: two identical failures are still two distinct values, so a repeated failure
        // is not swallowed by the state flow's distinctUntilChanged.
        val startFailure: Throwable? = null,
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

        // Suffix attempts for a session directory name that is already taken. The name carries a
        // second-resolution timestamp, so collisions only come from repeated starts within one
        // second; a bound keeps a permanently unwritable parent from spinning here.
        private const val MAX_SESSION_DIR_ATTEMPTS = 10

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
