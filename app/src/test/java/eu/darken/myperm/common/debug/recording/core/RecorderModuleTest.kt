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
}
