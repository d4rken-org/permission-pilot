package eu.darken.myperm.common.upgrade.ui

import com.android.billingclient.api.Purchase
import eu.darken.myperm.common.upgrade.core.MyPermSku
import eu.darken.myperm.common.upgrade.core.UpgradeRepoGplay
import eu.darken.myperm.common.upgrade.core.data.BillingData
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class UpgradeUiStateTest : BaseTest() {

    private val hour = 60 * 60 * 1000L
    private val diagnosticsAfter = 24 * hour

    private fun purchase(productId: String, autoRenew: Boolean = false) = mockk<Purchase> {
        every { products } returns listOf(productId)
        every { purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchaseTime } returns 1_000L
        every { isAutoRenewing } returns autoRenew
        every { isAcknowledged } returns true
    }

    private fun infoOwning(vararg purchases: Purchase) =
        UpgradeRepoGplay.Info(billingData = BillingData(purchases.toSet()))

    private fun graceInfo() = UpgradeRepoGplay.Info(gracePeriod = true, billingData = null)

    @Test
    fun `ownership - iap only`() {
        val info = infoOwning(purchase(MyPermSku.Iap.PRO_UPGRADE.id))
        val ownership = info.toOwnership()
        ownership.hasIap shouldBe true
        ownership.subscription shouldBe null
    }

    @Test
    fun `ownership - renewing subscription`() {
        val info = infoOwning(purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = true))
        val ownership = info.toOwnership()
        ownership.hasIap shouldBe false
        ownership.subscription?.isAutoRenewing shouldBe true
    }

    @Test
    fun `ownership - conservative, renewing if ANY record renews`() {
        val info = infoOwning(
            purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = false),
            purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = true),
        )
        info.toOwnership().subscription?.isAutoRenewing shouldBe true
    }

    @Test
    fun `switch unlocked only when a subscription is owned and not renewing`() {
        val notRenewing = toLoadedState(
            manageMode = true,
            info = infoOwning(purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = false)),
            pricing = Pricing(),
            lastProConfirmedAt = 0L,
            now = 0L,
            isSettled = true,
            action = ActionState.IDLE,
            wasEverPro = true,
            graceDiagnosticsAfterMs = diagnosticsAfter,
        )
        notRenewing.switchUnlocked shouldBe true

        val renewing = notRenewing.copy(
            ownership = infoOwning(purchase(MyPermSku.Sub.PRO_UPGRADE.id, autoRenew = true)).toOwnership(),
        )
        renewing.switchUnlocked shouldBe false
    }

    @Test
    fun `grace within window does not show diagnostics`() {
        val now = 100 * hour
        val state = toLoadedState(
            manageMode = true,
            info = graceInfo(),
            pricing = Pricing(),
            lastProConfirmedAt = now - hour, // 1h ago, well within 24h
            now = now,
            isSettled = true,
            action = ActionState.IDLE,
            wasEverPro = true,
            graceDiagnosticsAfterMs = diagnosticsAfter,
        )
        state.gracePeriod shouldBe true
        state.graceHint?.showDiagnostics shouldBe false
    }

    @Test
    fun `grace past 24h shows diagnostics`() {
        val now = 100 * hour
        val state = toLoadedState(
            manageMode = true,
            info = graceInfo(),
            pricing = Pricing(),
            lastProConfirmedAt = now - 25 * hour,
            now = now,
            isSettled = true,
            action = ActionState.IDLE,
            wasEverPro = true,
            graceDiagnosticsAfterMs = diagnosticsAfter,
        )
        state.graceHint?.showDiagnostics shouldBe true
    }

    @Test
    fun `wasPreviouslyPro only when ever-pro and not currently pro`() {
        fun build(info: UpgradeRepoGplay.Info, wasEverPro: Boolean) = toLoadedState(
            manageMode = false,
            info = info,
            pricing = Pricing(),
            lastProConfirmedAt = 0L,
            now = 0L,
            isSettled = true,
            action = ActionState.IDLE,
            wasEverPro = wasEverPro,
            graceDiagnosticsAfterMs = diagnosticsAfter,
        )

        build(UpgradeRepoGplay.Info(billingData = null), wasEverPro = true).wasPreviouslyPro shouldBe true
        build(UpgradeRepoGplay.Info(billingData = null), wasEverPro = false).wasPreviouslyPro shouldBe false
        build(infoOwning(purchase(MyPermSku.Iap.PRO_UPGRADE.id)), wasEverPro = true).wasPreviouslyPro shouldBe false
    }
}
