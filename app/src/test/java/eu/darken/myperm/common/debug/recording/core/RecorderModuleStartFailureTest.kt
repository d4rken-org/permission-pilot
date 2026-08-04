package eu.darken.myperm.common.debug.recording.core

import android.content.Context
import eu.darken.myperm.common.InstallId
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.FileLogger
import eu.darken.myperm.common.debug.logging.Logging
import eu.darken.myperm.common.upgrade.UpgradeDiagnostics
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
import java.io.IOException

/**
 * The storage legs of starting a recording: the session directory that cannot be created, and the
 * log file that cannot be opened. Both used to abandon the attempt mid-way — the module kept
 * "should record" set while nothing was recording, and the caller waited for a recording that was
 * never going to arrive.
 */
class RecorderModuleStartFailureTest {

    @TempDir
    lateinit var externalDir: File

    @TempDir
    lateinit var cacheDir: File

    private lateinit var context: Context
    private lateinit var installId: InstallId
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var appScope: CoroutineScope
    private lateinit var upgradeDiagnostics: UpgradeDiagnostics

    private val triggerFile: File
        get() = File(externalDir, "myperm_force_debug_run")

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

    /**
     * Same shape as RecorderModuleDurationTest.withModule: the recorder is stopped in a nested
     * finally before the scope goes, because cancelling the scope alone does not uninstall a
     * running recorder's globally installed [FileLogger]. A leaked logger must fail THIS test
     * rather than write into every later one.
     *
     * The module is constructed inside the harness: a seeded trigger file has to be written before
     * construction, since the init collector resumes the session on the unconfined dispatcher while
     * the constructor is still running.
     */
    private fun withModule(block: suspend (RecorderModule) -> Unit) {
        val fileLoggersBefore = Logging.loggers.filterIsInstance<FileLogger>()
        var module: RecorderModule? = null
        try {
            try {
                val created = RecorderModule(
                    context = context,
                    appScope = appScope,
                    dispatcherProvider = dispatcherProvider,
                    installId = installId,
                    upgradeDiagnostics = upgradeDiagnostics,
                )
                module = created
                // Envelope: a wedged start or stop must fail in seconds, not hold the gradle worker.
                runBlocking { withTimeout(TEST_ENVELOPE_MS) { block(created) } }
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
    fun `a session directory that cannot be created surfaces as a start failure`() {
        // The logs parent is a FILE, so no session directory can be made below it. This is
        // createSessionDir's explicit IOException, the earliest failure a start can hit.
        File(externalDir, "debug/logs").deleteRecursively()
        File(externalDir, "debug/logs").createNewFile()

        withModule { module ->
            val error = shouldThrow<IOException> { module.startRecorder() }
            error.message!! shouldContain "Failed to create session directory"

            val settled = module.state.first()
            settled.startFailure shouldBe error
            settled.isRecording shouldBe false
            settled.shouldRecord shouldBe false
            settled.logDir.shouldBeNull()
            module.currentLogDir.shouldBeNull()
            // Nothing got as far as arming a trigger, and nothing may leave one behind either.
            triggerFile.exists() shouldBe false
        }
    }

    @Test
    fun `a recorder that cannot open its log file surfaces as a start failure`() {
        // A session resumed from a previous process, with core.log occupied by a DIRECTORY: the
        // file logger cannot open its writer. That used to be swallowed, which installed a logger
        // that wrote nowhere and reported a running recording.
        val resumedDir = File(externalDir, "debug/logs/myperm_resumed_session").also { it.mkdirs() }
        File(resumedDir, "core.log").mkdirs()
        triggerFile.writeText("${resumedDir.absolutePath}\n${System.currentTimeMillis() - 5_000L}")

        withModule { module ->
            val settled = module.state.first { it.startFailure != null }
            settled.startFailure.shouldBeInstanceOf<IOException>()
            settled.isRecording shouldBe false
            settled.shouldRecord shouldBe false
            module.currentLogDir.shouldBeNull()
            // The trigger is gone, so the failing start is not repeated on every process start.
            triggerFile.exists() shouldBe false
            // The resumed directory predates this attempt and holds an earlier recording: only a
            // directory the attempt created itself is cleaned up.
            resumedDir.exists() shouldBe true
        }
    }

    companion object {
        // Independent of any production bound: a wedged wait has to fail the test, not hang the
        // gradle worker.
        private const val TEST_ENVELOPE_MS = 10_000L
    }
}
