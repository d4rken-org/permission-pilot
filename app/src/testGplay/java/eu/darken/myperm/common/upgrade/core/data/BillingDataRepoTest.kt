package eu.darken.myperm.common.upgrade.core.data

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import eu.darken.myperm.common.upgrade.core.client.BillingClientConnection
import eu.darken.myperm.common.upgrade.core.client.BillingClientConnectionProvider
import eu.darken.myperm.common.upgrade.core.client.BillingResultException
import eu.darken.myperm.common.upgrade.core.client.GplayServiceUnavailableException
import eu.darken.myperm.common.upgrade.core.client.ItemAlreadyOwnedBillingException
import eu.darken.myperm.common.upgrade.core.client.UserCanceledBillingException
import eu.darken.myperm.common.upgrade.core.data.BillingDataRepo.Companion.tryMapUserFriendly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.runTest2

class BillingDataRepoTest : BaseTest() {

    private fun resultException(code: Int) = BillingResultException(
        BillingResult.newBuilder().setResponseCode(code).build()
    )

    private fun repoWithListenerFailure(code: Int): BillingDataRepo {
        val connection = mockk<BillingClientConnection> {
            every { purchases } returns emptyFlow()
            every { purchaseFailures } returns flowOf(BillingResult.newBuilder().setResponseCode(code).build())
        }
        val provider = mockk<BillingClientConnectionProvider> {
            every { this@mockk.connection } returns flowOf(connection)
        }
        return BillingDataRepo(provider, CoroutineScope(Dispatchers.Unconfined))
    }

    @Test
    fun `async listener failures are mapped like launch failures`() = runTest2 {
        repoWithListenerFailure(BillingResponseCode.ITEM_ALREADY_OWNED)
            .purchaseFailures.first()
            .shouldBeInstanceOf<ItemAlreadyOwnedBillingException>()

        repoWithListenerFailure(BillingResponseCode.USER_CANCELED)
            .purchaseFailures.first()
            .shouldBeInstanceOf<UserCanceledBillingException>()

        repoWithListenerFailure(BillingResponseCode.DEVELOPER_ERROR)
            .purchaseFailures.first()
            .shouldBeInstanceOf<BillingResultException>()
    }

    @Test
    fun `user cancel maps to the silent cancel exception`() {
        resultException(BillingResponseCode.USER_CANCELED)
            .tryMapUserFriendly()
            .shouldBeInstanceOf<UserCanceledBillingException>()
    }

    @Test
    fun `already owned maps to its own exception`() {
        resultException(BillingResponseCode.ITEM_ALREADY_OWNED)
            .tryMapUserFriendly()
            .shouldBeInstanceOf<ItemAlreadyOwnedBillingException>()
    }

    @Test
    fun `temporary and permanent unavailability map to the service error`() {
        resultException(BillingResponseCode.SERVICE_UNAVAILABLE)
            .tryMapUserFriendly()
            .shouldBeInstanceOf<GplayServiceUnavailableException>()

        resultException(BillingResponseCode.BILLING_UNAVAILABLE)
            .tryMapUserFriendly()
            .shouldBeInstanceOf<GplayServiceUnavailableException>()
    }

    @Test
    fun `a completed connection flow maps to the service error`() {
        // connectionProvider.first() throws this when the flow completed without emitting
        NoSuchElementException("Flow is empty")
            .tryMapUserFriendly()
            .shouldBeInstanceOf<GplayServiceUnavailableException>()
    }

    @Test
    fun `unrelated errors pass through unchanged`() {
        val boom = IllegalStateException("boom")
        boom.tryMapUserFriendly() shouldBe boom

        val developerError = resultException(BillingResponseCode.DEVELOPER_ERROR)
        developerError.tryMapUserFriendly() shouldBe developerError
    }
}
