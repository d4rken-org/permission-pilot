package eu.darken.myperm.common.debug.recording.core

import android.content.Context
import eu.darken.myperm.common.InstallId
import eu.darken.myperm.common.coroutine.DispatcherProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.coroutine.TestDispatcherProvider
import java.io.File

class DebugSessionManagerTest {

    @TempDir
    lateinit var externalDir: File

    @TempDir
    lateinit var cacheDir: File

    private lateinit var context: Context
    private lateinit var installId: InstallId
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var appScope: CoroutineScope
    private lateinit var recorderModule: RecorderModule

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

        // Ensure the trigger file does NOT exist
        val triggerFile = File(externalDir, "myperm_force_debug_run")
        if (triggerFile.exists()) triggerFile.delete()

        recorderModule = RecorderModule(
            context = context,
            appScope = appScope,
            dispatcherProvider = dispatcherProvider,
            installId = installId,
        )
    }

    @AfterEach
    fun teardown() {
        appScope.cancel()
    }

    private fun createManager(): DebugSessionManager {
        return DebugSessionManager(
            context = context,
            appScope = appScope,
            dispatcherProvider = dispatcherProvider,
            recorderModule = recorderModule,
        )
    }

    @Test
    fun `scanSessions returns dirs, zips, and legacy logs`() = runTest {
        File(logDirExternal, "session1").mkdirs()
        File(logDirExternal, "recording.zip").createNewFile()
        File(logDirCache, "legacy.log").createNewFile()
        // A random .txt file should NOT be included
        File(logDirCache, "notes.txt").createNewFile()

        val manager = createManager()
        manager.refreshSessions()

        val sessions = manager.sessions.first()
        sessions.size shouldBe 3
        // All are empty/zero-length, so all Failed
        sessions.forEach { it.shouldBeInstanceOf<DebugSessionManager.LogSession.Failed>() }
    }

    @Test
    fun `scanSessions returns empty for empty directories`() = runTest {
        val manager = createManager()
        manager.refreshSessions()

        val sessions = manager.sessions.first()
        sessions.size shouldBe 0
    }

    @Test
    fun `scanSessions classifies directory with log content as Ready`() = runTest {
        val sessionDir = File(logDirExternal, "session1").also { it.mkdirs() }
        File(sessionDir, "core.log").writeText("log content")

        val manager = createManager()
        manager.refreshSessions()

        val sessions = manager.sessions.first()
        sessions.size shouldBe 1
        sessions[0].shouldBeInstanceOf<DebugSessionManager.LogSession.Ready>()
    }

    @Test
    fun `scanSessions classifies empty directory as Failed`() = runTest {
        File(logDirExternal, "session1").mkdirs()

        val manager = createManager()
        manager.refreshSessions()

        val sessions = manager.sessions.first()
        sessions.size shouldBe 1
        sessions[0].shouldBeInstanceOf<DebugSessionManager.LogSession.Failed>()
    }

    @Test
    fun `scanSessions classifies zero-length zip as Failed`() = runTest {
        File(logDirExternal, "recording.zip").createNewFile()

        val manager = createManager()
        manager.refreshSessions()

        val sessions = manager.sessions.first()
        sessions.size shouldBe 1
        sessions[0].shouldBeInstanceOf<DebugSessionManager.LogSession.Failed>()
    }

    @Test
    fun `scanSessions classifies non-empty zip as Ready`() = runTest {
        File(logDirExternal, "recording.zip").writeText("fake zip content")

        val manager = createManager()
        manager.refreshSessions()

        val sessions = manager.sessions.first()
        sessions.size shouldBe 1
        sessions[0].shouldBeInstanceOf<DebugSessionManager.LogSession.Ready>()
    }

    @Test
    fun `scanSessions classifies directory with only zero-length logs as Failed`() = runTest {
        val sessionDir = File(logDirExternal, "session1").also { it.mkdirs() }
        File(sessionDir, "core.log").createNewFile() // 0 bytes

        val manager = createManager()
        manager.refreshSessions()

        val sessions = manager.sessions.first()
        sessions.size shouldBe 1
        sessions[0].shouldBeInstanceOf<DebugSessionManager.LogSession.Failed>()
    }

    @Test
    fun `deleteLogSession removes session and updates flow`() = runTest {
        val sessionDir = File(logDirExternal, "session1").also { it.mkdirs() }
        File(sessionDir, "core.log").writeText("log content")
        File(logDirExternal, "session2").mkdirs()

        val manager = createManager()
        manager.refreshSessions()

        var sessions = manager.sessions.first()
        sessions.size shouldBe 2

        val toDelete = sessions.first { it.path.name == "session1" }
        manager.deleteLogSession(toDelete)

        sessions = manager.sessions.first()
        sessions.size shouldBe 1
        sessions[0].path.name shouldBe "session2"
    }

    @Test
    fun `deleteLogSession also removes associated zip`() = runTest {
        val sessionDir = File(logDirExternal, "session1").also { it.mkdirs() }
        File(sessionDir, "core.log").writeText("log content")
        val zip = File(logDirExternal, "session1.zip").also { it.createNewFile() }

        val manager = createManager()
        manager.refreshSessions()

        val sessions = manager.sessions.first()
        sessions.size shouldBe 2

        val dirSession = sessions.first { it.path == sessionDir }
        manager.deleteLogSession(dirSession)

        sessionDir.exists() shouldBe false
        zip.exists() shouldBe false
    }

    @Test
    fun `deleteAllLogs deletes all sessions and updates flow`() = runTest {
        val sessionDir = File(logDirExternal, "session1").also { it.mkdirs() }
        File(sessionDir, "core.log").writeText("content")
        File(logDirExternal, "recording.zip").createNewFile()
        File(logDirCache, "legacy.log").createNewFile()

        val manager = createManager()
        manager.refreshSessions()

        manager.sessions.first().size shouldBe 3

        manager.deleteAllLogs()

        manager.sessions.first().size shouldBe 0
    }

    @Test
    fun `scanSessions calculates correct size and fileCount for directories`() = runTest {
        val sessionDir = File(logDirExternal, "session1").also { it.mkdirs() }
        File(sessionDir, "core.log").writeText("A".repeat(100))
        File(sessionDir, "extra.log").writeText("B".repeat(50))

        val manager = createManager()
        manager.refreshSessions()

        val sessions = manager.sessions.first()
        sessions.size shouldBe 1
        sessions[0].shouldBeInstanceOf<DebugSessionManager.LogSession.Ready>()
        sessions[0].size shouldBe 150L
        sessions[0].fileCount shouldBe 2
    }

    @Test
    fun `scanSessions returns sessions sorted by lastModified descending`() = runTest {
        val older = File(logDirExternal, "session_old").also { it.mkdirs() }
        older.setLastModified(1000L)
        val newer = File(logDirExternal, "session_new").also { it.mkdirs() }
        newer.setLastModified(2000L)

        val manager = createManager()
        manager.refreshSessions()

        val sessions = manager.sessions.first()
        sessions.size shouldBe 2
        sessions[0].path.name shouldBe "session_new"
        sessions[1].path.name shouldBe "session_old"
    }
}
