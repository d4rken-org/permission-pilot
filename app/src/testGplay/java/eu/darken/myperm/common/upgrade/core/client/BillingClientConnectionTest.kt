package eu.darken.myperm.common.upgrade.core.client

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class BillingClientConnectionTest : BaseTest() {

    private fun purchase(time: Long) = mockk<Purchase>().apply {
        every { purchaseTime } returns time
    }

    @Test
    fun `combines both product types, newest first`() {
        val older = purchase(1_000)
        val newer = purchase(2_000)

        BillingClientConnection.combinePurchaseResults(
            iaps = Result.success(listOf(older)),
            subs = Result.success(listOf(newer)),
        ) shouldBe listOf(newer, older)
    }

    @Test
    fun `a single product-type failure does not mask a purchase found by the other`() {
        val owned = purchase(1_000)

        BillingClientConnection.combinePurchaseResults(
            iaps = Result.success(listOf(owned)),
            subs = Result.failure(RuntimeException("SUBS query failed")),
        ) shouldBe listOf(owned)

        BillingClientConnection.combinePurchaseResults(
            iaps = Result.failure(RuntimeException("IAP query failed")),
            subs = Result.success(listOf(owned)),
        ) shouldBe listOf(owned)
    }

    @Test
    fun `both product types empty returns empty`() {
        BillingClientConnection.combinePurchaseResults(
            iaps = Result.success(emptyList()),
            subs = Result.success(emptyList()),
        ) shouldBe emptyList()
    }

    @Test
    fun `nothing found but a query failed rethrows the error`() {
        shouldThrow<RuntimeException> {
            BillingClientConnection.combinePurchaseResults(
                iaps = Result.success(emptyList()),
                subs = Result.failure(RuntimeException("SUBS query failed")),
            )
        }
    }

    private fun result(code: Int) = BillingResult.newBuilder().setResponseCode(code).build()

    @Test
    fun `listener echo of a synchronous launch failure is detected`() {
        val code = BillingClient.BillingResponseCode.DEVELOPER_ERROR
        val now = 100_000L
        val justFailed = code to now - 1_000

        BillingClientConnection.isSyncLaunchFailureEcho(result(code), justFailed, now) shouldBe true

        // A different code is a new failure, not an echo.
        BillingClientConnection.isSyncLaunchFailureEcho(
            result(BillingClient.BillingResponseCode.ERROR), justFailed, now,
        ) shouldBe false

        // Outside the echo window it counts as a new failure again.
        val longAgo = code to now - BillingClientConnection.SYNC_LAUNCH_FAILURE_ECHO_WINDOW_MS - 1
        BillingClientConnection.isSyncLaunchFailureEcho(result(code), longAgo, now) shouldBe false

        // No synchronous failure recorded at all.
        BillingClientConnection.isSyncLaunchFailureEcho(result(code), null, now) shouldBe false

        // A negative age is never an echo (clock anomalies must not suppress real failures).
        BillingClientConnection.isSyncLaunchFailureEcho(result(code), code to now + 1_000, now) shouldBe false
    }
}
