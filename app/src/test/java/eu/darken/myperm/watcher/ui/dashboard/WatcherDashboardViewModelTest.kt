package eu.darken.myperm.watcher.ui.dashboard

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.datastore.DataStoreValue
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavEvent
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.watcher.core.WatcherBatteryCapability
import eu.darken.myperm.watcher.core.WatcherEventType
import eu.darken.myperm.watcher.core.WatcherManager
import eu.darken.myperm.watcher.core.WatcherNotificationCapability
import eu.darken.myperm.watcher.core.WatcherNotifications
import eu.darken.myperm.watcher.core.WatcherWorkScheduler
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider

class WatcherDashboardViewModelTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()

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

    private val isWatcherEnabled = MutableStateFlow(false)
    private val isNotificationsEnabled = MutableStateFlow(true)
    private val isBatteryHintDismissed = MutableStateFlow(false)
    /**
     * Hot flow, never a finite `flowOf`: `isProForUi` waits for a settled emission, and a finished
     * flow would push the tap gate onto its fail-open timeout path instead of the real decision.
     */
    private val upgradeInfo = MutableStateFlow(info(isPro = true))
    private val watcherFilterOptions = MutableStateFlow(WatcherFilterOptions())

    private val generalSettings: GeneralSettings = mockk(relaxed = true)
    private val changeDao: PermissionChangeDao = mockk(relaxed = true)
    private val upgradeRepo: UpgradeRepo = mockk(relaxed = true)
    private val capability: WatcherNotificationCapability = mockk()
    private val watcherWorkScheduler: WatcherWorkScheduler = mockk(relaxed = true)
    private val watcherManager: WatcherManager = mockk(relaxed = true) {
        every { phase } returns MutableStateFlow(null)
    }
    private val watcherNotifications: WatcherNotifications = mockk(relaxed = true)
    private val batteryCapability: WatcherBatteryCapability = mockk()
    private val json: Json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { generalSettings.isWatcherEnabled } returns mockk<DataStoreValue<Boolean>> {
            every { flow } returns isWatcherEnabled
        }
        every { generalSettings.isWatcherNotificationsEnabled } returns mockk<DataStoreValue<Boolean>> {
            every { flow } returns isNotificationsEnabled
        }
        every { generalSettings.watcherFilterOptions } returns mockk<DataStoreValue<WatcherFilterOptions>> {
            every { flow } returns watcherFilterOptions
        }
        every { upgradeRepo.upgradeInfo } returns upgradeInfo
        every { changeDao.getAll() } returns flowOf(emptyList())

        every { generalSettings.isWatcherBatteryHintDismissed } returns mockk<DataStoreValue<Boolean>> {
            every { flow } returns isBatteryHintDismissed
        }
        every { generalSettings.watcherLastSuccessfulPollAt } returns mockk<DataStoreValue<Long>> {
            every { valueBlocking } returns 0L
        }
        every { generalSettings.watcherPollingIntervalHours } returns mockk<DataStoreValue<Int>> {
            every { valueBlocking } returns 4
        }

        every { capability.areNotificationsEnabled() } returns true
        every { capability.isRuntimePermissionDenied() } returns false
        every { batteryCapability.isBatteryOptimizationIgnored() } returns true
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createVM() = WatcherDashboardViewModel(
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        generalSettings = generalSettings,
        changeDao = changeDao,
        upgradeRepo = upgradeRepo,
        capability = capability,
        watcherWorkScheduler = watcherWorkScheduler,
        watcherManager = watcherManager,
        watcherNotifications = watcherNotifications,
        batteryCapability = batteryCapability,
        json = json,
    )

    @Test
    fun `card hidden when watcher disabled`() = runTest(testDispatcher) {
        isWatcherEnabled.value = false
        isNotificationsEnabled.value = true
        every { capability.areNotificationsEnabled() } returns false
        // A report makes the computed state distinguishable from the default State() we start with.
        every { changeDao.getAll() } returns flowOf(reports(1))

        val vm = createVM()
        val state = vm.state.first { it != null && it.totalReportCount == 1 }

        state!!.showNotificationPermissionCard shouldBe false
    }

    @Test
    fun `card hidden when in-app notifications disabled`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        isNotificationsEnabled.value = false
        every { capability.areNotificationsEnabled() } returns false

        val vm = createVM()
        val state = vm.state.first { it != null && it.isWatcherEnabled }

        state!!.showNotificationPermissionCard shouldBe false
    }

    @Test
    fun `card hidden when notifications available`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        isNotificationsEnabled.value = true
        every { capability.areNotificationsEnabled() } returns true

        val vm = createVM()
        val state = vm.state.first { it != null && it.isWatcherEnabled }

        state!!.showNotificationPermissionCard shouldBe false
    }

    @Test
    fun `card shown when watcher enabled and notifications enabled but unavailable`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        isNotificationsEnabled.value = true
        every { capability.areNotificationsEnabled() } returns false

        val vm = createVM()
        val state = vm.state.first { it != null && it.isWatcherEnabled }

        state!!.showNotificationPermissionCard shouldBe true
    }

    @Test
    fun `canRequestNotificationPermission reflects capability`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        isNotificationsEnabled.value = true
        every { capability.areNotificationsEnabled() } returns false
        every { capability.isRuntimePermissionDenied() } returns true

        val vm = createVM()
        val state = vm.state.first { it != null && it.isWatcherEnabled }

        state!!.canRequestNotificationPermission shouldBe true
    }

    @Test
    fun `canRequestNotificationPermission false when permission already granted`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        isNotificationsEnabled.value = true
        every { capability.areNotificationsEnabled() } returns false
        every { capability.isRuntimePermissionDenied() } returns false

        val vm = createVM()
        val state = vm.state.first { it != null && it.isWatcherEnabled }

        state!!.canRequestNotificationPermission shouldBe false
    }

    @Test
    fun `refreshNotificationState updates state`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        isNotificationsEnabled.value = true
        every { capability.areNotificationsEnabled() } returns false

        val vm = createVM()
        val state1 = vm.state.first { it != null && it.isWatcherEnabled }
        state1!!.showNotificationPermissionCard shouldBe true

        every { capability.areNotificationsEnabled() } returns true
        vm.refreshNotificationState()

        val state2 = vm.state.first { it != null && it.isWatcherEnabled && !it.showNotificationPermissionCard }
        state2!!.showNotificationPermissionCard shouldBe false
    }

    // --- upgrade lock ---------------------------------------------------------------------------

    private fun reports(count: Int) = (1..count).map { index ->
        PermissionChangeEntity(
            id = index.toLong(),
            packageName = Pkg.Name("com.example.app$index"),
            userHandleId = 0,
            appLabel = "App $index",
            versionCode = 1L,
            versionName = "1.0",
            eventType = WatcherEventType.UPDATE,
            changesJson = "{}",
            detectedAt = index.toLong(),
        )
    }

    @Test
    fun `a settled free user sees a truncated report list`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)
        every { changeDao.getAll() } returns flowOf(reports(7))

        val vm = createVM()
        val state = vm.state.first { it != null && it.totalReportCount == 7 }

        state!!.isUpgradeLocked shouldBe true
        state.reports.size shouldBe 5
        state.lockedReportCount shouldBe 2
    }

    @Test
    fun `the unsettled cold start is not presented as hard-locked`() = runTest(testDispatcher) {
        // The GPlay seed reports non-Pro even for paying users — truncating here would hide a
        // paying user's reports while billing connects.
        upgradeInfo.value = info(isPro = false, isSettled = false)
        every { changeDao.getAll() } returns flowOf(reports(7))

        val vm = createVM()
        val state = vm.state.first { it != null && it.totalReportCount == 7 }

        state!!.isUpgradeLocked shouldBe false
        state.reports.size shouldBe 7
        state.lockedReportCount shouldBe 0
    }

    @Test
    fun `a settled error state is not presented as hard-locked`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true, error = IllegalStateException("nope"))
        every { changeDao.getAll() } returns flowOf(reports(7))

        val vm = createVM()
        val state = vm.state.first { it != null && it.totalReportCount == 7 }

        state!!.isUpgradeLocked shouldBe false
        state.reports.size shouldBe 7
    }

    @Test
    fun `a pro user is never hard-locked`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = true, isSettled = true)
        every { changeDao.getAll() } returns flowOf(reports(7))

        val vm = createVM()
        val state = vm.state.first { it != null && it.totalReportCount == 7 }

        state!!.isUpgradeLocked shouldBe false
        state.reports.size shouldBe 7
    }

    // --- notification permission gate -----------------------------------------------------------

    @Test
    fun `a settled free user tapping grant is routed to upgrade instead of the OS prompt`() =
        runTest(testDispatcher) {
            upgradeInfo.value = info(isPro = false, isSettled = true)
            val vm = createVM()
            val events = mutableListOf<WatcherDashboardViewModel.Event>()
            backgroundScope.launch { vm.events.toList(events) }
            runCurrent()

            vm.requestNotificationPermission()
            advanceUntilIdle()

            events.shouldBeEmpty()
            vm.navEvents.first().shouldBeInstanceOf<NavEvent.GoTo>().destination shouldBe Nav.Main.Upgrade()
        }

    @Test
    fun `a pro user tapping grant gets the permission request event`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = true, isSettled = true)
        val vm = createVM()
        val events = mutableListOf<WatcherDashboardViewModel.Event>()
        backgroundScope.launch { vm.events.toList(events) }
        runCurrent()

        vm.requestNotificationPermission()
        advanceUntilIdle()

        events shouldBe listOf(WatcherDashboardViewModel.Event.RequestNotificationPermission)
    }

    @Test
    fun `a paying user tapping during the unsettled cold start gets the prompt once the gate resolves`() =
        runTest(testDispatcher) {
            // No virtual time is advanced here: isProForUi must resolve off the settle signal, not
            // off its fail-open timeout.
            upgradeInfo.value = info(isPro = false, isSettled = false)
            val vm = createVM()
            val events = mutableListOf<WatcherDashboardViewModel.Event>()
            backgroundScope.launch { vm.events.toList(events) }
            runCurrent()

            vm.requestNotificationPermission()
            runCurrent()
            events.shouldBeEmpty()

            upgradeInfo.value = info(isPro = true, isSettled = true)
            runCurrent()

            events shouldBe listOf(WatcherDashboardViewModel.Event.RequestNotificationPermission)
        }

    @Test
    fun `battery card hidden when watcher disabled`() = runTest(testDispatcher) {
        isWatcherEnabled.value = false
        every { batteryCapability.isBatteryOptimizationIgnored() } returns false
        // A report makes the computed state distinguishable from the default State() we start with.
        every { changeDao.getAll() } returns flowOf(reports(1))

        val vm = createVM()
        val state = vm.state.first { it != null && it.totalReportCount == 1 }

        state!!.showBatteryOptimizationCard shouldBe false
    }

    @Test
    fun `battery card hidden when battery optimization ignored`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        every { batteryCapability.isBatteryOptimizationIgnored() } returns true

        val vm = createVM()
        vm.refreshBatteryState()
        val state = vm.state.first { it != null && it.isWatcherEnabled }

        state!!.showBatteryOptimizationCard shouldBe false
    }

    @Test
    fun `battery card hidden when no poll has ever run`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        every { batteryCapability.isBatteryOptimizationIgnored() } returns false
        every { generalSettings.watcherLastSuccessfulPollAt.valueBlocking } returns 0L

        val vm = createVM()
        val state = vm.state.first { it != null && it.isWatcherEnabled }

        state!!.showBatteryOptimizationCard shouldBe false
    }

    @Test
    fun `battery card shown when optimized and stale`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        every { batteryCapability.isBatteryOptimizationIgnored() } returns false
        every { generalSettings.watcherLastSuccessfulPollAt.valueBlocking } returns 1L

        val vm = createVM()
        val state = vm.state.first { it != null && it.isWatcherEnabled && it.showBatteryOptimizationCard }

        state!!.showBatteryOptimizationCard shouldBe true
    }

    @Test
    fun `battery card hidden when dismissed`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        isBatteryHintDismissed.value = true
        every { batteryCapability.isBatteryOptimizationIgnored() } returns false
        every { generalSettings.watcherLastSuccessfulPollAt.valueBlocking } returns 1L

        val vm = createVM()
        val state = vm.state.first { it != null && it.isWatcherEnabled }

        state!!.showBatteryOptimizationCard shouldBe false
    }

    @Test
    fun `battery card reappears when dismiss is reset`() = runTest(testDispatcher) {
        isWatcherEnabled.value = true
        isBatteryHintDismissed.value = true
        every { batteryCapability.isBatteryOptimizationIgnored() } returns false
        every { generalSettings.watcherLastSuccessfulPollAt.valueBlocking } returns 1L

        val vm = createVM()
        val state1 = vm.state.first { it != null && it.isWatcherEnabled }
        state1!!.showBatteryOptimizationCard shouldBe false

        every { generalSettings.watcherLastSuccessfulPollAt.valueBlocking } returns 1L
        isBatteryHintDismissed.value = false
        vm.refreshBatteryState()
        val state2 = vm.state.first { it != null && it.isWatcherEnabled && it.showBatteryOptimizationCard }
        state2!!.showBatteryOptimizationCard shouldBe true
    }
}
