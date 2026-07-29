package eu.darken.myperm.settings.ui.watcher

import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavEvent
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.watcher.core.WatcherNotificationCapability
import eu.darken.myperm.watcher.core.WatcherScope
import eu.darken.myperm.watcher.core.WatcherWorkScheduler
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider
import testhelpers.datastore.mockDataStoreValue

class WatcherSettingsViewModelTest : BaseTest() {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val isWatcherEnabled = mockDataStoreValue(true)
    private val watcherScope = mockDataStoreValue(WatcherScope.NON_SYSTEM)
    private val isNotificationsEnabled = mockDataStoreValue(true)
    private val isNotifyOnlyOnGained = mockDataStoreValue(true)
    private val retentionDays = mockDataStoreValue(30)
    private val pollingIntervalHours = mockDataStoreValue(4)
    private val isBatteryHintDismissed = mockDataStoreValue(false)

    private val generalSettings: GeneralSettings = mockk()
    private val changeDao: PermissionChangeDao = mockk(relaxed = true)
    private val watcherWorkScheduler: WatcherWorkScheduler = mockk(relaxed = true)
    private val notificationCapability: WatcherNotificationCapability = mockk(relaxed = true)
    private val upgradeRepo: UpgradeRepo = mockk(relaxed = true)

    /**
     * Hot flow, never a finite `flowOf`: `isProForUi` waits for a settled emission, and a finished
     * flow would push every gate onto its fail-open timeout path instead of the real decision.
     */
    private val upgradeInfo = MutableStateFlow(info(isPro = false))

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { generalSettings.isWatcherEnabled } returns isWatcherEnabled
        every { generalSettings.watcherScope } returns watcherScope
        every { generalSettings.isWatcherNotificationsEnabled } returns isNotificationsEnabled
        every { generalSettings.isWatcherNotifyOnlyOnGained } returns isNotifyOnlyOnGained
        every { generalSettings.watcherRetentionDays } returns retentionDays
        every { generalSettings.watcherPollingIntervalHours } returns pollingIntervalHours
        every { generalSettings.isWatcherBatteryHintDismissed } returns isBatteryHintDismissed
        every { changeDao.getTotalCount() } returns flowOf(0)
        every { upgradeRepo.upgradeInfo } returns upgradeInfo
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createVM() = WatcherSettingsViewModel(
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        generalSettings = generalSettings,
        changeDao = changeDao,
        watcherWorkScheduler = watcherWorkScheduler,
        upgradeRepo = upgradeRepo,
        notificationCapability = notificationCapability,
    )

    // --- presentation ---------------------------------------------------------------------------

    @Test
    fun `a settled free user sees the hard-locked upgrade presentation`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)

        createVM().isUpgradeLocked.first() shouldBe true
    }

    @Test
    fun `the unsettled cold start is not presented as hard-locked`() = runTest(testDispatcher) {
        // Rendering the locked row during the seed would also force a paying user's notification
        // preference off the moment they tap it.
        upgradeInfo.value = info(isPro = false, isSettled = false)

        createVM().isUpgradeLocked.first() shouldBe false
    }

    @Test
    fun `a settled error state is not presented as hard-locked`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true, error = IllegalStateException("nope"))

        createVM().isUpgradeLocked.first() shouldBe false
    }

    @Test
    fun `a pro user is never hard-locked`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = true, isSettled = true)

        createVM().isUpgradeLocked.first() shouldBe false
    }

    // --- setter gates ---------------------------------------------------------------------------

    @Test
    fun `a pro user can change the watcher scope`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = true)
        val vm = createVM()

        vm.setWatcherScope(WatcherScope.ALL)

        watcherScope.value() shouldBe WatcherScope.ALL
    }

    @Test
    fun `a settled free user is routed to upgrade instead of changing the watcher scope`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)
        val vm = createVM()

        val navEvent = async { vm.navEvents.first() }
        vm.setWatcherScope(WatcherScope.ALL)

        watcherScope.value() shouldBe WatcherScope.NON_SYSTEM
        navEvent.await().shouldBeInstanceOf<NavEvent.GoTo>().destination shouldBe Nav.Main.Upgrade()
    }

    @Test
    fun `a paying user tapping during the unsettled cold start still gets the write`() = runTest(testDispatcher) {
        // isProForUi waits for the first settled Info instead of denying off the seed.
        upgradeInfo.value = info(isPro = false, isSettled = false)
        val vm = createVM()

        vm.setWatcherScope(WatcherScope.ALL)
        watcherScope.value() shouldBe WatcherScope.NON_SYSTEM

        upgradeInfo.value = info(isPro = true, isSettled = true)

        watcherScope.value() shouldBe WatcherScope.ALL
    }

    @Test
    fun `a settled free user is routed to upgrade instead of enabling notifications`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)
        isNotificationsEnabled.value(false)
        val vm = createVM()

        val navEvent = async { vm.navEvents.first() }
        vm.setNotificationsEnabled(true)

        isNotificationsEnabled.value() shouldBe false
        navEvent.await().shouldBeInstanceOf<NavEvent.GoTo>().destination shouldBe Nav.Main.Upgrade()
    }

    @Test
    fun `disabling notifications is a safe revocation that needs no entitlement`() = runTest(testDispatcher) {
        // The free path writes `false` when the locked row is tapped — that write must go through.
        upgradeInfo.value = info(isPro = false, isSettled = true)
        val vm = createVM()

        vm.setNotificationsEnabled(false)

        isNotificationsEnabled.value() shouldBe false
    }

    @Test
    fun `a settled free user is routed to upgrade instead of changing the gained-only filter`() =
        runTest(testDispatcher) {
            upgradeInfo.value = info(isPro = false, isSettled = true)
            val vm = createVM()

            val navEvent = async { vm.navEvents.first() }
            vm.setNotifyOnlyOnGained(false)

            isNotifyOnlyOnGained.value() shouldBe true
            navEvent.await().shouldBeInstanceOf<NavEvent.GoTo>().destination shouldBe Nav.Main.Upgrade()
        }

    @Test
    fun `non-pro settings are writable without an entitlement`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)
        val vm = createVM()

        vm.setPollingIntervalHours(12)
        vm.setBatteryHintDismissed(true)

        pollingIntervalHours.value() shouldBe 12
        isBatteryHintDismissed.value() shouldBe true
    }

    companion object {
        private fun info(
            isPro: Boolean,
            isSettled: Boolean = true,
            error: Throwable? = null,
        ): UpgradeRepo.Info = mockk<UpgradeRepo.Info>(relaxed = true).also {
            every { it.isPro } returns isPro
            every { it.isSettled } returns isSettled
            every { it.error } returns error
            every { it.type } returns UpgradeRepo.Type.GPLAY
        }
    }
}
