package eu.darken.myperm.common.debug.recording.core

import android.content.Context
import eu.darken.myperm.common.InstallId
import eu.darken.myperm.common.coroutine.DispatcherProvider
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.coroutine.TestDispatcherProvider
import java.io.File

class RecorderModuleTest {

    @TempDir
    lateinit var externalDir: File

    @TempDir
    lateinit var cacheDir: File

    private lateinit var context: Context
    private lateinit var installId: InstallId
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var appScope: CoroutineScope

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
    }

    @AfterEach
    fun teardown() {
        appScope.cancel()
    }

    private fun createModule(): RecorderModule {
        // Ensure the trigger file does NOT exist so the init block won't start recording
        val triggerFile = File(externalDir, "myperm_force_debug_run")
        if (triggerFile.exists()) triggerFile.delete()

        return RecorderModule(
            context = context,
            appScope = appScope,
            dispatcherProvider = dispatcherProvider,
            installId = installId,
        )
    }

    @Test
    fun `createSessionDir falls back to cacheDir when external storage returns null`() {
        every { context.getExternalFilesDir(null) } returns null

        val module = createModule()

        val logDirs = module.getLogDirectories()
        logDirs.size shouldBe 1
        logDirs[0].absolutePath shouldBe File(cacheDir, "debug/logs").absolutePath
    }

    @Test
    fun `getLogDirectories includes both external and cache dirs`() {
        val module = createModule()
        val dirs = module.getLogDirectories()

        dirs.size shouldBe 2
        dirs[0].absolutePath shouldBe File(externalDir, "debug/logs").absolutePath
        dirs[1].absolutePath shouldBe File(cacheDir, "debug/logs").absolutePath
    }

    @Test
    fun `initial state is not recording`() {
        val module = createModule()
        module.currentLogDir shouldBe null
    }

    @Nested
    inner class FindExistingSessionDir {
        @Test
        fun `returns null when no directories exist`(@TempDir tempDir: File) {
            val logDir = File(tempDir, "debug/logs")
            RecorderModule.findExistingSessionDir(listOf(logDir)) shouldBe null
        }

        @Test
        fun `returns null when log directory is empty`(@TempDir tempDir: File) {
            val logDir = File(tempDir, "debug/logs").also { it.mkdirs() }
            RecorderModule.findExistingSessionDir(listOf(logDir)) shouldBe null
        }

        @Test
        fun `returns null when session dir has no core log`(@TempDir tempDir: File) {
            val logDir = File(tempDir, "debug/logs").also { it.mkdirs() }
            File(logDir, "myperm_1.0_20260309T120000Z_abc12345").mkdirs()
            RecorderModule.findExistingSessionDir(listOf(logDir)) shouldBe null
        }

        @Test
        fun `returns null for non-myperm directories`(@TempDir tempDir: File) {
            val logDir = File(tempDir, "debug/logs").also { it.mkdirs() }
            val dir = File(logDir, "some_other_dir").also { it.mkdirs() }
            File(dir, "core.log").createNewFile()
            RecorderModule.findExistingSessionDir(listOf(logDir)) shouldBe null
        }

        @Test
        fun `finds existing session with core log`(@TempDir tempDir: File) {
            val logDir = File(tempDir, "debug/logs").also { it.mkdirs() }
            val sessionDir = File(logDir, "myperm_1.0_20260309T120000Z_abc12345").also { it.mkdirs() }
            File(sessionDir, "core.log").createNewFile()

            RecorderModule.findExistingSessionDir(listOf(logDir)) shouldBe sessionDir
        }

        @Test
        fun `returns most recent session when multiple exist`(@TempDir tempDir: File) {
            val logDir = File(tempDir, "debug/logs").also { it.mkdirs() }

            val older = File(logDir, "myperm_1.0_20260308T100000Z_abc12345").also { it.mkdirs() }
            File(older, "core.log").createNewFile()
            older.setLastModified(1000L)

            val newer = File(logDir, "myperm_1.0_20260309T120000Z_abc12345").also { it.mkdirs() }
            File(newer, "core.log").createNewFile()
            newer.setLastModified(2000L)

            RecorderModule.findExistingSessionDir(listOf(logDir)) shouldBe newer
        }

        @Test
        fun `returns null when most recent session has no core log`(@TempDir tempDir: File) {
            val logDir = File(tempDir, "debug/logs").also { it.mkdirs() }

            val withLog = File(logDir, "myperm_1.0_20260308T100000Z_abc12345").also { it.mkdirs() }
            File(withLog, "core.log").createNewFile()
            withLog.setLastModified(1000L)

            val withoutLog = File(logDir, "myperm_1.0_20260309T120000Z_abc12345").also { it.mkdirs() }
            withoutLog.setLastModified(2000L)

            // Only checks the most recent dir - if it has no core.log, returns null
            RecorderModule.findExistingSessionDir(listOf(logDir)) shouldBe null
        }

        @Test
        fun `prefers first directory with a match`(@TempDir tempDir: File) {
            val extDir = File(tempDir, "ext/debug/logs").also { it.mkdirs() }
            val cacheDir = File(tempDir, "cache/debug/logs").also { it.mkdirs() }

            val extSession = File(extDir, "myperm_1.0_20260309T120000Z_abc12345").also { it.mkdirs() }
            File(extSession, "core.log").createNewFile()
            extSession.setLastModified(1000L)

            val cacheSession = File(cacheDir, "myperm_1.0_20260309T130000Z_abc12345").also { it.mkdirs() }
            File(cacheSession, "core.log").createNewFile()
            cacheSession.setLastModified(2000L)

            // Returns from first directory that has a match (ext), not the globally most recent
            RecorderModule.findExistingSessionDir(listOf(extDir, cacheDir)) shouldBe extSession
        }
    }
}
