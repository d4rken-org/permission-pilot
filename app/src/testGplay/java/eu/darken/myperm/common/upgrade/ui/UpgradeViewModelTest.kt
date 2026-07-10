package eu.darken.myperm.common.upgrade.ui

import android.app.Activity
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.common.upgrade.core.MyPermSku
import eu.darken.myperm.common.upgrade.core.UpgradeRepoGplay
import eu.darken.myperm.common.upgrade.core.client.ItemAlreadyOwnedBillingException
import eu.darken.myperm.common.upgrade.core.client.UserCanceledBillingException
import eu.darken.myperm.common.upgrade.core.data.BillingData
import eu.darken.myperm.common.upgrade.core.data.SkuDetails
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider
import testhelper.coroutine.runTest2

class UpgradeViewModelTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()
    private val activity = mockk<Activity>()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun info(isPro: Boolean) = UpgradeRepoGplay.Info(
        gracePeriod = isPro,
        billingData = null,
    )

    private fun mockRepo(
        isPro: Boolean = false,
        wasEverPro: Boolean = false,
    ): UpgradeRepoGplay = mockk<UpgradeRepoGplay>(relaxed = true).apply {
        every { upgradeInfo } returns MutableStateFlow(info(isPro))
        every { this@apply.wasEverPro } returns MutableStateFlow(wasEverPro)
        coEvery { querySkus() } returns emptyList()
    }

    private fun buildVm(repo: UpgradeRepoGplay): UpgradeViewModel = UpgradeViewModel(
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        upgradeRepo = repo,
    )

    private fun iapSkuDetails(): SkuDetails = SkuDetails(
        sku = MyPermSku.Iap.PRO_UPGRADE,
        details = mockk<ProductDetails> {
            every { oneTimePurchaseOfferDetails } returns null
            every { subscriptionOfferDetails } returns null
        },
    )

    private fun result(code: Int): BillingResult = BillingResult.newBuilder().setResponseCode(code).build()

    @Test
    fun `restore with no purchase emits RestoreFailed`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.restorePurchaseNow() } returns info(isPro = false)
        val vm = buildVm(repo)

        vm.restorePurchase()
        advanceUntilIdle()

        vm.events.first() shouldBe UpgradeViewModel.UpgradeEvent.RestoreFailed
    }

    @Test
    fun `restore that finds pro emits no failure`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.restorePurchaseNow() } returns info(isPro = true)
        val vm = buildVm(repo)

        val events = mutableListOf<UpgradeViewModel.UpgradeEvent>()
        val collector = launch { vm.events.collect { events.add(it) } }
        vm.restorePurchase()
        advanceUntilIdle()

        events shouldBe emptyList()
        collector.cancel()
    }

    @Test
    fun `restore that times out emits RestoreFailed`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.restorePurchaseNow() } coAnswers {
            delay(30_000) // longer than the 15s restore timeout
            info(isPro = true)
        }
        val vm = buildVm(repo)

        vm.restorePurchase()
        advanceUntilIdle()

        vm.events.first() shouldBe UpgradeViewModel.UpgradeEvent.RestoreFailed
    }

    @Test
    fun `restore that errors forwards the error instead of RestoreFailed`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        val boom = IllegalStateException("Play unavailable")
        coEvery { repo.restorePurchaseNow() } throws boom
        val vm = buildVm(repo)

        vm.restorePurchase()
        advanceUntilIdle()

        vm.errorEvents.first() shouldBe boom
    }

    @Test
    fun `restore is single-flight, taps during a running restore are ignored`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.restorePurchaseNow() } coAnswers {
            delay(5_000)
            info(isPro = true)
        }
        val vm = buildVm(repo)

        vm.restorePurchase()
        vm.restorePurchase()
        vm.restorePurchase()
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.restorePurchaseNow() }
    }

    @Test
    fun `a finished restore allows a new attempt`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.restorePurchaseNow() } returns info(isPro = false)
        val vm = buildVm(repo)

        vm.restorePurchase()
        advanceUntilIdle()
        vm.restorePurchase()
        advanceUntilIdle()

        coVerify(exactly = 2) { repo.restorePurchaseNow() }
    }

    @Test
    fun `restore progress flows into the ui state and resets`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.restorePurchaseNow() } coAnswers {
            delay(5_000)
            info(isPro = false)
        }
        val vm = buildVm(repo)

        val inProgress = async { vm.state.first { it.restoreInProgress } }
        vm.restorePurchase()
        inProgress.await().restoreInProgress shouldBe true

        advanceUntilIdle()
        vm.state.first { !it.restoreInProgress }.restoreInProgress shouldBe false
    }

    @Test
    fun `previously-pro on this device flows into the banner flag`() = runTest2(context = testDispatcher) {
        val vm = buildVm(mockRepo(isPro = false, wasEverPro = true))

        val state = async { vm.state.first { it.pricing != null } }
        advanceUntilIdle()

        state.await().wasPreviouslyPro shouldBe true
    }

    @Test
    fun `banner flag stays off while grace still keeps the user pro`() = runTest2(context = testDispatcher) {
        val vm = buildVm(mockRepo(isPro = true, wasEverPro = true))

        val state = async { vm.state.first { it.pricing != null } }
        advanceUntilIdle()

        state.await().wasPreviouslyPro shouldBe false
    }

    @Test
    fun `user cancel during the billing flow stays silent`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.querySkus() } returns listOf(iapSkuDetails())
        coEvery { repo.launchBillingFlow(any(), any(), null) } throws
            UserCanceledBillingException(result(BillingResponseCode.USER_CANCELED))
        val vm = buildVm(repo)

        val errors = mutableListOf<Throwable>()
        val collector = launch { vm.errorEvents.collect { errors.add(it) } }

        val ready = async { vm.state.first { it.pricing?.iap != null } }
        advanceUntilIdle()
        ready.await()

        vm.launchBillingIap(activity)
        advanceUntilIdle()

        errors shouldBe emptyList()
        collector.cancel()
    }

    @Test
    fun `already-owned buy attempt silently restores the purchase instead of erroring`() =
        runTest2(context = testDispatcher) {
            val repo = mockRepo()
            coEvery { repo.querySkus() } returns listOf(iapSkuDetails())
            coEvery { repo.launchBillingFlow(any(), any(), null) } throws
                ItemAlreadyOwnedBillingException(result(BillingResponseCode.ITEM_ALREADY_OWNED))
            coEvery { repo.restorePurchaseNow() } returns info(isPro = true)
            val vm = buildVm(repo)

            val errors = mutableListOf<Throwable>()
            val collector = launch { vm.errorEvents.collect { errors.add(it) } }

            val ready = async { vm.state.first { it.pricing?.iap != null } }
            advanceUntilIdle()
            ready.await()

            vm.launchBillingIap(activity)
            advanceUntilIdle()

            errors shouldBe emptyList()
            coVerify(exactly = 1) { repo.restorePurchaseNow() }
            collector.cancel()
        }

    @Test
    fun `already-owned buy attempt falls back to the error dialog when restore finds nothing`() =
        runTest2(context = testDispatcher) {
            val repo = mockRepo()
            coEvery { repo.querySkus() } returns listOf(iapSkuDetails())
            coEvery { repo.launchBillingFlow(any(), any(), null) } throws
                ItemAlreadyOwnedBillingException(result(BillingResponseCode.ITEM_ALREADY_OWNED))
            coEvery { repo.restorePurchaseNow() } returns info(isPro = false)
            val vm = buildVm(repo)

            val ready = async { vm.state.first { it.pricing?.iap != null } }
            advanceUntilIdle()
            ready.await()

            vm.launchBillingIap(activity)
            advanceUntilIdle()

            vm.errorEvents.first().shouldBeInstanceOf<ItemAlreadyOwnedBillingException>()
        }

    @Test
    fun `already-owned buy attempt falls back to the error dialog when restore itself errors`() =
        runTest2(context = testDispatcher) {
            val repo = mockRepo()
            coEvery { repo.querySkus() } returns listOf(iapSkuDetails())
            coEvery { repo.launchBillingFlow(any(), any(), null) } throws
                ItemAlreadyOwnedBillingException(result(BillingResponseCode.ITEM_ALREADY_OWNED))
            coEvery { repo.restorePurchaseNow() } throws RuntimeException("Play unavailable")
            val vm = buildVm(repo)

            val ready = async { vm.state.first { it.pricing?.iap != null } }
            advanceUntilIdle()
            ready.await()

            vm.launchBillingIap(activity)
            advanceUntilIdle()

            vm.errorEvents.first().shouldBeInstanceOf<ItemAlreadyOwnedBillingException>()
        }

    @Test
    fun `consecutive unequal pro emissions trigger a single navUp`() = runTest2(context = testDispatcher) {
        val infoFlow = MutableStateFlow<UpgradeRepo.Info>(info(isPro = false))
        val repo = mockRepo()
        every { repo.upgradeInfo } returns infoFlow
        val vm = buildVm(repo)

        val navEvents = mutableListOf<Any>()
        val collector = launch { vm.navEvents.collect { navEvents.add(it) } }
        advanceUntilIdle()

        // Two Pro Infos with different backing data — unequal objects, both isPro.
        infoFlow.value = UpgradeRepoGplay.Info(gracePeriod = true, billingData = null)
        advanceUntilIdle()
        infoFlow.value = UpgradeRepoGplay.Info(gracePeriod = true, billingData = BillingData(emptySet()))
        advanceUntilIdle()

        navEvents.size shouldBe 1
        collector.cancel()
    }

    @Test
    fun `other launch failures are forwarded to the error dialog`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        val boom = IllegalStateException("launch broke")
        coEvery { repo.querySkus() } returns listOf(iapSkuDetails())
        coEvery { repo.launchBillingFlow(any(), any(), null) } throws boom
        val vm = buildVm(repo)

        val ready = async { vm.state.first { it.pricing?.iap != null } }
        advanceUntilIdle()
        ready.await()

        vm.launchBillingIap(activity)
        advanceUntilIdle()

        vm.errorEvents.first() shouldBe boom
    }
}
