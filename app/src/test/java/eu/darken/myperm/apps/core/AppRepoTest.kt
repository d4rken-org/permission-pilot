package eu.darken.myperm.apps.core

import android.os.Process
import android.os.UserHandle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import eu.darken.myperm.apps.core.manifest.ManifestHintRepo
import eu.darken.myperm.common.room.PermPilotDatabase
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import kotlinx.coroutines.flow.flowOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.runTest2

class AppRepoTest : BaseTest() {

    private val packageEventListener: PackageEventListener = mockk()
    private val appSourcer: AppSourcer = mockk()
    private val database: PermPilotDatabase = mockk()
    private val snapshotDao: SnapshotDao = mockk()
    private val snapshotPkgDao: SnapshotPkgDao = mockk()
    private val snapshotMapper: SnapshotMapper = mockk()
    private val workManager: WorkManager = mockk(relaxed = true)
    private val manifestHintRepo: ManifestHintRepo = mockk(relaxed = true)

    private val packageEvents = MutableSharedFlow<PackageEventListener.Event>()

    @BeforeEach
    fun setup() {
        mockkStatic(Process::class)
        every { Process.myUserHandle() } returns mockk<UserHandle>()

        every { packageEventListener.events } returns packageEvents

        @Suppress("UNCHECKED_CAST")
        coEvery { database.inTransaction(any<suspend () -> Any?>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }

        every { snapshotDao.observeLatestSnapshotId() } returns flowOf(null)
        coEvery { appSourcer.scanPackages() } returns emptyList()
        coEvery { snapshotDao.insertSnapshot(any()) } just Runs
        coEvery { snapshotPkgDao.insertPkgs(any()) } just Runs
        coEvery { snapshotPkgDao.insertPermissions(any()) } just Runs
        coEvery { snapshotPkgDao.insertDeclaredPermissions(any()) } just Runs
        coEvery { snapshotDao.getOldSnapshotIds(any()) } returns emptyList()
    }

    private fun createAppRepo(scope: TestScope) = AppRepo(
        context = mockk(relaxed = true),
        appScope = scope,
        packageEventListener = packageEventListener,
        appSourcer = appSourcer,
        database = database,
        snapshotDao = snapshotDao,
        snapshotPkgDao = snapshotPkgDao,
        snapshotMapper = snapshotMapper,
        workManager = workManager,
        manifestHintRepo = manifestHintRepo,
    )

    @Test
    fun `permission watcher is enqueued on app launch`() = runTest2(autoCancel = true) {
        createAppRepo(this)
        advanceTimeBy(100)

        verify {
            workManager.enqueueUniqueWork(
                eq("permission_watcher"),
                eq(ExistingWorkPolicy.KEEP),
                any<OneTimeWorkRequest>(),
            )
        }
    }

    @Test
    fun `permission watcher is enqueued after package change scan`() = runTest2(autoCancel = true) {
        createAppRepo(this)
        advanceTimeBy(100) // Let APP_LAUNCH complete first
        clearMocks(workManager, answers = false, recordedCalls = true, verificationMarks = true)

        packageEvents.emit(PackageEventListener.Event.PackageInstalled(Pkg.Id(Pkg.Name("com.test.app"))))
        advanceTimeBy(1_100) // Past the 1s debounce

        verify {
            workManager.enqueueUniqueWork(
                eq("permission_watcher"),
                eq(ExistingWorkPolicy.KEEP),
                any<OneTimeWorkRequest>(),
            )
        }
    }
}
