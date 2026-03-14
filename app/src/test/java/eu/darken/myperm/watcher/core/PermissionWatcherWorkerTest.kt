package eu.darken.myperm.watcher.core

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class PermissionWatcherWorkerTest : BaseTest() {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val watcherManager: WatcherManager = mockk()

    @BeforeEach
    fun setup() {
        coEvery { watcherManager.processChanges() } returns 0
    }

    private fun createWorker() = PermissionWatcherWorker(
        context = context,
        params = params,
        watcherManager = watcherManager,
    )

    @Test
    fun `delegates to WatcherManager and returns success`() = runTest {
        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.success()
        coVerify { watcherManager.processChanges() }
    }

    @Test
    fun `returns retry on exception`() = runTest {
        coEvery { watcherManager.processChanges() } throws RuntimeException("test error")

        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.retry()
    }
}
