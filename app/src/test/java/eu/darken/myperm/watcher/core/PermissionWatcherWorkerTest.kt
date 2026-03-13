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
    private val watcherDiffRunner: WatcherDiffRunner = mockk()

    @BeforeEach
    fun setup() {
        coEvery { watcherDiffRunner.processNewSnapshots() } returns 0
    }

    private fun createWorker() = PermissionWatcherWorker(
        context = context,
        params = params,
        watcherDiffRunner = watcherDiffRunner,
    )

    @Test
    fun `delegates to WatcherDiffRunner and returns success`() = runTest {
        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.success()
        coVerify { watcherDiffRunner.processNewSnapshots() }
    }

    @Test
    fun `returns retry on exception`() = runTest {
        coEvery { watcherDiffRunner.processNewSnapshots() } throws RuntimeException("test error")

        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.retry()
    }
}
