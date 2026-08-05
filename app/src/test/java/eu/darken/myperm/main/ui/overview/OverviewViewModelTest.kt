package eu.darken.myperm.main.ui.overview

import androidx.lifecycle.SavedStateHandle
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.common.review.ReviewTool
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider
import testhelpers.TestApplication
import java.util.concurrent.atomic.AtomicInteger

/**
 * The dashboard title brands itself for supporters, so the entitlement arm of the state must not
 * fall back to "unknown" when the state flow restarts inside the same process — that would flicker
 * a paying supporter's title back to the plain app name.
 *
 * Robolectric, like PP's other framework-touching tests: constructing the ViewModel initializes
 * [eu.darken.myperm.apps.core.known.AKnownPkg] for its store-package sets, and every `Pkg.Id` in
 * there defaults its userHandle to `Process.myUserHandle()` — null on a bare JVM, which fails the
 * class initializer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class OverviewViewModelTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()

    private val proInfo: UpgradeRepo.Info = mockk<UpgradeRepo.Info>(relaxed = true).also {
        every { it.isPro } returns true
        every { it.isSettled } returns true
        every { it.error } returns null
        every { it.type } returns UpgradeRepo.Type.GPLAY
    }

    // Emits the entitlement to the FIRST subscriber only, then stays open: a re-subscription that
    // re-emits by itself would make the retention untestable.
    private val upgradeSubscriptions = AtomicInteger(0)
    private val upgradeInfoFlow = flow<UpgradeRepo.Info> {
        if (upgradeSubscriptions.getAndIncrement() == 0) emit(proInfo)
        awaitCancellation()
    }

    private val appRepo: AppRepo = mockk(relaxed = true)
    private val upgradeRepo: UpgradeRepo = mockk(relaxed = true)
    private val generalSettings: GeneralSettings = mockk(relaxed = true)

    // Never relaxed: a relaxed `state` would hand the combine an empty flow, and the whole render
    // state would silently stop being computed.
    private val reviewTool: ReviewTool = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { appRepo.appData } returns MutableStateFlow(AppRepo.AppDataState.Ready(emptyList()))
        every { appRepo.isScanning } returns MutableStateFlow(false)
        every { appRepo.scanError } returns MutableStateFlow<Throwable?>(null)
        every { upgradeRepo.upgradeInfo } returns upgradeInfoFlow
        every { reviewTool.state } returns flowOf(ReviewTool.State())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createVM() = OverviewViewModel(
        handle = SavedStateHandle(),
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        appRepo = appRepo,
        upgradeRepo = upgradeRepo,
        generalSettings = generalSettings,
        reviewTool = reviewTool,
    )

    @Test
    fun `a restarted state flow seeds with the last known entitlement`() = runTest(testDispatcher) {
        val vm = createVM()

        val firstCollector = launch { vm.state.collect { } }
        advanceUntilIdle()
        vm.state.value?.upgradeInfo?.isPro shouldBe true
        firstCollector.cancel()

        // Past the sharing timeout: the shared state flow tears its upstream down.
        advanceTimeBy(10_000)
        advanceUntilIdle()

        val secondCollector = launch { vm.state.collect { } }
        advanceUntilIdle()

        // Non-vacuity: the upstream really was re-subscribed, and it stayed silent this time — so
        // only the retained seed can put the entitlement back into the recomputed state.
        upgradeSubscriptions.get() shouldBe 2
        vm.state.value?.upgradeInfo?.isPro shouldBe true

        secondCollector.cancel()
    }

    @Test
    fun `a refresh failure suppresses the review card`() = runTest(testDispatcher) {
        every { reviewTool.state } returns flowOf(ReviewTool.State(shouldAskForReview = true))

        // Non-vacuity: without an error the same eligibility does show the card.
        createVM().let { vm ->
            val subscription = launch { vm.state.collect { } }
            advanceUntilIdle()
            vm.state.value?.showReviewCard shouldBe true
            subscription.cancel()
        }

        // Data plus a failed refresh: the screen carries an inline error banner, which is the wrong
        // moment to ask the user for a five star rating.
        every { appRepo.scanError } returns MutableStateFlow<Throwable?>(IllegalStateException("scan failed"))

        createVM().let { vm ->
            val subscription = launch { vm.state.collect { } }
            advanceUntilIdle()
            vm.state.value!!.apply {
                refreshError shouldNotBe null
                showReviewCard shouldBe false
            }
            subscription.cancel()
        }
    }
}
