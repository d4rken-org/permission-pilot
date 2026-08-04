package eu.darken.myperm.common.debug.recording.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.InstallId
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.FileLogger
import eu.darken.myperm.common.debug.logging.Logging
import eu.darken.myperm.common.upgrade.UpgradeDiagnostics
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider
import testhelpers.TestApplication
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.system.measureTimeMillis

/**
 * The recording header reads diagnostics that live outside the recorder. Those reads happen AFTER
 * the recorder is already writing, so a guarded failure must never abort the state update — that
 * would leave a running recorder the module no longer knows about, i.e. a debug recording that
 * can't be stopped or collected.
 *
 * Failures that DO escape the header take the uncommitted recorder down with them and are then
 * reported: the attempt is rolled back, the failure is committed to the state, and the caller gets
 * it thrown. What must NOT happen is the old behaviour — the exception escaping the shared
 * collector, which killed it, wedged every later start and crashed the process.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class RecorderModuleDiagnosticsTest : BaseTest() {

    private val logLines = CopyOnWriteArrayList<String>()
    private val logCapture = object : Logging.Logger {
        override fun log(priority: Logging.Priority, tag: String, message: String, metaData: Map<String, Any>?) {
            logLines.add(message)
        }
    }

    @Before
    fun installLogCapture() {
        Logging.install(logCapture)
    }

    @After
    fun removeLogCapture() {
        Logging.remove(logCapture)
    }

    // The trigger file lives next to the log dirs in the (shared) external files dir: a stale one
    // would make the next module in this process resume-start during construction.
    private val triggerFile: File
        get() = File(
            ApplicationProvider.getApplicationContext<Context>().getExternalFilesDir(null),
            "myperm_force_debug_run",
        )

    @Before
    fun clearTriggerFile() {
        triggerFile.delete()
    }

    // Runs after the per-test leak assertions: a test that fails halfway must not hand its recorder's
    // globally installed logger to the next one.
    @After
    fun removeStragglingFileLoggers() {
        Logging.loggers.filterIsInstance<FileLogger>().forEach { Logging.remove(it) }
    }

    private fun buildModule(
        scope: CoroutineScope,
        upgradeDiagnostics: UpgradeDiagnostics,
        dispatcherProvider: DispatcherProvider = TestDispatcherProvider(),
    ) = RecorderModule(
        context = ApplicationProvider.getApplicationContext(),
        appScope = scope,
        dispatcherProvider = dispatcherProvider,
        installId = mockk<InstallId>(relaxed = true).apply {
            every { id } returns "abcdef12-0000-0000-0000-000000000000"
        },
        upgradeDiagnostics = upgradeDiagnostics,
    )

    /**
     * Real dispatchers on purpose: the header's read deadline is wall-clock, so a virtual-time test
     * would skip past it instead of exercising it — an ignored seam has to fail this, not pass after
     * the full production budget. The seam is set before [RecorderModule.startRecorder] so no header
     * read can run against the production bound.
     *
     * The block runs inside its own deadline: a missing or mis-wired production bound must fail this
     * test rather than wedge the gradle worker on a read that never answers.
     */
    private fun withRealtimeModule(
        upgradeDiagnostics: UpgradeDiagnostics,
        headerTimeoutMs: Long = 300L,
        block: suspend (RecorderModule) -> Unit,
    ) {
        val fileLoggersBefore = Logging.loggers.filterIsInstance<FileLogger>()
        val moduleScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val module = buildModule(moduleScope, upgradeDiagnostics, TestDispatcherProvider(Dispatchers.IO))
        module.headerReadTimeoutMs = headerTimeoutMs
        try {
            runBlocking { withTimeout(TEST_ENVELOPE_MS) { block(module) } }
        } finally {
            // Stop the recorder BEFORE the scope goes: cancelling the scope alone leaves the
            // recorder's globally installed FileLogger and its trigger file behind for every later
            // test in this process.
            runBlocking { withTimeoutOrNull(TEST_ENVELOPE_MS) { module.stopRecorder() } }
            moduleScope.cancel()
        }
        Logging.loggers.filterIsInstance<FileLogger>() shouldBe fileLoggersBefore
    }

    @Test
    fun `a failing upgrade-diagnostics read still leaves a tracked recording`() = runTest {
        val diagnostics = mockk<UpgradeDiagnostics>()
        coEvery { diagnostics.debugInfo() } throws IllegalStateException("cache unreadable")

        val module = buildModule(backgroundScope, diagnostics)

        val logDir = module.startRecorder()
        logDir.exists() shouldBe true
        module.state.first { it.isRecording }.logDir shouldBe logDir
        module.currentLogDir shouldBe logDir
        coVerify { diagnostics.debugInfo() }

        module.stopRecorder().shouldNotBeNull()
    }

    /**
     * A cancellation escaping the header is not necessarily OUR cancellation — a dependency can let
     * one out while the module's scope is perfectly alive. The recorder is already live at that
     * point: it has to be stopped on the way out, or it keeps writing into a session the module no
     * longer tracks, and the caller has to be answered instead of left waiting on a start that will
     * never arrive.
     *
     * The start is launched, not awaited inline: the virtual-time delay is what lets the module's
     * own background collectors run to completion before the assertions read the outcome.
     */
    @Test
    fun `a cancelled upgrade-diagnostics read stops the recorder instead of leaking it`() = runTest {
        val fileLoggersBefore = Logging.loggers.filterIsInstance<FileLogger>()
        val diagnostics = mockk<UpgradeDiagnostics>()
        coEvery { diagnostics.debugInfo() } throws CancellationException("scope died mid-read")

        val module = buildModule(backgroundScope, diagnostics)

        val start = backgroundScope.async { runCatching { module.startRecorder() } }
        delay(1_000)

        coVerify { diagnostics.debugInfo() }
        // The foreign cancellation became this attempt's failure instead of a wedged caller, and it
        // arrives as an ordinary exception: handed back as a cancellation, the caller's launch would
        // end "normally" and never surface the failure. The original is kept as the cause, looked up
        // along the chain because stack-trace recovery may hand back a copy that wraps the wrapper.
        val error = start.await().exceptionOrNull()
            .shouldBeInstanceOf<RecorderModule.RecordingStartFailedException>()
        generateSequence(error.cause) { it.cause }
            .filterIsInstance<CancellationException>()
            .map { it.message }
            .toList() shouldContain "scope died mid-read"
        module.state.first().isRecording shouldBe false
        module.currentLogDir.shouldBeNull()
        // The recorder that was already writing when the header aborted got stopped: its file
        // logger is no longer installed.
        Logging.loggers.filterIsInstance<FileLogger>() shouldBe fileLoggersBefore
    }

    /**
     * Same window as above, but for an ordinary failure instead of a cancellation. The header's
     * injected sources are individually guarded, so the escape path is one of the unguarded first
     * log lines — here the build description read.
     *
     * Awaited, not launched: a start that cannot finish has to come back to its caller as a thrown
     * error. This used to be a caller suspended forever behind a collector the same exception had
     * already killed.
     */
    @Test
    fun `a failed start surfaces the error instead of hanging`() = runTest {
        val fileLoggersBefore = Logging.loggers.filterIsInstance<FileLogger>()
        val diagnostics = mockk<UpgradeDiagnostics>()
        coEvery { diagnostics.debugInfo() } returns "BillingCache(...)"

        mockkObject(BuildConfigWrap)
        every { BuildConfigWrap.VERSION_DESCRIPTION } throws IllegalStateException("build info unreadable")

        // Own scope: nothing may reach it, but a module scope that is not the test's own keeps a
        // regression from being reported as an unrelated test-scope failure.
        val moduleScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val module = buildModule(moduleScope, diagnostics)

            val error = shouldThrow<IllegalStateException> { module.startRecorder() }
            error.message shouldBe "build info unreadable"

            val settled = module.state.first()
            settled.isRecording shouldBe false
            // The request is retracted, so the next start is a fresh edge rather than a no-op.
            settled.shouldRecord shouldBe false
            module.currentLogDir.shouldBeNull()
            // A trigger left behind would re-run this failing start on every process start.
            triggerFile.exists() shouldBe false
            // Non-vacuity: without the rollback the started recorder's file logger would still be
            // installed here.
            Logging.loggers.filterIsInstance<FileLogger>() shouldBe fileLoggersBefore
        } finally {
            moduleScope.cancel()
            unmockkObject(BuildConfigWrap)
        }
    }

    /**
     * The collector that processes a failed start has to survive it: it is the single shared
     * collector behind every later start and stop request.
     */
    @Test
    fun `the recorder recovers after a failed start`() = runTest {
        val fileLoggersBefore = Logging.loggers.filterIsInstance<FileLogger>()
        val diagnostics = mockk<UpgradeDiagnostics>()
        coEvery { diagnostics.debugInfo() } returns null

        mockkObject(BuildConfigWrap)
        every { BuildConfigWrap.VERSION_DESCRIPTION } throws IllegalStateException("build info unreadable")

        val moduleScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val module = buildModule(moduleScope, diagnostics)

            shouldThrow<IllegalStateException> { module.startRecorder() }

            every { BuildConfigWrap.VERSION_DESCRIPTION } returns "v1.2.3 (4) ~ deadbeef/foss/DEV"

            val logDir = module.startRecorder()
            module.state.first { it.isRecording }.logDir shouldBe logDir
            module.currentLogDir shouldBe logDir
            // The previous attempt's failure was cleared, not carried into the live recording.
            module.state.first().startFailure.shouldBeNull()

            module.stopRecorder().shouldNotBeNull()
            module.state.first().isRecording shouldBe false
            Logging.loggers.filterIsInstance<FileLogger>() shouldBe fileLoggersBefore
        } finally {
            moduleScope.cancel()
            unmockkObject(BuildConfigWrap)
        }
    }

    /**
     * A trigger file makes the module start recording during construction, i.e. during App.onCreate.
     * A failure there used to be rethrown onto the app scope — a crash — while the trigger stayed on
     * disk, so the next process start crashed the same way. The module has to settle instead.
     */
    @Test
    fun `a boot trigger that cannot start settles instead of crash-looping`() = runTest {
        val fileLoggersBefore = Logging.loggers.filterIsInstance<FileLogger>()
        val diagnostics = mockk<UpgradeDiagnostics>()
        coEvery { diagnostics.debugInfo() } returns null

        val externalDir = ApplicationProvider.getApplicationContext<Context>().getExternalFilesDir(null)!!
        val resumedDir = File(externalDir, "debug/logs/myperm_resumed_session").also { it.mkdirs() }
        triggerFile.writeText("${resumedDir.absolutePath}\n${System.currentTimeMillis() - 5_000L}")

        mockkObject(BuildConfigWrap)
        every { BuildConfigWrap.VERSION_DESCRIPTION } throws IllegalStateException("build info unreadable")

        // Inverted from the usual pattern: this handler stands in for the process default handler
        // that an escaping exception would reach, and it has to stay empty.
        val escaped = CopyOnWriteArrayList<Throwable>()
        val moduleScope = CoroutineScope(
            coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, e -> escaped.add(e) }
        )
        try {
            val module = buildModule(moduleScope, diagnostics)

            val settled = module.state.first { it.startFailure != null }
            settled.isRecording shouldBe false
            settled.shouldRecord shouldBe false
            module.currentLogDir.shouldBeNull()
            // The trigger is gone: this is what stops the failure from repeating every process start.
            triggerFile.exists() shouldBe false
            // The resumed session predates this attempt, so its directory stays.
            resumedDir.exists() shouldBe true
            escaped.shouldBeEmpty()
            Logging.loggers.filterIsInstance<FileLogger>() shouldBe fileLoggersBefore
        } finally {
            moduleScope.cancel()
            unmockkObject(BuildConfigWrap)
            resumedDir.deleteRecursively()
        }
    }

    /**
     * Debug recording is what a user reaches for when the app is ALREADY misbehaving, so a
     * diagnostics source that never answers must not be the thing that denies them the log.
     */
    @Test
    fun `a wedged upgrade diagnostics read does not hold up the recording`() {
        val diagnostics = mockk<UpgradeDiagnostics>()
        coEvery { diagnostics.debugInfo() } coAnswers { awaitCancellation() }

        withRealtimeModule(diagnostics, headerTimeoutMs = 300L) { module ->
            val elapsed = measureTimeMillis { module.startRecorder() }

            module.state.first().isRecording shouldBe true
            logLines.any { it.startsWith("Upgrade diagnostics unavailable") } shouldBe true
            // Non-vacuity: without the bound this would sit on the wedged read forever.
            elapsed shouldBeLessThan 1_500L
        }
    }

    @Test
    fun `a flavor without diagnostics is not reported as unavailable`() {
        // FOSS has nothing to report and returns null: no diagnostics line at all, and above all
        // not one claiming the read failed or timed out.
        val diagnostics = mockk<UpgradeDiagnostics>()
        coEvery { diagnostics.debugInfo() } returns null

        withRealtimeModule(diagnostics) { module ->
            module.startRecorder()

            module.state.first().isRecording shouldBe true
            logLines.any { it.startsWith("Upgrade diagnostics") } shouldBe false
        }
    }

    companion object {
        // Independent of the production bound: a missing or mis-wired one has to fail the test, not
        // hang the gradle worker.
        private const val TEST_ENVELOPE_MS = 10_000L
    }
}
