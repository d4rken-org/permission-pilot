package eu.darken.myperm.main.ui

import androidx.lifecycle.SavedStateHandle
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.theming.ThemeColor
import eu.darken.myperm.common.theming.ThemeMode
import eu.darken.myperm.common.theming.ThemeStyle
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider
import testhelpers.datastore.mockDataStoreValue

class MainActivityVMTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()

    private val generalSettings: GeneralSettings = mockk()
    private val changeDao: PermissionChangeDao = mockk()
    private val upgradeRepo: UpgradeRepo = mockk(relaxed = true)

    // Hot, never-completing: the readiness gate must react to the FIRST emission, so a finite flow
    // would settle it for the wrong reason (completion instead of an entitlement answer).
    private val upgradeInfo = MutableSharedFlow<UpgradeRepo.Info>(replay = 0)

    private fun info(isPro: Boolean = false): UpgradeRepo.Info = mockk<UpgradeRepo.Info>(relaxed = true).also {
        every { it.isPro } returns isPro
        every { it.isSettled } returns true
        every { it.error } returns null
        every { it.type } returns UpgradeRepo.Type.GPLAY
    }

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { generalSettings.themeMode } returns mockDataStoreValue(ThemeMode.SYSTEM)
        every { generalSettings.themeStyle } returns mockDataStoreValue(ThemeStyle.DEFAULT)
        every { generalSettings.themeColor } returns mockDataStoreValue(ThemeColor.BLUE)
        every { generalSettings.isWatcherEnabled } returns mockDataStoreValue(false)
        every { changeDao.getUnseenCount() } returns flowOf(0)
        every { upgradeRepo.upgradeInfo } returns upgradeInfo
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createVM() = MainActivityVM(
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        handle = SavedStateHandle(),
        upgradeRepo = upgradeRepo,
        generalSettings = generalSettings,
        changeDao = changeDao,
    )

    @Test
    fun `the first entitlement emission releases the splash screen`() = runTest(testDispatcher) {
        val vm = createVM()
        runCurrent()

        upgradeInfo.emit(info())
        runCurrent()

        vm.readyState.value shouldBe true
    }

    @Test
    fun `a delayed first emission still releases the splash screen without waiting out the fallback`() =
        runTest(testDispatcher) {
            val vm = createVM()
            runCurrent()
            vm.readyState.value shouldBe false

            advanceTimeBy(500)
            upgradeInfo.emit(info())
            runCurrent()

            vm.readyState.value shouldBe true
            currentTime shouldBe 500
        }

    @Test
    fun `a never-emitting entitlement flow releases the splash screen via the fallback`() =
        runTest(testDispatcher) {
            val vm = createVM()

            advanceTimeBy(MainActivityVM.READY_FALLBACK_MS - 1)
            runCurrent()
            vm.readyState.value shouldBe false

            advanceUntilIdle()

            vm.readyState.value shouldBe true
        }
}
