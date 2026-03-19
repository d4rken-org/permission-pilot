package eu.darken.myperm.watcher.ui.dashboard

import eu.darken.myperm.common.datastore.DataStoreValue
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.watcher.core.WatcherManager
import eu.darken.myperm.watcher.core.WatcherNotificationCapability
import eu.darken.myperm.watcher.core.WatcherNotifications
import eu.darken.myperm.watcher.core.WatcherWorkScheduler
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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

    private val isWatcherEnabled = MutableStateFlow(false)
    private val isNotificationsEnabled = MutableStateFlow(true)
    private val upgradeInfo = MutableStateFlow<UpgradeRepo.Info>(mockk { every { isPro } returns true })
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

        every { capability.areNotificationsEnabled() } returns true
        every { capability.isRuntimePermissionDenied() } returns false
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
        json = json,
    )

    @Test
    fun `card hidden when watcher disabled`() = runTest(testDispatcher) {
        isWatcherEnabled.value = false
        isNotificationsEnabled.value = true
        every { capability.areNotificationsEnabled() } returns false

        val vm = createVM()
        val state = vm.state.first { it != null && it != WatcherDashboardViewModel.State() }

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
}
