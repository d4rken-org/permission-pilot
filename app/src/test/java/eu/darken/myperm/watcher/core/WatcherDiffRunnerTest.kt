package eu.darken.myperm.watcher.core

import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.room.entity.SnapshotEntity
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
import eu.darken.myperm.watcher.core.WatcherEventType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelpers.datastore.mockDataStoreValue

class WatcherDiffRunnerTest : BaseTest() {

    private val snapshotDao: SnapshotDao = mockk()
    private val snapshotPkgDao: SnapshotPkgDao = mockk()
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
        coEvery { changeDao.existsByPackageAndSnapshot(any(), any(), any()) } returns false
        coEvery { watcherNotifications.postChangeNotification(any(), any(), any(), any()) } returns Unit
        coEvery { watcherNotifications.postSummaryNotification(any()) } returns Unit
    }

    private fun createRunner() = WatcherDiffRunner(
        snapshotDao = snapshotDao,
        snapshotPkgDao = snapshotPkgDao,
        snapshotDiffer = snapshotDiffer,
        changeDao = changeDao,
        watcherNotifications = watcherNotifications,
        generalSettings = generalSettings,
        json = json,
    )

    private fun snapshotEntity(snapshotId: String, createdAt: Long = 0L) = SnapshotEntity(
        snapshotId = snapshotId,
        createdAt = createdAt,
        triggerReason = "TEST",
        pkgCount = 0,
        durationMs = 0L,
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

    private data class PairSetup(
        val oldSnapshotId: String,
        val newSnapshotId: String,
        val oldPkgs: List<SnapshotPkgEntity>,
        val newPkgs: List<SnapshotPkgEntity>,
    )

    private fun setupChain(
        pairs: List<PairSetup>,
        latestSnapshotId: String = pairs.last().newSnapshotId,
        permsRequested: Map<String, List<SnapshotPkgPermEntity>> = emptyMap(),
        permsDeclared: Map<String, List<SnapshotPkgDeclaredPermEntity>> = emptyMap(),
    ) {
        val anchorId = pairs.first().oldSnapshotId
        coEvery { snapshotDao.getLatestSnapshot() } returns snapshotEntity(latestSnapshotId, 1000L)
        coEvery { snapshotDao.getSnapshotById(anchorId) } returns snapshotEntity(anchorId, 0L)
        coEvery { snapshotDao.getSnapshotsAfter(anchorId) } returns pairs.mapIndexed { index, pair ->
            snapshotEntity(pair.newSnapshotId, (index + 1) * 100L)
        }

        val pkgsPerSnapshot = mutableMapOf<String, List<SnapshotPkgEntity>>()
        for (pair in pairs) {
            pkgsPerSnapshot.getOrPut(pair.oldSnapshotId) { pair.oldPkgs }
            pkgsPerSnapshot[pair.newSnapshotId] = pair.newPkgs
        }

        for (snapshotId in pkgsPerSnapshot.keys) {
            coEvery { snapshotPkgDao.getPkgsForSnapshot(snapshotId) } returns (pkgsPerSnapshot[snapshotId] ?: emptyList())
            coEvery { snapshotPkgDao.getPermsForSnapshot(snapshotId) } returns (permsRequested[snapshotId] ?: emptyList())
            coEvery { snapshotPkgDao.getDeclaredPermsForSnapshot(snapshotId) } returns (permsDeclared[snapshotId] ?: emptyList())
        }
    }

    private fun setupSinglePairChain(
        oldPkgs: List<SnapshotPkgEntity>,
        newPkgs: List<SnapshotPkgEntity>,
        oldPermsRequested: List<SnapshotPkgPermEntity> = emptyList(),
        oldPermsDeclared: List<SnapshotPkgDeclaredPermEntity> = emptyList(),
        newPermsRequested: List<SnapshotPkgPermEntity> = emptyList(),
        newPermsDeclared: List<SnapshotPkgDeclaredPermEntity> = emptyList(),
    ) {
        setupChain(
            pairs = listOf(
                PairSetup(
                    oldSnapshotId = "snap-old",
                    newSnapshotId = "snap-new",
                    oldPkgs = oldPkgs,
                    newPkgs = newPkgs,
                )
            ),
            permsRequested = mapOf(
                "snap-old" to oldPermsRequested,
                "snap-new" to newPermsRequested,
            ),
            permsDeclared = mapOf(
                "snap-old" to oldPermsDeclared,
                "snap-new" to newPermsDeclared,
            ),
        )
    }

    @Test
    fun `new install with permissions creates INSTALL report`() = runTest {
        setupSinglePairChain(
            oldPkgs = emptyList(),
            newPkgs = listOf(pkg("snap-new", "com.new.app")),
            newPermsRequested = listOf(
                perm("snap-new", "com.new.app", "android.permission.CAMERA"),
                perm("snap-new", "com.new.app", "android.permission.INTERNET"),
            ),
        )

        val count = createRunner().processNewSnapshots()
        count shouldBe 1

        val slot = slot<PermissionChangeEntity>()
        coVerify { changeDao.insert(capture(slot)) }

        slot.captured.eventType shouldBe WatcherEventType.INSTALL
        slot.captured.packageName shouldBe "com.new.app"
        slot.captured.sourceSnapshotId shouldBe "snap-new"

        val diff = json.decodeFromString<PermissionDiff>(slot.captured.changesJson)
        diff.addedPermissions shouldBe listOf("android.permission.CAMERA", "android.permission.INTERNET")
    }

    @Test
    fun `new install without permissions creates INSTALL report`() = runTest {
        setupSinglePairChain(
            oldPkgs = emptyList(),
            newPkgs = listOf(pkg("snap-new", "com.simple.app")),
        )

        val count = createRunner().processNewSnapshots()
        count shouldBe 1

        val slot = slot<PermissionChangeEntity>()
        coVerify { changeDao.insert(capture(slot)) }

        slot.captured.eventType shouldBe WatcherEventType.INSTALL
        slot.captured.packageName shouldBe "com.simple.app"
    }

    @Test
    fun `update with permission changes creates UPDATE report`() = runTest {
        setupSinglePairChain(
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

        val count = createRunner().processNewSnapshots()
        count shouldBe 1

        val slot = slot<PermissionChangeEntity>()
        coVerify { changeDao.insert(capture(slot)) }

        slot.captured.eventType shouldBe WatcherEventType.UPDATE
        val diff = json.decodeFromString<PermissionDiff>(slot.captured.changesJson)
        diff.addedPermissions shouldBe listOf("android.permission.CAMERA")
    }

    @Test
    fun `update without permission changes creates no report`() = runTest {
        setupSinglePairChain(
            oldPkgs = listOf(pkg("snap-old", "com.existing.app")),
            newPkgs = listOf(pkg("snap-new", "com.existing.app")),
            oldPermsRequested = listOf(
                perm("snap-old", "com.existing.app", "android.permission.INTERNET"),
            ),
            newPermsRequested = listOf(
                perm("snap-new", "com.existing.app", "android.permission.INTERNET"),
            ),
        )

        val count = createRunner().processNewSnapshots()
        count shouldBe 0

        coVerify(exactly = 0) { changeDao.insert(any()) }
    }

    @Test
    fun `removed package creates REMOVED report`() = runTest {
        setupSinglePairChain(
            oldPkgs = listOf(pkg("snap-old", "com.removed.app")),
            newPkgs = emptyList(),
            oldPermsRequested = listOf(
                perm("snap-old", "com.removed.app", "android.permission.CAMERA"),
            ),
        )

        val count = createRunner().processNewSnapshots()
        count shouldBe 1

        val slot = slot<PermissionChangeEntity>()
        coVerify { changeDao.insert(capture(slot)) }

        slot.captured.eventType shouldBe WatcherEventType.REMOVED
        val diff = json.decodeFromString<PermissionDiff>(slot.captured.changesJson)
        diff.removedPermissions shouldBe listOf("android.permission.CAMERA")
    }

    @Test
    fun `removed package without permissions creates REMOVED report`() = runTest {
        setupSinglePairChain(
            oldPkgs = listOf(pkg("snap-old", "com.empty.app")),
            newPkgs = emptyList(),
        )

        val count = createRunner().processNewSnapshots()
        count shouldBe 1

        val slot = slot<PermissionChangeEntity>()
        coVerify { changeDao.insert(capture(slot)) }

        slot.captured.eventType shouldBe WatcherEventType.REMOVED
        slot.captured.packageName shouldBe "com.empty.app"
        val diff = json.decodeFromString<PermissionDiff>(slot.captured.changesJson)
        diff.removedPermissions shouldBe emptyList()
        diff.removedDeclared shouldBe emptyList()
    }

    @Test
    fun `system apps are skipped when scope is NON_SYSTEM`() = runTest {
        watcherScope.value(WatcherScope.NON_SYSTEM)

        setupSinglePairChain(
            oldPkgs = emptyList(),
            newPkgs = listOf(pkg("snap-new", "com.system.app", isSystemApp = true)),
            newPermsRequested = listOf(
                perm("snap-new", "com.system.app", "android.permission.CAMERA"),
            ),
        )

        val count = createRunner().processNewSnapshots()
        count shouldBe 0

        coVerify(exactly = 0) { changeDao.insert(any()) }
    }

    @Test
    fun `disabled watcher skips processing`() = runTest {
        isWatcherEnabled.value(false)

        val count = createRunner().processNewSnapshots()
        count shouldBe 0

        coVerify(exactly = 0) { changeDao.insert(any()) }
    }

    @Test
    fun `progressive lastDiffedSnapshotId updated after each pair`() = runTest {
        setupChain(
            pairs = listOf(
                PairSetup(
                    oldSnapshotId = "snap-old",
                    newSnapshotId = "snap-mid",
                    oldPkgs = listOf(pkg("snap-old", "com.app")),
                    newPkgs = listOf(pkg("snap-mid", "com.app")),
                ),
                PairSetup(
                    oldSnapshotId = "snap-mid",
                    newSnapshotId = "snap-new",
                    oldPkgs = listOf(pkg("snap-mid", "com.app")),
                    newPkgs = listOf(pkg("snap-new", "com.app")),
                ),
            ),
            latestSnapshotId = "snap-new",
        )

        createRunner().processNewSnapshots()

        // After processing, lastDiffedSnapshotId should be snap-new (the last pair's newSnapshotId)
        lastDiffedSnapshotId.value() shouldBe "snap-new"
    }

    @Test
    fun `idempotent - re-running same snapshots does not create duplicate reports`() = runTest {
        setupSinglePairChain(
            oldPkgs = emptyList(),
            newPkgs = listOf(pkg("snap-new", "com.new.app")),
            newPermsRequested = listOf(
                perm("snap-new", "com.new.app", "android.permission.CAMERA"),
            ),
        )

        // Simulate that this report was already created
        coEvery { changeDao.existsByPackageAndSnapshot("com.new.app", 0, "snap-new") } returns true

        val count = createRunner().processNewSnapshots()
        count shouldBe 0

        coVerify(exactly = 0) { changeDao.insert(any()) }
    }

    @Test
    fun `multiple pairs in chain processes all and deduplicates across pairs`() = runTest {
        setupChain(
            pairs = listOf(
                PairSetup(
                    oldSnapshotId = "snap-old",
                    newSnapshotId = "snap-mid",
                    oldPkgs = emptyList(),
                    newPkgs = listOf(pkg("snap-mid", "com.app.a")),
                ),
                PairSetup(
                    oldSnapshotId = "snap-mid",
                    newSnapshotId = "snap-new",
                    oldPkgs = listOf(pkg("snap-mid", "com.app.a")),
                    newPkgs = listOf(
                        pkg("snap-new", "com.app.a"),
                        pkg("snap-new", "com.app.b"),
                    ),
                ),
            ),
            latestSnapshotId = "snap-new",
            permsRequested = mapOf(
                "snap-mid" to listOf(perm("snap-mid", "com.app.a", "android.permission.INTERNET")),
                "snap-new" to listOf(perm("snap-new", "com.app.a", "android.permission.INTERNET")),
            ),
        )

        val count = createRunner().processNewSnapshots()
        // com.app.a: INSTALL in pair 1, skipped in pair 2 (already reported)
        // com.app.b: INSTALL in pair 2
        count shouldBe 2

        coVerify(exactly = 2) { changeDao.insert(any()) }
        // Summary posted because reportCount > 1
        coVerify { watcherNotifications.postSummaryNotification(2) }
    }

    @Test
    fun `multi-pair chain with shared snapshot uses correct pkg data`() = runTest {
        // pair1.newPkgs for snap-mid has versionCode=1, pair2.oldPkgs has versionCode=2
        // getOrPut should keep pair1's newPkgs (first writer wins for the shared ID)
        val midPkgV1 = pkg("snap-mid", "com.app").copy(versionCode = 1L)
        val midPkgV2 = pkg("snap-mid", "com.app").copy(versionCode = 2L)
        val newPkg = pkg("snap-new", "com.app").copy(versionCode = 2L)

        setupChain(
            pairs = listOf(
                PairSetup(
                    oldSnapshotId = "snap-old",
                    newSnapshotId = "snap-mid",
                    oldPkgs = emptyList(),
                    newPkgs = listOf(midPkgV1),
                ),
                PairSetup(
                    oldSnapshotId = "snap-mid",
                    newSnapshotId = "snap-new",
                    // Different from pair1.newPkgs — setupChain should use pair1's value for snap-mid
                    oldPkgs = listOf(midPkgV2),
                    newPkgs = listOf(newPkg),
                ),
            ),
            latestSnapshotId = "snap-new",
        )

        val count = createRunner().processNewSnapshots()
        // pair1: INSTALL for com.app (new app in snap-mid)
        // pair2: com.app already reported, skipped
        count shouldBe 1

        val slot = slot<PermissionChangeEntity>()
        coVerify(exactly = 1) { changeDao.insert(capture(slot)) }
        slot.captured.eventType shouldBe WatcherEventType.INSTALL
        // versionCode should come from pair1's newPkgs (versionCode=1), not pair2's oldPkgs
        slot.captured.versionCode shouldBe 1L
    }

    @Test
    fun `first run with no lastDiffedSnapshotId fast-forwards without reports`() = runTest {
        lastDiffedSnapshotId.value(null)
        coEvery { snapshotDao.getLatestSnapshot() } returns snapshotEntity("snap-new")

        val count = createRunner().processNewSnapshots()
        count shouldBe 0

        coVerify(exactly = 0) { changeDao.insert(any()) }
        lastDiffedSnapshotId.value() shouldBe "snap-new"
    }

    @Test
    fun `summary notification not posted for single report`() = runTest {
        setupSinglePairChain(
            oldPkgs = emptyList(),
            newPkgs = listOf(pkg("snap-new", "com.single.app")),
        )

        val count = createRunner().processNewSnapshots()
        count shouldBe 1

        coVerify(exactly = 0) { watcherNotifications.postSummaryNotification(any()) }
    }
}
