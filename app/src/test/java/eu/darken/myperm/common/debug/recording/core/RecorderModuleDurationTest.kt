package eu.darken.myperm.common.debug.recording.core

import android.content.Context
import eu.darken.myperm.common.InstallId
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.FileLogger
import eu.darken.myperm.common.debug.logging.Logging
import eu.darken.myperm.common.upgrade.UpgradeDiagnostics
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.coroutine.TestDispatcherProvider
import java.io.File

/**
 * The "that recording looks too short" prompt is a duration heuristic, and duration was measured
 * against the wall clock. A clock adjustment mid-recording (NTP sync, the user changing the time)
 * therefore either invented a long recording out of a short one or trapped a long recording in the
 * warning. A live session now measures monotonically; only a session resumed from the trigger file
 * has to fall back to the persisted wall time, because monotonic time does not survive a reboot.
 */
class RecorderModuleDurationTest {

    @TempDir
    lateinit var externalDir: File

    @TempDir
    lateinit var cacheDir: File

    private lateinit var context: Context
    private lateinit var installId: InstallId
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var appScope: CoroutineScope
    private lateinit var upgradeDiagnostics: UpgradeDiagnostics

    @BeforeEach
    fun setup() {
        File(externalDir, "debug/logs").mkdirs()
        File(cacheDir, "debug/logs").mkdirs()

        context = mockk(relaxed = true)
        every { context.getExternalFilesDir(null) } returns externalDir
        every { context.cacheDir } returns cacheDir

        installId = mockk()
        every { installId.id } returns "abcdef12-0000-0000-0000-000000000000"

        dispatcherProvider = TestDispatcherProvider()
        appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

        // Inert diagnostics: the header reads are covered by RecorderModuleDiagnosticsTest, these
        // fixtures only need them to not touch storage.
        upgradeDiagnostics = mockk(relaxed = true)
    }

    @AfterEach
    fun teardown() {
        appScope.cancel()
    }

    // Test-controlled clocks, handed to the module's two seams. The durations under test are
    // wall-clock/monotonic, so virtual time cannot drive them.
    private class TestClocks(var wall: Long, var monotonic: Long)

    /**
     * The recorder is stopped in a nested finally, before the scope goes: cancelling the scope alone
     * does NOT uninstall a running recorder's globally installed [FileLogger], and the forward-jump
     * case deliberately ends still recording. A leaked logger must fail THIS test rather than write
     * into every later one.
     *
     * [seededTrigger] is a persisted wall-clock start time for a resumable session. It is written
     * BEFORE the module is constructed — seeding it afterwards races the init collector, which on
     * the unconfined dispatcher resumes the session during construction. That also means a resumed
     * session's start runs against the default clocks; only the seams the assertions depend on
     * (the stop-time reads) are under test control there.
     */
    private fun withModule(
        clocks: TestClocks,
        seededTrigger: Long? = null,
        block: suspend (RecorderModule) -> Unit,
    ) {
        val fileLoggersBefore = Logging.loggers.filterIsInstance<FileLogger>()
        val triggerFile = File(externalDir, "myperm_force_debug_run")
        if (seededTrigger != null) {
            // PP's real two-line trigger format: session dir + wall-clock start time.
            val sessionDir = File(externalDir, "debug/logs/myperm_resumed_session").also { it.mkdirs() }
            triggerFile.writeText("${sessionDir.absolutePath}\n$seededTrigger")
        } else if (triggerFile.exists()) {
            triggerFile.delete()
        }

        var module: RecorderModule? = null
        try {
            try {
                val created = RecorderModule(
                    context = context,
                    appScope = appScope,
                    dispatcherProvider = dispatcherProvider,
                    installId = installId,
                    upgradeDiagnostics = upgradeDiagnostics,
                ).apply {
                    wallClock = { clocks.wall }
                    monotonicClock = { clocks.monotonic }
                }
                module = created
                // Envelope: a wedged start or stop must fail in seconds, not hold the gradle worker.
                runBlocking {
                    withTimeout(TEST_ENVELOPE_MS) {
                        if (seededTrigger != null) created.state.first { it.isRecording }
                        block(created)
                    }
                }
            } finally {
                module?.let { runBlocking { withTimeoutOrNull(TEST_ENVELOPE_MS) { it.stopRecorder() } } }
            }
        } finally {
            appScope.cancel()
            if (triggerFile.exists()) triggerFile.delete()
            // Remove stragglers after asserting so one failure can't cascade into later tests.
            val leaked = Logging.loggers.filterIsInstance<FileLogger>() - fileLoggersBefore.toSet()
            leaked.forEach { Logging.remove(it) }
            leaked shouldBe emptyList<FileLogger>()
        }
    }

    @Test
    fun `an eight second recording warns`() {
        val clocks = TestClocks(wall = WALL_BASE, monotonic = 100_000L)
        withModule(clocks) { module ->
            module.startRecorder()

            clocks.monotonic += 8_000L
            module.requestStopRecorder() shouldBe RecorderModule.StopResult.TooShort
            module.state.first().isRecording shouldBe true

            // "Stop anyway" is the user's own next step, and past the threshold it stops cleanly.
            clocks.monotonic += 3_000L
            module.requestStopRecorder().shouldBeInstanceOf<RecorderModule.StopResult.Stopped>()
            module.state.first().isRecording shouldBe false
        }
    }

    @Test
    fun `a ten second recording stops`() {
        val clocks = TestClocks(wall = WALL_BASE, monotonic = 100_000L)
        withModule(clocks) { module ->
            module.startRecorder()

            clocks.monotonic += 10_000L

            val result = module.requestStopRecorder()
            result.shouldBeInstanceOf<RecorderModule.StopResult.Stopped>()
            result.logDir.exists() shouldBe true
            result.sessionId.isNotEmpty() shouldBe true
            module.state.first().isRecording shouldBe false
        }
    }

    @Test
    fun `a backward wall-clock jump does not warn on a long recording`() {
        val clocks = TestClocks(wall = WALL_BASE, monotonic = 100_000L)
        withModule(clocks) { module ->
            module.startRecorder()

            // Twelve real seconds of recording, and an NTP sync that moves the wall clock an hour
            // back. Wall-clock measurement would report a negative duration here.
            clocks.monotonic += 12_000L
            clocks.wall -= 3_600_000L

            module.requestStopRecorder().shouldBeInstanceOf<RecorderModule.StopResult.Stopped>()
        }
    }

    @Test
    fun `a forward wall-clock jump does not skip the warning`() {
        val clocks = TestClocks(wall = WALL_BASE, monotonic = 100_000L)
        withModule(clocks) { module ->
            module.startRecorder()

            // Three real seconds of recording, and a clock correction an hour forward. Wall-clock
            // measurement would call this a one-hour recording and skip the prompt.
            clocks.monotonic += 3_000L
            clocks.wall += 3_600_000L

            module.requestStopRecorder() shouldBe RecorderModule.StopResult.TooShort
            module.state.first().isRecording shouldBe true
        }
    }

    @Test
    fun `a resumed session measures from the persisted start time`() {
        // Resumed after a process death: there is no monotonic base to measure against, so the
        // persisted wall-clock start is all the module has.
        val base = System.currentTimeMillis()
        val startedAt = base - 8_000L
        val clocks = TestClocks(wall = base, monotonic = 100_000L)

        withModule(clocks, seededTrigger = startedAt) { module ->
            module.requestStopRecorder() shouldBe RecorderModule.StopResult.TooShort

            clocks.wall = startedAt + 10_000L
            module.requestStopRecorder().shouldBeInstanceOf<RecorderModule.StopResult.Stopped>()
        }
    }

    @Test
    fun `a resumed session with a future start time fails open`() {
        // The persisted start lies in the future (the wall clock moved backward across the resume).
        // A negative duration must not trap the user in the warning.
        val base = System.currentTimeMillis()
        val clocks = TestClocks(wall = base, monotonic = 100_000L)

        withModule(clocks, seededTrigger = base + 60_000L) { module ->
            module.requestStopRecorder().shouldBeInstanceOf<RecorderModule.StopResult.Stopped>()
        }
    }

    companion object {
        // Independent of any production bound: a wedged wait has to fail the test, not hang the
        // gradle worker.
        private const val TEST_ENVELOPE_MS = 10_000L
        private const val WALL_BASE = 1_800_000_000_000L
    }
}
