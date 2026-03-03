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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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

    private lateinit var logDirExternal: File
    private lateinit var logDirCache: File

    @BeforeEach
    fun setup() {
        logDirExternal = File(externalDir, "debug/logs").also { it.mkdirs() }
        logDirCache = File(cacheDir, "debug/logs").also { it.mkdirs() }

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
    fun `getLogSessionCount counts directories in log dirs`() {
        File(logDirExternal, "session1").mkdirs()
        File(logDirExternal, "session2").mkdirs()
        File(logDirCache, "session3").mkdirs()

        val module = createModule()
        module.getLogSessionCount() shouldBe 3
    }

    @Test
    fun `getLogSessionCount counts standalone zip files`() {
        File(logDirExternal, "recording.zip").createNewFile()
        File(logDirCache, "recording2.zip").createNewFile()

        val module = createModule()
        module.getLogSessionCount() shouldBe 2
    }

    @Test
    fun `getLogSessionCount counts legacy flat log files`() {
        File(logDirExternal, "core.log").createNewFile()
        File(logDirExternal, "other.log").createNewFile()

        val module = createModule()
        module.getLogSessionCount() shouldBe 2
    }

    @Test
    fun `getLogSessionCount counts mixed entries`() {
        File(logDirExternal, "session1").mkdirs()
        File(logDirExternal, "recording.zip").createNewFile()
        File(logDirCache, "legacy.log").createNewFile()
        // A random .txt file should NOT be counted
        File(logDirCache, "notes.txt").createNewFile()

        val module = createModule()
        module.getLogSessionCount() shouldBe 3
    }

    @Test
    fun `getLogSessionCount returns 0 for empty directories`() {
        val module = createModule()
        module.getLogSessionCount() shouldBe 0
    }

    @Test
    fun `getLogFolderSize recursively sums file sizes in session dirs`() {
        val sessionDir = File(logDirExternal, "session1").also { it.mkdirs() }
        File(sessionDir, "core.log").writeText("A".repeat(100))
        val subDir = File(sessionDir, "sub").also { it.mkdirs() }
        File(subDir, "extra.log").writeText("B".repeat(50))

        // Standalone file
        File(logDirCache, "flat.log").writeText("C".repeat(30))

        val module = createModule()
        module.getLogFolderSize() shouldBe 180L
    }

    @Test
    fun `getLogFolderSize returns 0 for empty directories`() {
        val module = createModule()
        module.getLogFolderSize() shouldBe 0L
    }

    @Test
    fun `deleteAllLogs deletes all entries when not recording`() = runTest {
        val sessionDir = File(logDirExternal, "session1").also { it.mkdirs() }
        File(sessionDir, "core.log").writeText("log content")
        File(logDirExternal, "recording.zip").createNewFile()
        File(logDirCache, "legacy.log").createNewFile()

        val module = createModule()

        module.getLogSessionCount() shouldBe 3

        module.deleteAllLogs()

        module.getLogSessionCount() shouldBe 0
    }

    @Test
    fun `deleteAllLogs deletes legacy flat files`() = runTest {
        File(logDirExternal, "core.log").writeText("log data")
        File(logDirExternal, "debug.log").writeText("more log data")
        File(logDirCache, "old.log").writeText("old log")

        val module = createModule()
        module.getLogSessionCount() shouldBe 3

        module.deleteAllLogs()

        module.getLogSessionCount() shouldBe 0
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
        dirs[0].absolutePath shouldBe logDirExternal.absolutePath
        dirs[1].absolutePath shouldBe logDirCache.absolutePath
    }
}
