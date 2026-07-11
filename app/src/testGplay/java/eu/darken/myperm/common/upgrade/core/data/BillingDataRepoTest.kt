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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.runTest2

class BillingDataRepoTest : BaseTest() {

    private fun resultException(code: Int) = BillingResultException(
        BillingResult.newBuilder().setResponseCode(code).build()
    )

    private fun repo(): BillingDataRepo {
        val connection = mockk<BillingClientConnection> {
            every { purchases } returns emptyFlow()
            every { purchaseFailures } returns emptyFlow()
        }
        val provider = mockk<BillingClientConnectionProvider> {
            every { this@mockk.connection } returns flowOf(connection)
        }
        return BillingDataRepo(provider, CoroutineScope(Dispatchers.Unconfined))
    }

    @Test
    fun `a reconnect request wakes the connection backoff with no delay`() =
        runTest2(context = StandardTestDispatcher()) {
            val repo = repo()
            var finished = false
            // attempt=100 pins the backoff at the 5-min cap; only a signal can end it without time passing.
            launch { repo.awaitReconnectBackoff(attempt = 100); finished = true }
            runCurrent()
            finished shouldBe false

            repo.requestReconnect()
            runCurrent() // no virtual time advanced — the signal alone must complete the wait
            finished shouldBe true
        }

    @Test
    fun `the connection backoff waits out its delay when no reconnect is requested`() =
        runTest2(context = StandardTestDispatcher()) {
            val repo = repo()
            var finished = false
            launch { repo.awaitReconnectBackoff(attempt = 0); finished = true } // 30s backoff

            advanceTimeBy(29_999)
            runCurrent()
            finished shouldBe false

            advanceTimeBy(2)
            runCurrent()
            finished shouldBe true
        }

    @Test
    fun `acquireConnection skips a disconnected cached connection and waits for a ready one`() =
        runTest2(context = StandardTestDispatcher()) {
            // A disconnected connection lingers in the replay cache during the reconnect backoff;
            // acquireConnection must not hand it back — it must wait for the next live one.
            val source = MutableSharedFlow<BillingClientConnection>(replay = 1)
            val provider = mockk<BillingClientConnectionProvider> {
                every { connection } returns source
            }
            // backgroundScope so the repo's process-lifetime collectors (which subscribe the
            // never-completing source) are cancelled when the test body ends.
            val repo = BillingDataRepo(provider, backgroundScope)

            val stale = mockk<BillingClientConnection>(relaxed = true) {
                every { isReady } returns false
                every { purchases } returns emptyFlow()
                every { purchaseFailures } returns emptyFlow()
            }
            source.emit(stale)
            runCurrent()

            val sku = mockk<Sku>()
            val call = async { repo.querySkus(sku) }
            runCurrent()
            call.isCompleted shouldBe false // dead cached connection skipped — still waiting

            val live = mockk<BillingClientConnection>(relaxed = true) {
                every { isReady } returns true
                every { purchases } returns emptyFlow()
                every { purchaseFailures } returns emptyFlow()
                coEvery { querySkus(sku) } returns emptyList()
            }
            source.emit(live)
            runCurrent()
            call.await()

            coVerify(exactly = 1) { live.querySkus(sku) }
            coVerify(exactly = 0) { stale.querySkus(sku) }
        }

    @Test
    fun `a reconnect request sent before the backoff is consumed exactly once, no spin`() =
        runTest2(context = StandardTestDispatcher()) {
            val repo = repo()
            // Request arrives while the loop is NOT waiting (conflated channel holds it).
            repo.requestReconnect()

            var firstFinished = false
            launch { repo.awaitReconnectBackoff(attempt = 100); firstFinished = true }
            runCurrent()
            firstFinished shouldBe true // the buffered request wakes the first backoff immediately

            // The next backoff must NOT be woken by the already-consumed request.
            var secondFinished = false
            launch { repo.awaitReconnectBackoff(attempt = 100); secondFinished = true }
            runCurrent()
            secondFinished shouldBe false
        }

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
