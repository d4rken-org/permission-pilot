package eu.darken.myperm.common.upgrade.core

import com.android.billingclient.api.Purchase
import eu.darken.myperm.common.upgrade.core.data.BillingData
import eu.darken.myperm.common.upgrade.core.data.BillingDataRepo
import eu.darken.myperm.common.upgrade.core.data.PurchasedSku
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.runTest2
import testhelpers.datastore.mockDataStoreValue
import java.time.Duration
import java.time.Instant

class UpgradeRepoGplayTest : BaseTest() {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val billingDataRepo = mockk<BillingDataRepo>()
    private val billingCache = mockk<BillingCache>()

    // Builds a repo whose cached last-Pro state is `lastProAt`/`lastSku`. billingData is stubbed
    // because the eager upgradeInfo flow subscribes it at construction.
    private fun repo(
        lastProAt: Long = 0L,
        lastSku: String = "",
        billingData: Flow<BillingData> = flowOf(BillingData(emptySet())),
    ): UpgradeRepoGplay {
        every { billingDataRepo.billingData } returns billingData
        every { billingDataRepo.purchaseFailures } returns emptyFlow()
        val atValue = mockDataStoreValue(lastProAt)
        val skuValue = mockDataStoreValue(lastSku)
        every { billingCache.lastProStateAt } returns atValue
        every { billingCache.lastProStateSku } returns skuValue
        coEvery { billingCache.confirmPro(any(), any()) } coAnswers {
            atValue.value(arg(0))
            skuValue.value(arg(1))
        }
        return UpgradeRepoGplay(scope, billingDataRepo, billingCache)
    }

    private fun proPurchase(sku: String = MyPermSku.Iap.PRO_UPGRADE.id) = mockk<Purchase>().apply {
        every { products } returns listOf(sku)
        every { purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchaseTime } returns Instant.parse("2024-01-01T00:00:00Z").toEpochMilli()
    }

    @Test fun `restore returns pro when a purchase is found`() = runTest2 {
        coEvery { billingDataRepo.refresh() } returns BillingData(setOf(proPurchase()))

        repo().restorePurchaseNow().isPro shouldBe true
    }

    @Test fun `restore records the confirmed sku and timestamp`() = runTest2 {
        coEvery { billingDataRepo.refresh() } returns BillingData(setOf(proPurchase()))
        val repo = repo()

        repo.restorePurchaseNow()

        billingCache.lastProStateAt.value() shouldBeGreaterThan 0L
        billingCache.lastProStateSku.value() shouldBe MyPermSku.Iap.PRO_UPGRADE.id
    }

    @Test fun `restore keeps pro within grace when the query comes back empty`() = runTest2 {
        coEvery { billingDataRepo.refresh() } returns BillingData(emptySet())

        repo(lastProAt = System.currentTimeMillis() - 1_000).restorePurchaseNow().isPro shouldBe true
    }

    @Test fun `restore is not pro when the query is empty and grace has expired`() = runTest2 {
        coEvery { billingDataRepo.refresh() } returns BillingData(emptySet())

        val expired = System.currentTimeMillis() - UpgradeRepoGplay.GRACE_PERIOD_NORMAL_MS - 1_000
        repo(lastProAt = expired).restorePurchaseNow().isPro shouldBe false
    }

    @Test fun `restore keeps pro within grace when the query errors`() = runTest2 {
        coEvery { billingDataRepo.refresh() } throws RuntimeException("Play unavailable")

        repo(lastProAt = System.currentTimeMillis() - 1_000).restorePurchaseNow().isPro shouldBe true
    }

    @Test fun `restore rethrows the error when it happens outside grace`() = runTest2 {
        coEvery { billingDataRepo.refresh() } throws RuntimeException("Play unavailable")

        shouldThrow<RuntimeException> {
            repo(lastProAt = 0L).restorePurchaseNow()
        }
    }

    @Test fun `restore publishes its outcome into the canonical upgradeInfo`() = runTest2 {
        coEvery { billingDataRepo.refresh() } throws RuntimeException("Play unavailable")
        // 8h ago: outside the reactive 6h "normal" window, inside the 24h "error" window.
        val eightHoursAgo = System.currentTimeMillis() - Duration.ofHours(8).toMillis()
        val source = MutableSharedFlow<BillingData>()
        val repo = repo(lastProAt = eightHoursAgo, billingData = source)

        // The reactive pipeline evaluates empty data with the 6h window -> non-Pro.
        source.emit(BillingData(emptySet()))
        repo.upgradeInfo.value.isPro shouldBe false

        // The explicit restore hits a billing error within the 24h error window -> grace-Pro.
        // That outcome must reach the canonical upgradeInfo, not just the returned value.
        repo.restorePurchaseNow().isPro shouldBe true
        repo.upgradeInfo.value.isPro shouldBe true
    }

    @Test fun `reactive empty data after a billing error keeps the error grace window`() = runTest2 {
        coEvery { billingDataRepo.refresh() } throws RuntimeException("Play unavailable")
        val eightHoursAgo = System.currentTimeMillis() - Duration.ofHours(8).toMillis()
        val source = MutableSharedFlow<BillingData>()
        val repo = repo(lastProAt = eightHoursAgo, billingData = source)

        // Records the billing error and concludes grace-Pro via the 24h window.
        repo.restorePurchaseNow().isPro shouldBe true

        // A racing reactive empty emission processed afterwards must not downgrade to the 6h
        // window — an empty result during billing trouble isn't evidence of "not owned".
        source.emit(BillingData(emptySet()))
        repo.upgradeInfo.value.isPro shouldBe true
    }

    @Test fun `a successful refresh retires the error grace override`() = runTest2 {
        val eightHoursAgo = System.currentTimeMillis() - Duration.ofHours(8).toMillis()
        val repo = repo(lastProAt = eightHoursAgo)

        // A billing error activates the 24h error window...
        coEvery { billingDataRepo.refresh() } throws RuntimeException("Play unavailable")
        repo.restorePurchaseNow().isPro shouldBe true

        // ...but once Play answers authoritatively, empty settles back to the 6h window.
        coEvery { billingDataRepo.refresh() } returns BillingData(emptySet())
        repo.restorePurchaseNow().isPro shouldBe false
    }

    @Test fun `permanent IAP keeps grace well beyond the subscription windows`() = runTest2 {
        coEvery { billingDataRepo.refresh() } returns BillingData(emptySet())
        // 20 days ago: far past the 6h/24h windows, but within the 30-day IAP window.
        val twentyDaysAgo = System.currentTimeMillis() - Duration.ofDays(20).toMillis()

        repo(lastProAt = twentyDaysAgo, lastSku = MyPermSku.Iap.PRO_UPGRADE.id)
            .restorePurchaseNow().isPro shouldBe true
    }

    @Test fun `subscription grace expires after the short window`() = runTest2 {
        coEvery { billingDataRepo.refresh() } returns BillingData(emptySet())
        val twentyDaysAgo = System.currentTimeMillis() - Duration.ofDays(20).toMillis()

        repo(lastProAt = twentyDaysAgo, lastSku = MyPermSku.Sub.PRO_UPGRADE.id)
            .restorePurchaseNow().isPro shouldBe false
    }

    @Test fun `legacy cache without a sku gets the short windows`() = runTest2 {
        coEvery { billingDataRepo.refresh() } returns BillingData(emptySet())
        val twentyDaysAgo = System.currentTimeMillis() - Duration.ofDays(20).toMillis()

        repo(lastProAt = twentyDaysAgo, lastSku = "").restorePurchaseNow().isPro shouldBe false
    }

    @Test fun `a future timestamp is reset instead of granting indefinite grace`() = runTest2 {
        coEvery { billingDataRepo.refresh() } returns BillingData(emptySet())
        val future = System.currentTimeMillis() + Duration.ofDays(365).toMillis()
        val repo = repo(lastProAt = future)

        // Treated as "just now": still in grace for this evaluation, but the stored timestamp is
        // clamped so the grace window ends on schedule instead of a year from now.
        repo.restorePurchaseNow().isPro shouldBe true
        billingCache.lastProStateAt.value() shouldBeLessThanOrEqual System.currentTimeMillis()
    }

    @Test fun `grace window constants`() {
        (UpgradeRepoGplay.GRACE_PERIOD_IAP_MS > UpgradeRepoGplay.GRACE_PERIOD_ERROR_MS) shouldBe true
        (UpgradeRepoGplay.GRACE_PERIOD_ERROR_MS > UpgradeRepoGplay.GRACE_PERIOD_NORMAL_MS) shouldBe true
        UpgradeRepoGplay.GRACE_PERIOD_IAP_MS shouldBe Duration.ofDays(30).toMillis()
    }

    @Test fun `preferredProSku prefers the permanent IAP when both are owned`() {
        val iap = PurchasedSku(MyPermSku.Iap.PRO_UPGRADE, mockk<Purchase>())
        val sub = PurchasedSku(MyPermSku.Sub.PRO_UPGRADE, mockk<Purchase>())

        UpgradeRepoGplay.preferredProSku(listOf(sub, iap))?.sku shouldBe MyPermSku.Iap.PRO_UPGRADE
        UpgradeRepoGplay.preferredProSku(listOf(iap))?.sku shouldBe MyPermSku.Iap.PRO_UPGRADE
        UpgradeRepoGplay.preferredProSku(listOf(sub))?.sku shouldBe MyPermSku.Sub.PRO_UPGRADE
        UpgradeRepoGplay.preferredProSku(emptyList()) shouldBe null
    }
}
