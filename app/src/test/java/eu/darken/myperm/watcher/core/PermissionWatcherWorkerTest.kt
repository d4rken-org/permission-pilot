package eu.darken.myperm.watcher.core

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity
import eu.darken.myperm.settings.core.GeneralSettings
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelpers.datastore.mockDataStoreValue

class PermissionWatcherWorkerTest : BaseTest() {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val appRepo: AppRepo = mockk()
    private val snapshotDiffer = SnapshotDiffer()
    private val changeDao: PermissionChangeDao = mockk(relaxUnitFun = true)
    private val watcherNotifications: WatcherNotifications = mockk(relaxUnitFun = true)
    private val generalSettings: GeneralSettings = mockk()
    private val json = Json { ignoreUnknownKeys = true }

    private val isWatcherEnabled = mockDataStoreValue(true)
    private val watcherScope = mockDataStoreValue(WatcherScope.ALL)
    private val lastDiffedSnapshotId = mockDataStoreValue<String?>("snap-old")

    @BeforeEach
    fun setup() {
        every { generalSettings.isWatcherEnabled } returns isWatcherEnabled
        every { generalSettings.watcherScope } returns watcherScope
        every { generalSettings.lastDiffedSnapshotId } returns lastDiffedSnapshotId
        coEvery { changeDao.insert(any()) } returns 1L
        coEvery { watcherNotifications.postChangeNotification(any(), any(), any(), any()) } returns Unit
    }

    private fun createWorker() = PermissionWatcherWorker(
        context = context,
        params = params,
        appRepo = appRepo,
        snapshotDiffer = snapshotDiffer,
        changeDao = changeDao,
        watcherNotifications = watcherNotifications,
        generalSettings = generalSettings,
        json = json,
    )

    private fun pkg(
        snapshotId: String,
        pkgName: String,
        isSystemApp: Boolean = false,
        cachedLabel: String? = pkgName,
    ) = SnapshotPkgEntity(
        snapshotId = snapshotId,
        pkgName = pkgName,
        userHandleId = 0,
        pkgType = "INSTALLED",
        versionName = "1.0",
        versionCode = 1L,
        sharedUserId = null,
        apiTargetLevel = 34,
        apiCompileLevel = 34,
        apiMinimumLevel = 21,
        isSystemApp = isSystemApp,
        installedAt = null,
        updatedAt = null,
        internetAccess = "NONE",
        batteryOptimization = "UNKNOWN",
        installerPkgName = null,
        applicationFlags = 0,
        cachedLabel = cachedLabel,
    )

    private fun perm(snapshotId: String, pkgName: String, permId: String, status: String = "GRANTED") =
        SnapshotPkgPermEntity(
            snapshotId = snapshotId,
            pkgName = pkgName,
            userHandleId = 0,
            permissionId = permId,
            status = status,
        )

    private fun declaredPerm(snapshotId: String, pkgName: String, permId: String) =
        SnapshotPkgDeclaredPermEntity(
            snapshotId = snapshotId,
            pkgName = pkgName,
            userHandleId = 0,
            permissionId = permId,
            protectionLevel = null,
        )

    private fun setupChain(
        oldPkgs: List<SnapshotPkgEntity>,
        newPkgs: List<SnapshotPkgEntity>,
        oldPermsRequested: List<SnapshotPkgPermEntity> = emptyList(),
        oldPermsDeclared: List<SnapshotPkgDeclaredPermEntity> = emptyList(),
        newPermsRequested: List<SnapshotPkgPermEntity> = emptyList(),
        newPermsDeclared: List<SnapshotPkgDeclaredPermEntity> = emptyList(),
    ) {
        coEvery { appRepo.getLatestSnapshotId() } returns "snap-new"
        coEvery { appRepo.getSnapshotChainSince("snap-old") } returns AppRepo.SnapshotChain(
            latestSnapshotId = "snap-new",
            pairs = listOf(
                AppRepo.SnapshotPair(
                    oldSnapshotId = "snap-old",
                    newSnapshotId = "snap-new",
                    oldPkgs = oldPkgs,
                    newPkgs = newPkgs,
                )
            ),
        )
        coEvery { appRepo.getSnapshotPermissions("snap-old") } returns AppRepo.SnapshotPermissions(
            requested = oldPermsRequested.groupBy { Pair(it.pkgName, it.userHandleId) },
            declared = oldPermsDeclared.groupBy { Pair(it.pkgName, it.userHandleId) },
        )
        coEvery { appRepo.getSnapshotPermissions("snap-new") } returns AppRepo.SnapshotPermissions(
            requested = newPermsRequested.groupBy { Pair(it.pkgName, it.userHandleId) },
            declared = newPermsDeclared.groupBy { Pair(it.pkgName, it.userHandleId) },
        )
    }

    @Test
    fun `new install with permissions creates INSTALL report`() = runTest {
        setupChain(
            oldPkgs = emptyList(),
            newPkgs = listOf(pkg("snap-new", "com.new.app")),
            newPermsRequested = listOf(
                perm("snap-new", "com.new.app", "android.permission.CAMERA"),
                perm("snap-new", "com.new.app", "android.permission.INTERNET"),
            ),
        )

        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.success()

        val slot = slot<PermissionChangeEntity>()
        coVerify { changeDao.insert(capture(slot)) }

        slot.captured.eventType shouldBe "INSTALL"
        slot.captured.packageName shouldBe "com.new.app"

        val diff = json.decodeFromString<PermissionDiff>(slot.captured.changesJson)
        diff.addedPermissions shouldBe listOf("android.permission.CAMERA", "android.permission.INTERNET")
        diff.removedPermissions shouldBe emptyList()
    }

    @Test
    fun `new install without permissions creates INSTALL report`() = runTest {
        setupChain(
            oldPkgs = emptyList(),
            newPkgs = listOf(pkg("snap-new", "com.simple.app")),
        )

        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.success()

        val slot = slot<PermissionChangeEntity>()
        coVerify { changeDao.insert(capture(slot)) }

        slot.captured.eventType shouldBe "INSTALL"
        slot.captured.packageName shouldBe "com.simple.app"

        val diff = json.decodeFromString<PermissionDiff>(slot.captured.changesJson)
        diff.isEmpty shouldBe true
    }

    @Test
    fun `update with permission changes creates UPDATE report`() = runTest {
        setupChain(
            oldPkgs = listOf(pkg("snap-old", "com.existing.app")),
            newPkgs = listOf(pkg("snap-new", "com.existing.app")),
            oldPermsRequested = listOf(
                perm("snap-old", "com.existing.app", "android.permission.INTERNET"),
            ),
            newPermsRequested = listOf(
                perm("snap-new", "com.existing.app", "android.permission.INTERNET"),
                perm("snap-new", "com.existing.app", "android.permission.CAMERA"),
            ),
        )

        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.success()

        val slot = slot<PermissionChangeEntity>()
        coVerify { changeDao.insert(capture(slot)) }

        slot.captured.eventType shouldBe "UPDATE"
        val diff = json.decodeFromString<PermissionDiff>(slot.captured.changesJson)
        diff.addedPermissions shouldBe listOf("android.permission.CAMERA")
    }

    @Test
    fun `update without permission changes creates no report`() = runTest {
        setupChain(
            oldPkgs = listOf(pkg("snap-old", "com.existing.app")),
            newPkgs = listOf(pkg("snap-new", "com.existing.app")),
            oldPermsRequested = listOf(
                perm("snap-old", "com.existing.app", "android.permission.INTERNET"),
            ),
            newPermsRequested = listOf(
                perm("snap-new", "com.existing.app", "android.permission.INTERNET"),
            ),
        )

        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.success()

        coVerify(exactly = 0) { changeDao.insert(any()) }
    }

    @Test
    fun `removed package creates REMOVED report`() = runTest {
        setupChain(
            oldPkgs = listOf(pkg("snap-old", "com.removed.app")),
            newPkgs = emptyList(),
            oldPermsRequested = listOf(
                perm("snap-old", "com.removed.app", "android.permission.CAMERA"),
            ),
        )

        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.success()

        val slot = slot<PermissionChangeEntity>()
        coVerify { changeDao.insert(capture(slot)) }

        slot.captured.eventType shouldBe "REMOVED"
        val diff = json.decodeFromString<PermissionDiff>(slot.captured.changesJson)
        diff.removedPermissions shouldBe listOf("android.permission.CAMERA")
    }

    @Test
    fun `system apps are skipped when scope is NON_SYSTEM`() = runTest {
        watcherScope.value(WatcherScope.NON_SYSTEM)

        setupChain(
            oldPkgs = emptyList(),
            newPkgs = listOf(pkg("snap-new", "com.system.app", isSystemApp = true)),
            newPermsRequested = listOf(
                perm("snap-new", "com.system.app", "android.permission.CAMERA"),
            ),
        )

        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.success()

        coVerify(exactly = 0) { changeDao.insert(any()) }
    }

    @Test
    fun `disabled watcher skips processing`() = runTest {
        isWatcherEnabled.value(false)

        val result = createWorker().doWork()
        result shouldBe ListenableWorker.Result.success()

        coVerify(exactly = 0) { changeDao.insert(any()) }
    }
}
