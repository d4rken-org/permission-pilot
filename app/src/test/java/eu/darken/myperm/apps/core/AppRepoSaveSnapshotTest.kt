package eu.darken.myperm.apps.core

import android.content.Context
import android.os.Process
import android.os.UserHandle
import androidx.work.WorkManager
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.manifest.ManifestHintRepo
import eu.darken.myperm.common.room.PermPilotDatabase
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.common.room.entity.PkgType
import eu.darken.myperm.common.room.entity.SnapshotEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity
import eu.darken.myperm.common.room.entity.TriggerReason
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.runTest2

class AppRepoSaveSnapshotTest : BaseTest() {

    private val context: Context = mockk(relaxed = true)
    private val packageEventListener: PackageEventListener = mockk()
    private val appSourcer: AppSourcer = mockk()
    private val database: PermPilotDatabase = mockk()
    private val snapshotDao: SnapshotDao = mockk()
    private val snapshotPkgDao: SnapshotPkgDao = mockk()
    private val snapshotMapper: SnapshotMapper = mockk()
    private val workManager: WorkManager = mockk(relaxed = true)
    private val manifestHintRepo: ManifestHintRepo = mockk(relaxed = true)

    private val capturedPkgs = mutableListOf<List<SnapshotPkgEntity>>()
    private val capturedPerms = mutableListOf<List<SnapshotPkgPermEntity>>()
    private val capturedDeclaredPerms = mutableListOf<List<SnapshotPkgDeclaredPermEntity>>()
    private val capturedSnapshot = slot<SnapshotEntity>()
    private var transactionCount = 0

    @BeforeEach
    fun setup() {
        mockkStatic(Process::class)
        every { Process.myUserHandle() } returns mockk<UserHandle>()

        every { packageEventListener.events } returns MutableSharedFlow()
        every { snapshotDao.observeLatestSnapshotId() } returns flowOf(null)
        coEvery { snapshotDao.getOldSnapshotIds(any()) } returns emptyList()

        capturedPkgs.clear()
        capturedPerms.clear()
        capturedDeclaredPerms.clear()
        transactionCount = 0

        @Suppress("UNCHECKED_CAST")
        coEvery { database.inTransaction(any<suspend () -> Any?>()) } coAnswers {
            transactionCount++
            val block = firstArg<suspend () -> Any?>()
            block()
        }

        coEvery { snapshotDao.insertSnapshot(capture(capturedSnapshot)) } just Runs
        coEvery { snapshotPkgDao.insertPkgs(capture(capturedPkgs)) } just Runs
        coEvery { snapshotPkgDao.insertPermissions(capture(capturedPerms)) } just Runs
        coEvery { snapshotPkgDao.insertDeclaredPermissions(capture(capturedDeclaredPerms)) } just Runs
    }

    private fun createPkg(name: String, permCount: Int = 2, declaredPermCount: Int = 0): BasePkg {
        val pkg = mockk<BasePkg>(relaxed = true)
        every { pkg.id } returns Pkg.Id(Pkg.Name(name))
        every { pkg.getLabel(any()) } returns name

        val snapshotId = "test-snapshot"
        val pkgEntity = SnapshotPkgEntity(
            snapshotId = snapshotId,
            pkgName = Pkg.Name(name),
            userHandleId = 0,
            pkgType = PkgType.PRIMARY,
            versionName = "1.0",
            versionCode = 1L,
            sharedUserId = null,
            apiTargetLevel = 33,
            apiCompileLevel = null,
            apiMinimumLevel = null,
            isSystemApp = false,
            installedAt = null,
            updatedAt = null,
            internetAccess = InternetAccess.UNKNOWN,
            batteryOptimization = BatteryOptimization.UNKNOWN,
            installerPkgName = null,
            applicationFlags = 0,
            cachedLabel = name,
            twinCount = 0,
            siblingCount = 0,
            hasAccessibilityServices = false,
            hasDeviceAdmin = false,
            allInstallerPkgNames = null,
        )

        val permEntities = (1..permCount).map { i ->
            SnapshotPkgPermEntity(
                snapshotId = snapshotId,
                pkgName = Pkg.Name(name),
                userHandleId = 0,
                permissionId = "android.permission.PERM_${name}_$i",
                status = "GRANTED",
            )
        }

        val declaredPermEntities = (1..declaredPermCount).map { i ->
            SnapshotPkgDeclaredPermEntity(
                snapshotId = snapshotId,
                pkgName = Pkg.Name(name),
                userHandleId = 0,
                permissionId = "com.$name.permission.DECLARED_$i",
                protectionLevel = 0,
            )
        }

        coEvery { snapshotMapper.toEntities(any(), pkg, any()) } returns SnapshotMapper.PkgEntities(
            pkg = pkgEntity.copy(snapshotId = ""), // snapshotId is set dynamically
            permissions = permEntities.map { it.copy(snapshotId = "") },
            declaredPermissions = declaredPermEntities.map { it.copy(snapshotId = "") },
        )

        return pkg
    }

    private fun createAppRepo(scope: TestScope): AppRepo {
        val repo = AppRepo(
            context = context,
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
        return repo
    }

    /** Let init's APP_LAUNCH scan settle, then clear captured state for the actual test. */
    private fun TestScope.settleInit() {
        advanceTimeBy(100)
        capturedPkgs.clear()
        capturedPerms.clear()
        capturedDeclaredPerms.clear()
        transactionCount = 0
    }

    @Test
    fun `scanAndSave inserts all packages across chunks`() = runTest2(autoCancel = true) {
        val repo = createAppRepo(this)
        settleInit()

        val pkgs = (1..120).map { createPkg("pkg$it", permCount = 3, declaredPermCount = 1) }
        coEvery { appSourcer.scanPackages() } returns pkgs
        repo.scanAndSave(TriggerReason.MANUAL_REFRESH)

        // 120 pkgs / chunk size 50 = 3 chunks
        val totalPkgs = capturedPkgs.flatten()
        val totalPerms = capturedPerms.flatten()
        val totalDeclared = capturedDeclaredPerms.flatten()

        totalPkgs.size shouldBe 120
        totalPerms.size shouldBe 360  // 120 * 3
        totalDeclared.size shouldBe 120  // 120 * 1
    }

    @Test
    fun `scanAndSave uses single transaction for atomicity`() = runTest2(autoCancel = true) {
        val repo = createAppRepo(this)
        settleInit()

        val pkgs = (1..120).map { createPkg("pkg$it") }
        coEvery { appSourcer.scanPackages() } returns pkgs
        repo.scanAndSave(TriggerReason.MANUAL_REFRESH)

        transactionCount shouldBe 1
    }

    @Test
    fun `scanAndSave inserts snapshot entity with correct pkg count`() = runTest2(autoCancel = true) {
        val repo = createAppRepo(this)
        settleInit()

        val pkgs = (1..75).map { createPkg("pkg$it") }
        coEvery { appSourcer.scanPackages() } returns pkgs
        repo.scanAndSave(TriggerReason.MANUAL_REFRESH)

        capturedSnapshot.captured.pkgCount shouldBe 75
        capturedSnapshot.captured.triggerReason shouldBe TriggerReason.MANUAL_REFRESH.name
    }

    @Test
    fun `scanAndSave handles empty package list`() = runTest2(autoCancel = true) {
        val repo = createAppRepo(this)
        settleInit()

        coEvery { appSourcer.scanPackages() } returns emptyList()
        repo.scanAndSave(TriggerReason.MANUAL_REFRESH)

        capturedSnapshot.captured.pkgCount shouldBe 0
        capturedPkgs.flatten().size shouldBe 0
    }

    @Test
    fun `scanAndSave survives label resolution failure`() = runTest2(autoCancel = true) {
        val repo = createAppRepo(this)
        settleInit()

        val goodPkg = createPkg("good.app")
        val badPkg = createPkg("bad.app")
        every { badPkg.getLabel(any()) } throws RuntimeException("Corrupted APK")

        coEvery { appSourcer.scanPackages() } returns listOf(goodPkg, badPkg)
        repo.scanAndSave(TriggerReason.MANUAL_REFRESH)

        // Both packages should still be inserted despite label failure
        val totalPkgs = capturedPkgs.flatten()
        totalPkgs.size shouldBe 2
    }

    @Test
    fun `labels resolve before the transaction and mapper receives resolved label`() =
        runTest2(autoCancel = true) {
            val repo = createAppRepo(this)
            settleInit()

            // Gate: flip to true when database.inTransaction is entered. After that point,
            // any call to pkg.getLabel() indicates a regression (label resolution leaked
            // back into the transaction).
            val inTransaction = java.util.concurrent.atomic.AtomicBoolean(false)
            @Suppress("UNCHECKED_CAST")
            coEvery { database.inTransaction(any<suspend () -> Any?>()) } coAnswers {
                inTransaction.set(true)
                val block = firstArg<suspend () -> Any?>()
                val result = block()
                inTransaction.set(false)
                result
            }

            val pkg = mockk<BasePkg>(relaxed = true)
            every { pkg.id } returns Pkg.Id(Pkg.Name("single.pkg"))
            every { pkg.getLabel(any()) } answers {
                if (inTransaction.get()) {
                    error("pkg.getLabel() must not be called inside database.inTransaction")
                }
                "Resolved Label"
            }
            val capturedLabel = slot<String>()
            coEvery {
                snapshotMapper.toEntities(any(), pkg, capture(capturedLabel))
            } returns SnapshotMapper.PkgEntities(
                pkg = SnapshotPkgEntity(
                    snapshotId = "",
                    pkgName = Pkg.Name("single.pkg"),
                    userHandleId = 0,
                    pkgType = PkgType.PRIMARY,
                    versionName = "1.0",
                    versionCode = 1L,
                    sharedUserId = null,
                    apiTargetLevel = 33,
                    apiCompileLevel = null,
                    apiMinimumLevel = null,
                    isSystemApp = false,
                    installedAt = null,
                    updatedAt = null,
                    internetAccess = InternetAccess.UNKNOWN,
                    batteryOptimization = BatteryOptimization.UNKNOWN,
                    installerPkgName = null,
                    applicationFlags = 0,
                    cachedLabel = "Resolved Label",
                    twinCount = 0,
                    siblingCount = 0,
                    hasAccessibilityServices = false,
                    hasDeviceAdmin = false,
                    allInstallerPkgNames = null,
                ),
                permissions = emptyList(),
                declaredPermissions = emptyList(),
            )

            coEvery { appSourcer.scanPackages() } returns listOf(pkg)
            repo.scanAndSave(TriggerReason.MANUAL_REFRESH)

            capturedLabel.captured shouldBe "Resolved Label"
        }

    @Test
    fun `scanAndSave rejects duplicate Pkg Ids from the scanner`() = runTest2(autoCancel = true) {
        val repo = createAppRepo(this)
        settleInit()

        // Two BasePkg mocks share the same Pkg.Id instance, matching the scanner regression
        // where LauncherApps.getActivityList emitted two SecondaryProfilePkg objects for the
        // same (packageName, userHandle).
        val duplicateId = Pkg.Id(Pkg.Name("duplicate.app"))
        val pkg1 = mockk<BasePkg>(relaxed = true).also { every { it.id } returns duplicateId }
        val pkg2 = mockk<BasePkg>(relaxed = true).also { every { it.id } returns duplicateId }

        coEvery { appSourcer.scanPackages() } returns listOf(pkg1, pkg2)

        val thrown = shouldThrow<IllegalStateException> {
            repo.scanAndSave(TriggerReason.MANUAL_REFRESH)
        }
        thrown.message!! shouldContain "duplicate.app"

        coVerify(exactly = 0) { snapshotPkgDao.insertPkgs(any()) }
        coVerify(exactly = 0) { snapshotDao.insertSnapshot(any()) }
    }

    @Test
    fun `scanAndSave works with fewer packages than chunk size`() = runTest2(autoCancel = true) {
        val repo = createAppRepo(this)
        settleInit()

        val pkgs = (1..10).map { createPkg("pkg$it", permCount = 5) }
        coEvery { appSourcer.scanPackages() } returns pkgs
        repo.scanAndSave(TriggerReason.MANUAL_REFRESH)

        val totalPkgs = capturedPkgs.flatten()
        val totalPerms = capturedPerms.flatten()

        totalPkgs.size shouldBe 10
        totalPerms.size shouldBe 50  // 10 * 5
    }
}
