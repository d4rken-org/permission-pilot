package eu.darken.myperm.settings.ui.general

import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavEvent
import eu.darken.myperm.common.theming.ThemeColor
import eu.darken.myperm.common.theming.ThemeMode
import eu.darken.myperm.common.theming.ThemeStyle
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

class GeneralSettingsViewModelTest : BaseTest() {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val themeMode = mockDataStoreValue(ThemeMode.SYSTEM)
    private val themeStyle = mockDataStoreValue(ThemeStyle.DEFAULT)
    private val themeColor = mockDataStoreValue(ThemeColor.BLUE)
    private val ipcParallelisation = mockDataStoreValue(0)

    private val generalSettings: GeneralSettings = mockk()
    private val upgradeRepo: UpgradeRepo = mockk(relaxed = true)

    /**
     * Hot flow, never a finite `flowOf`: `isProForUi` waits for a settled emission, and a finished
     * flow would push every gate onto its fail-open timeout path instead of the real decision.
     */
    private val upgradeInfo = MutableStateFlow(info(isPro = false))

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { generalSettings.themeMode } returns themeMode
        every { generalSettings.themeStyle } returns themeStyle
        every { generalSettings.themeColor } returns themeColor
        every { generalSettings.ipcParallelisation } returns ipcParallelisation
        every { upgradeRepo.upgradeInfo } returns upgradeInfo
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createVM() = GeneralSettingsViewModel(
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        generalSettings = generalSettings,
        upgradeRepo = upgradeRepo,
    )

    // --- presentation ---------------------------------------------------------------------------

    @Test
    fun `a settled free user sees the hard-locked upgrade presentation`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)

        createVM().isUpgradeLocked.first() shouldBe true
    }

    @Test
    fun `the unsettled cold start is not presented as hard-locked`() = runTest(testDispatcher) {
        // The GPlay seed reports non-Pro even for paying users — rendering the upgrade branch here
        // would route them to upgrade without a setter ever running.
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
    fun `a pro user can change the theme mode`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = true)
        val vm = createVM()

        vm.setThemeMode(ThemeMode.DARK)

        themeMode.value() shouldBe ThemeMode.DARK
    }

    @Test
    fun `a settled free user is routed to upgrade instead of changing the theme mode`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)
        val vm = createVM()

        val navEvent = async { vm.navEvents.first() }
        vm.setThemeMode(ThemeMode.DARK)

        themeMode.value() shouldBe ThemeMode.SYSTEM
        navEvent.await().shouldBeInstanceOf<NavEvent.GoTo>().destination shouldBe Nav.Main.Upgrade()
    }

    @Test
    fun `a paying user tapping during the unsettled cold start still gets the write`() = runTest(testDispatcher) {
        // isProForUi waits for the first settled Info instead of denying off the seed.
        upgradeInfo.value = info(isPro = false, isSettled = false)
        val vm = createVM()

        vm.setThemeStyle(ThemeStyle.MATERIAL_YOU)
        themeStyle.value() shouldBe ThemeStyle.DEFAULT

        upgradeInfo.value = info(isPro = true, isSettled = true)

        themeStyle.value() shouldBe ThemeStyle.MATERIAL_YOU
    }

    @Test
    fun `a settled free user is routed to upgrade instead of changing the theme color`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)
        val vm = createVM()

        val navEvent = async { vm.navEvents.first() }
        vm.setThemeColor(ThemeColor.AMBER)

        themeColor.value() shouldBe ThemeColor.BLUE
        navEvent.await().shouldBeInstanceOf<NavEvent.GoTo>().destination shouldBe Nav.Main.Upgrade()
    }

    @Test
    fun `a settled free user is routed to upgrade instead of changing the scan speed`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)
        val vm = createVM()

        val navEvent = async { vm.navEvents.first() }
        vm.setIpcParallelisation(4)

        ipcParallelisation.value() shouldBe 0
        navEvent.await().shouldBeInstanceOf<NavEvent.GoTo>().destination shouldBe Nav.Main.Upgrade()
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
