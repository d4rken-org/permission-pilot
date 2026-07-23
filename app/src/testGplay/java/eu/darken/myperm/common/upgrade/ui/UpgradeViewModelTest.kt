package eu.darken.myperm.common.upgrade.ui

import android.app.Activity
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.navigation.NavEvent
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.common.upgrade.core.MyPermSku
import eu.darken.myperm.common.upgrade.core.UpgradeRepoGplay
import eu.darken.myperm.common.upgrade.core.client.ItemAlreadyOwnedBillingException
import eu.darken.myperm.common.upgrade.core.client.UserCanceledBillingException
import eu.darken.myperm.common.upgrade.core.data.BillingData
import eu.darken.myperm.common.upgrade.core.data.SkuDetails
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
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
    private val webpageTool = mockk<WebpageTool>(relaxed = true)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun purchase(productId: String, autoRenew: Boolean = false) = mockk<Purchase> {
        every { products } returns listOf(productId)
        every { purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchaseTime } returns 1_000L
        every { isAutoRenewing } returns autoRenew
        every { isAcknowledged } returns true
    }

    private fun notProInfo() = UpgradeRepoGplay.Info(billingData = null)
    private fun graceInfo() = UpgradeRepoGplay.Info(gracePeriod = true, billingData = null)
    private fun iapOwnedInfo() = UpgradeRepoGplay.Info(
        billingData = BillingData(setOf(purchase(MyPermSku.Iap.PRO_UPGRADE.id))),
    )

    private fun iapSkuDetails(): SkuDetails = SkuDetails(
        sku = MyPermSku.Iap.PRO_UPGRADE,
        details = mockk<ProductDetails> {
            every { oneTimePurchaseOfferDetails } returns null
            every { subscriptionOfferDetails } returns null
        },
    )

    private fun result(code: Int): BillingResult = BillingResult.newBuilder().setResponseCode(code).build()

    private fun mockRepo(
        info: UpgradeRepoGplay.Info = notProInfo(),
        wasEverPro: Boolean = false,
        skus: List<SkuDetails> = emptyList(),
    ): UpgradeRepoGplay = mockk<UpgradeRepoGplay>(relaxed = true).apply {
        every { upgradeInfo } returns MutableStateFlow<UpgradeRepo.Info>(info)
        every { this@apply.wasEverPro } returns MutableStateFlow(wasEverPro)
        every { lastProConfirmedAt } returns MutableStateFlow(0L)
        coEvery { querySkus() } returns skus
        coEvery { refresh() } returns Unit
        coEvery { queryCurrentSubscriptions() } returns emptyList()
    }

    private fun buildVm(repo: UpgradeRepoGplay): UpgradeViewModel = UpgradeViewModel(
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        upgradeRepo = repo,
        webpageTool = webpageTool,
    )

    private suspend fun UpgradeViewModel.loaded(): UpgradeUiState.Loaded =
        state.first { it is UpgradeUiState.Loaded } as UpgradeUiState.Loaded

    // --- restore -------------------------------------------------------------------------------

    @Test
    fun `restore with no purchase emits RestoreFailed`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.restorePurchaseNow() } returns notProInfo()
        val vm = buildVm(repo).also { it.init(manage = false) }

        vm.onRestore()
        advanceUntilIdle()

        vm.events.first() shouldBe UpgradeViewModel.UpgradeEvent.RestoreFailed
    }

    @Test
    fun `restore that returns an actual purchase emits RestoreSucceeded`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.restorePurchaseNow() } returns iapOwnedInfo()
        val vm = buildVm(repo).also { it.init(manage = true) }

        vm.onRestore()
        advanceUntilIdle()

        vm.events.first() shouldBe UpgradeViewModel.UpgradeEvent.RestoreSucceeded
    }

    @Test
    fun `grace-only pro is not a successful restore`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.restorePurchaseNow() } returns graceInfo()
        val vm = buildVm(repo).also { it.init(manage = true) }

        vm.onRestore()
        advanceUntilIdle()

        vm.events.first() shouldBe UpgradeViewModel.UpgradeEvent.RestoreFailed
    }

    @Test
    fun `restore that errors forwards the error`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        val boom = IllegalStateException("Play unavailable")
        coEvery { repo.restorePurchaseNow() } throws boom
        val vm = buildVm(repo).also { it.init(manage = false) }

        vm.onRestore()
        advanceUntilIdle()

        vm.errorEvents.first() shouldBe boom
    }

    @Test
    fun `restore is single-flight`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        coEvery { repo.restorePurchaseNow() } coAnswers {
            delay(5_000)
            notProInfo()
        }
        val vm = buildVm(repo).also { it.init(manage = false) }

        vm.onRestore()
        vm.onRestore()
        vm.onRestore()
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.restorePurchaseNow() }
    }

    // --- switch gate ---------------------------------------------------------------------------

    @Test
    fun `switch blocked while a subscription is still renewing`() = runTest2(context = testDispatcher) {
        val repo = mockRepo(skus = listOf(iapSkuDetails()))
        coEvery { repo.queryCurrentSubscriptions() } returns listOf(purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = true))
        val vm = buildVm(repo).also { it.init(manage = true) }
        vm.loaded()
        advanceUntilIdle()

        vm.onSwitchToIap(activity)
        advanceUntilIdle()

        vm.events.first() shouldBe UpgradeViewModel.UpgradeEvent.SubscriptionStillRenewing
        coVerify(exactly = 0) { repo.launchBillingFlow(any(), any(), any()) }
    }

    @Test
    fun `switch fails closed when the verify times out`() = runTest2(context = testDispatcher) {
        val repo = mockRepo(skus = listOf(iapSkuDetails()))
        coEvery { repo.queryCurrentSubscriptions() } coAnswers {
            delay(20_000) // longer than VERIFY_TIMEOUT_MS
            emptyList()
        }
        val vm = buildVm(repo).also { it.init(manage = true) }
        vm.loaded()
        advanceUntilIdle()

        vm.onSwitchToIap(activity)
        advanceUntilIdle()

        vm.events.first() shouldBe UpgradeViewModel.UpgradeEvent.SubscriptionCheckFailed
        coVerify(exactly = 0) { repo.launchBillingFlow(any(), any(), any()) }
    }

    @Test
    fun `switch launches the one-time purchase when no subscription is renewing`() = runTest2(context = testDispatcher) {
        val repo = mockRepo(skus = listOf(iapSkuDetails()))
        coEvery { repo.queryCurrentSubscriptions() } returns listOf(purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = false))
        val vm = buildVm(repo).also { it.init(manage = true) }
        vm.loaded()
        advanceUntilIdle()

        vm.onSwitchToIap(activity)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.launchBillingFlow(activity, match { it.sku == MyPermSku.Iap.PRO_UPGRADE }, null) }
    }

    // --- manage vs sales nav -------------------------------------------------------------------

    @Test
    fun `sales mode closes the screen once the user becomes pro`() = runTest2(context = testDispatcher) {
        val infoFlow = MutableStateFlow<UpgradeRepo.Info>(notProInfo())
        val repo = mockRepo().apply { every { upgradeInfo } returns infoFlow }
        val vm = buildVm(repo).also { it.init(manage = false) }

        val navEvents = mutableListOf<NavEvent>()
        val collector = launch { vm.navEvents.collect { navEvents.add(it) } }
        advanceUntilIdle()

        infoFlow.value = iapOwnedInfo()
        advanceUntilIdle()

        navEvents.any { it is NavEvent.Up } shouldBe true
        collector.cancel()
    }

    @Test
    fun `manage mode stays open when the user is pro`() = runTest2(context = testDispatcher) {
        val infoFlow = MutableStateFlow<UpgradeRepo.Info>(iapOwnedInfo())
        val repo = mockRepo().apply { every { upgradeInfo } returns infoFlow }
        val vm = buildVm(repo).also { it.init(manage = true) }

        val navEvents = mutableListOf<NavEvent>()
        val collector = launch { vm.navEvents.collect { navEvents.add(it) } }
        advanceUntilIdle()

        navEvents.none { it is NavEvent.Up } shouldBe true
        collector.cancel()
    }

    // --- direct buy ----------------------------------------------------------------------------

    @Test
    fun `user cancel during the billing flow stays silent`() = runTest2(context = testDispatcher) {
        val repo = mockRepo(skus = listOf(iapSkuDetails()))
        coEvery { repo.launchBillingFlow(any(), any(), null) } throws
            UserCanceledBillingException(result(BillingResponseCode.USER_CANCELED))
        val vm = buildVm(repo).also { it.init(manage = false) }
        vm.state.first { it is UpgradeUiState.Loaded && it.pricing.iap != null }

        val errors = mutableListOf<Throwable>()
        val collector = launch { vm.errorEvents.collect { errors.add(it) } }
        vm.onBuyIap(activity)
        advanceUntilIdle()

        errors shouldBe emptyList()
        collector.cancel()
    }

    @Test
    fun `already-owned buy attempt restores when the exact sku comes back`() = runTest2(context = testDispatcher) {
        val repo = mockRepo(skus = listOf(iapSkuDetails()))
        coEvery { repo.launchBillingFlow(any(), any(), null) } throws
            ItemAlreadyOwnedBillingException(result(BillingResponseCode.ITEM_ALREADY_OWNED))
        coEvery { repo.restorePurchaseNow() } returns iapOwnedInfo()
        val vm = buildVm(repo).also { it.init(manage = false) }
        vm.state.first { it is UpgradeUiState.Loaded && it.pricing.iap != null }

        val errors = mutableListOf<Throwable>()
        val collector = launch { vm.errorEvents.collect { errors.add(it) } }
        vm.onBuyIap(activity)
        advanceUntilIdle()

        errors shouldBe emptyList()
        coVerify(exactly = 1) { repo.restorePurchaseNow() }
        collector.cancel()
    }

    @Test
    fun `direct buy is also gated and blocks while a subscription is renewing`() = runTest2(context = testDispatcher) {
        // Even the ordinary "Buy" button (e.g. exposed during a grace window) must not launch the
        // one-time purchase while a subscription is still auto-renewing.
        val repo = mockRepo(skus = listOf(iapSkuDetails()))
        coEvery { repo.queryCurrentSubscriptions() } returns listOf(purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = true))
        val vm = buildVm(repo).also { it.init(manage = false) }
        vm.state.first { it is UpgradeUiState.Loaded && it.pricing.iap != null }

        vm.onBuyIap(activity)
        advanceUntilIdle()

        vm.events.first() shouldBe UpgradeViewModel.UpgradeEvent.SubscriptionStillRenewing
        coVerify(exactly = 0) { repo.launchBillingFlow(any(), any(), any()) }
    }
}
