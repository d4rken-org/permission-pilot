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

    // Settled fixtures: settledness rides the Info itself, and an unsettled Info would park the
    // screen in Loading instead of exercising the state under test.
    private fun notProInfo() = UpgradeRepoGplay.Info(billingData = null, isSettled = true)
    private fun graceInfo() = UpgradeRepoGplay.Info(gracePeriod = true, billingData = null, isSettled = true)
    private fun iapOwnedInfo() = UpgradeRepoGplay.Info(
        billingData = BillingData(setOf(purchase(MyPermSku.Iap.PRO_UPGRADE.id))),
        isSettled = true,
    )

    private fun iapSkuDetails(): SkuDetails = SkuDetails(
        sku = MyPermSku.Iap.PRO_UPGRADE,
        details = mockk<ProductDetails> {
            every { oneTimePurchaseOfferDetails } returns null
            every { subscriptionOfferDetails } returns null
        },
    )

    private fun subSkuDetails(): SkuDetails = SkuDetails(
        sku = MyPermSku.Sub.PRO_UPGRADE,
        details = mockk<ProductDetails> {
            every { oneTimePurchaseOfferDetails } returns null
            every { subscriptionOfferDetails } returns null
        },
    )

    // A fully-loaded catalog: Ready requires BOTH the subscription and the one-time offer.
    private fun bothSkus(): List<SkuDetails> = listOf(iapSkuDetails(), subSkuDetails())

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
        val repo = mockRepo(skus = bothSkus())
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
        val repo = mockRepo(skus = bothSkus())
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
        val repo = mockRepo(skus = bothSkus())
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
        val repo = mockRepo(skus = bothSkus())
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
        val repo = mockRepo(skus = bothSkus())
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
        val repo = mockRepo(skus = bothSkus())
        coEvery { repo.queryCurrentSubscriptions() } returns listOf(purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = true))
        val vm = buildVm(repo).also { it.init(manage = false) }
        vm.state.first { it is UpgradeUiState.Loaded && it.pricing.iap != null }

        vm.onBuyIap(activity)
        advanceUntilIdle()

        vm.events.first() shouldBe UpgradeViewModel.UpgradeEvent.SubscriptionStillRenewing
        coVerify(exactly = 0) { repo.launchBillingFlow(any(), any(), any()) }
    }

    // --- offer availability / unavailable state ------------------------------------------------

    @Test
    fun `non-pro with no usable offers shows Unavailable`() = runTest2(context = testDispatcher) {
        val repo = mockRepo() // querySkus returns an empty list -> no usable offer
        val vm = buildVm(repo).also { it.init(manage = false) }

        val state = vm.state.first { it is UpgradeUiState.Unavailable }

        (state is UpgradeUiState.Unavailable) shouldBe true
    }

    @Test
    fun `non-pro with only the one-time offer shows Unavailable`() = runTest2(context = testDispatcher) {
        // A partial catalog (subscription offer missing) is treated as unavailable, not shown as a
        // half-broken screen. Would be Loaded under an OR condition; must be Unavailable under AND.
        val repo = mockRepo(skus = listOf(iapSkuDetails()))
        val vm = buildVm(repo).also { it.init(manage = false) }

        val state = vm.state.first { it is UpgradeUiState.Unavailable }

        (state is UpgradeUiState.Unavailable) shouldBe true
    }

    @Test
    fun `non-pro with only the subscription offer shows Unavailable`() = runTest2(context = testDispatcher) {
        val repo = mockRepo(skus = listOf(subSkuDetails()))
        val vm = buildVm(repo).also { it.init(manage = false) }

        val state = vm.state.first { it is UpgradeUiState.Unavailable }

        (state is UpgradeUiState.Unavailable) shouldBe true
    }

    @Test
    fun `owner with a failed sku query still sees the ownership screen`() = runTest2(context = testDispatcher) {
        // The offer catalog is a non-pro concern; an owner must never be dropped into Unavailable
        // because Play couldn't return prices.
        val repo = mockRepo(info = iapOwnedInfo()) // querySkus empty -> Failed, but IAP is owned
        val vm = buildVm(repo).also { it.init(manage = true) }

        val loaded = vm.loaded()

        loaded.isPro shouldBe true
    }

    @Test
    fun `grace user with a failed sku query stays on the status screen`() = runTest2(context = testDispatcher) {
        val repo = mockRepo(info = graceInfo())
        val vm = buildVm(repo).also { it.init(manage = true) }

        val loaded = vm.loaded()

        loaded.isPro shouldBe true
        loaded.gracePeriod shouldBe true
    }

    @Test
    fun `retry re-runs the sku query`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        val vm = buildVm(repo).also { it.init(manage = false) }
        vm.state.first { it is UpgradeUiState.Unavailable }

        vm.retrySkuQuery()
        advanceUntilIdle()

        coVerify(atLeast = 2) { repo.querySkus() }
    }

    @Test
    fun `onResume retries the query after a failure`() = runTest2(context = testDispatcher) {
        val repo = mockRepo()
        val vm = buildVm(repo).also { it.init(manage = false) }
        vm.state.first { it is UpgradeUiState.Unavailable }

        vm.onResume()
        advanceUntilIdle()

        coVerify(atLeast = 2) { repo.querySkus() }
    }

    @Test
    fun `onResume does not re-query when offers are already loaded`() = runTest2(context = testDispatcher) {
        val repo = mockRepo(skus = bothSkus())
        val vm = buildVm(repo).also { it.init(manage = false) }
        vm.state.first { it is UpgradeUiState.Loaded }

        vm.onResume()
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.querySkus() }
    }

    @Test
    fun `switch is a silent no-op when the iap catalog is unavailable`() = runTest2(context = testDispatcher) {
        // Owner of a non-renewing subscription with a failed price query. The UI disables the switch
        // button here (pricing.iapAvailable is false); if it is invoked anyway, the VM must neither
        // launch a broken flow nor pop an error dialog — it just quietly aborts.
        val subInfo = UpgradeRepoGplay.Info(
            billingData = BillingData(setOf(purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = false))),
            isSettled = true,
        )
        val repo = mockRepo(info = subInfo) // querySkus empty -> no IAP details
        coEvery { repo.queryCurrentSubscriptions() } returns
            listOf(purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = false))
        val vm = buildVm(repo).also { it.init(manage = true) }
        vm.loaded()
        advanceUntilIdle()

        val errors = mutableListOf<Throwable>()
        val collector = launch { vm.errorEvents.collect { errors.add(it) } }
        vm.onSwitchToIap(activity)
        advanceUntilIdle()

        errors shouldBe emptyList()
        coVerify(exactly = 0) { repo.launchBillingFlow(any(), any(), any()) }
        collector.cancel()
    }
}
