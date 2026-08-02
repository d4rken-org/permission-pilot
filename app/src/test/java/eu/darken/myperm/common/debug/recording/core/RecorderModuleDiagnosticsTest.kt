package eu.darken.myperm.common.debug.recording.core

import androidx.test.core.app.ApplicationProvider
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.InstallId
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.FileLogger
import eu.darken.myperm.common.debug.logging.Logging
import eu.darken.myperm.common.upgrade.UpgradeDiagnostics
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.system.measureTimeMillis

/**
 * The recording header reads diagnostics that live outside the recorder. Those reads happen AFTER
 * the recorder is already writing, so a guarded failure must never abort the state update — that
 * would leave a running recorder the module no longer knows about, i.e. a debug recording that
 * can't be stopped or collected. Failures that DO escape the header have to take the uncommitted
 * recorder down with them.
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
     * Cancellation is one of the failures that escape the header, so it is one of the failures that
     * can abort the state update. The recorder is already live at that point: it has to be stopped
     * on the way out, or it keeps writing into a session the module no longer tracks.
     *
     * The start is launched, not awaited: an aborted update never flips isRecording, so
     * startRecorder() stays suspended. The virtual-time delay is what lets the module's own
     * background collectors run to completion.
     */
    @Test
    fun `a cancelled upgrade-diagnostics read stops the recorder instead of leaking it`() = runTest {
        val fileLoggersBefore = Logging.loggers.filterIsInstance<FileLogger>()
        val diagnostics = mockk<UpgradeDiagnostics>()
        coEvery { diagnostics.debugInfo() } throws CancellationException("scope died mid-read")

        val module = buildModule(backgroundScope, diagnostics)

        backgroundScope.launch { module.startRecorder() }
        delay(1_000)

        coVerify { diagnostics.debugInfo() }
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
     */
    @Test
    fun `a failing header read stops the uncommitted recorder`() = runTest {
        val fileLoggersBefore = Logging.loggers.filterIsInstance<FileLogger>()
        val diagnostics = mockk<UpgradeDiagnostics>()
        coEvery { diagnostics.debugInfo() } returns "BillingCache(...)"

        mockkObject(BuildConfigWrap)
        every { BuildConfigWrap.VERSION_DESCRIPTION } throws IllegalStateException("build info unreadable")

        // Own scope: the escaping exception fails the collector, which must not fail the test's own
        // scope. SupervisorJob keeps the module's state flow alive so it can be inspected after.
        val moduleScope = CoroutineScope(coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, _ -> })
        try {
            val module = buildModule(moduleScope, diagnostics)

            moduleScope.launch { module.startRecorder() }
            delay(1_000)

            module.state.first().isRecording shouldBe false
            module.currentLogDir.shouldBeNull()
            // Non-vacuity: without the guard's cleanup the started recorder's file logger would
            // still be installed here.
            Logging.loggers.filterIsInstance<FileLogger>() shouldBe fileLoggersBefore
        } finally {
            moduleScope.cancel()
            unmockkObject(BuildConfigWrap)
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
