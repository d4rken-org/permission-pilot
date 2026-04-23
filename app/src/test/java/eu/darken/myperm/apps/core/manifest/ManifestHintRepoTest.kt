package eu.darken.myperm.apps.core.manifest

import androidx.work.WorkManager
import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.PermissionUse
import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.common.room.dao.ManifestHintDao
import eu.darken.myperm.common.room.entity.PkgType
import io.kotest.matchers.nulls.shouldBeNull
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.runTest2
import java.time.Instant

class ManifestHintRepoTest : BaseTest() {

    private lateinit var manifestHintDao: ManifestHintDao
    private lateinit var manifestRepo: ManifestRepo
    private lateinit var manifestHintScanner: ManifestHintScanner
    private lateinit var workManager: WorkManager
    private lateinit var appScope: CoroutineScope

    @BeforeEach
    fun setup() {
        manifestHintDao = mockk(relaxed = true)
        manifestRepo = mockk(relaxed = true)
        manifestHintScanner = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

        every { manifestHintDao.observeAll() } returns emptyFlow()
        coEvery { manifestHintDao.getAll() } returns emptyList()
        coEvery { manifestHintDao.upsertHints(any()) } just Runs
        coEvery { manifestHintDao.deleteByPkgName(any()) } just Runs
        coEvery { manifestHintDao.pruneStale() } just Runs
        every { manifestHintScanner.evaluate(any()) } returns ManifestHintScanner.Flags(
            hasActionMainQuery = false,
            packageQueryCount = 0,
            intentQueryCount = 0,
            providerQueryCount = 0,
        )
    }

    @AfterEach
    fun teardown() {
        appScope.cancel()
    }

    private fun buildRepo() = ManifestHintRepo(
        appScope = appScope,
        manifestHintDao = manifestHintDao,
        manifestRepo = manifestRepo,
        manifestHintScanner = manifestHintScanner,
        workManager = workManager,
    )

    private fun appInfo(pkgName: String, versionCode: Long = 1L, lastUpdate: Long = 1_000L) = AppInfo(
        pkgName = Pkg.Name(pkgName),
        userHandleId = 0,
        label = pkgName,
        versionName = "1.0",
        versionCode = versionCode,
        isSystemApp = false,
        installerPkgName = null,
        apiTargetLevel = 33,
        apiCompileLevel = null,
        apiMinimumLevel = null,
        internetAccess = InternetAccess.UNKNOWN,
        batteryOptimization = BatteryOptimization.UNKNOWN,
        installedAt = null,
        updatedAt = Instant.ofEpochMilli(lastUpdate),
        requestedPermissions = emptyList<PermissionUse>(),
        declaredPermissionCount = 0,
        pkgType = PkgType.PRIMARY,
        twinCount = 0,
        siblingCount = 0,
        hasAccessibilityServices = false,
        hasDeviceAdmin = false,
        allInstallerPkgNames = emptyList(),
        sharedUserId = null,
        hasManifestFlags = null,
    )

    @Test
    fun `Success outcome upserts hint`() = runTest2(autoCancel = true) {
        val pkgName = Pkg.Name("com.foo")
        coEvery { manifestRepo.getQueriesFor(pkgName) } returns QueriesOutcome.Success(QueriesInfo())

        buildRepo().runScan(listOf(appInfo("com.foo")))

        coVerify(atLeast = 1) { manifestHintDao.upsertHints(any()) }
        coVerify(exactly = 0) { manifestHintDao.deleteByPkgName(any()) }
    }

    @Test
    fun `APK_TOO_LARGE with existing version-mismatched hint deletes stale entry`() = runTest2(autoCancel = true) {
        val pkgName = Pkg.Name("com.stale")
        val staleHint = ManifestHintEntity(
            pkgName = pkgName,
            versionCode = 1L,            // old version
            lastUpdateTime = 900L,
            hasActionMainQuery = true,
            packageQueryCount = 5,
            intentQueryCount = 0,
            providerQueryCount = 0,
            scannedAt = 0L,
        )
        coEvery { manifestHintDao.getAll() } returns listOf(staleHint)
        coEvery { manifestRepo.getQueriesFor(pkgName) } returns QueriesOutcome.Unavailable(UnavailableReason.APK_TOO_LARGE)

        // New scan sees a different versionCode — the old hint must be evicted.
        buildRepo().runScan(listOf(appInfo("com.stale", versionCode = 2L, lastUpdate = 2_000L)))

        coVerify(exactly = 1) { manifestHintDao.deleteByPkgName(pkgName) }
        coVerify(exactly = 0) { manifestHintDao.upsertHints(any()) }
    }

    @Test
    fun `LOW_MEMORY with no existing hint is a no-op`() = runTest2(autoCancel = true) {
        val pkgName = Pkg.Name("com.clean")
        coEvery { manifestHintDao.getAll() } returns emptyList()
        coEvery { manifestRepo.getQueriesFor(pkgName) } returns QueriesOutcome.Unavailable(UnavailableReason.LOW_MEMORY)

        buildRepo().runScan(listOf(appInfo("com.clean")))

        coVerify(exactly = 0) { manifestHintDao.deleteByPkgName(any()) }
        coVerify(exactly = 0) { manifestHintDao.upsertHints(any()) }
    }

    @Test
    fun `Failure outcome with existing mismatched hint deletes stale entry`() = runTest2(autoCancel = true) {
        val pkgName = Pkg.Name("com.boom")
        val staleHint = ManifestHintEntity(
            pkgName = pkgName,
            versionCode = 1L,
            lastUpdateTime = 900L,
            hasActionMainQuery = true,
            packageQueryCount = 3,
            intentQueryCount = 0,
            providerQueryCount = 0,
            scannedAt = 0L,
        )
        coEvery { manifestHintDao.getAll() } returns listOf(staleHint)
        coEvery { manifestRepo.getQueriesFor(pkgName) } returns QueriesOutcome.Failure(RuntimeException("parser"))

        buildRepo().runScan(listOf(appInfo("com.boom", versionCode = 2L, lastUpdate = 2_000L)))

        coVerify(exactly = 1) { manifestHintDao.deleteByPkgName(pkgName) }
    }

    @Test
    fun `exception during scan clears scan state`() = runTest2(autoCancel = true) {
        val pkgName = Pkg.Name("com.throws")
        coEvery { manifestRepo.getQueriesFor(pkgName) } throws RuntimeException("boom")

        val repo = buildRepo()
        try {
            repo.runScan(listOf(appInfo("com.throws")))
        } catch (_: RuntimeException) {
            // expected — runScan doesn't swallow here, finally still runs.
        }

        repo.currentlyScanning.value.shouldBeNull()
        repo.scanProgress.value.shouldBeNull()
    }
}
