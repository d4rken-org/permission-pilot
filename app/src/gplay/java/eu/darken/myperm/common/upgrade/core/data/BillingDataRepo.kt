package eu.darken.myperm.common.upgrade.core.data

import android.app.Activity
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.Purchase
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.Bugs
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.Logging.Priority.INFO
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.replayingShare
import eu.darken.myperm.common.flow.setupCommonEventHandlers
import eu.darken.myperm.common.upgrade.core.client.BillingClientConnection
import eu.darken.myperm.common.upgrade.core.client.BillingClientConnectionProvider
import eu.darken.myperm.common.upgrade.core.client.BillingException
import eu.darken.myperm.common.upgrade.core.client.BillingResultException
import eu.darken.myperm.common.upgrade.core.client.GplayServiceUnavailableException
import eu.darken.myperm.common.upgrade.core.client.ItemAlreadyOwnedBillingException
import eu.darken.myperm.common.upgrade.core.client.UserCanceledBillingException
import eu.darken.myperm.common.upgrade.core.client.isGplayUnavailablePermanent
import eu.darken.myperm.common.upgrade.core.client.isGplayUnavailableTemporary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingDataRepo @Inject constructor(
    clientConnectionProvider: BillingClientConnectionProvider,
    @AppScope private val scope: CoroutineScope,
) {

    // Wakes an in-progress connection backoff. Conflated so a request sent while the retry loop is
    // NOT sleeping is still consumed by the next backoff — exactly once, so it can't cause a spin.
    private val reconnectSignal = Channel<Unit>(Channel.CONFLATED)

    // Purchase tokens we've already acknowledged at least once. Play keeps reporting a just-acked
    // purchase as isAcknowledged=false until a fresh query supersedes it, so the ack legitimately
    // re-fires on every emission. Re-acknowledging is a documented Play no-op, and gating the ack on
    // this set would risk skipping a genuinely-needed ack (Play auto-refunds after ~3 days), so the
    // set is used ONLY to pick the log level (first ack = INFO, idempotent repeats = DEBUG). Mutated
    // from a single sequential collector, so no synchronization is needed.
    private val loggedAckTokens = mutableSetOf<String>()

    private val connectionProvider = clientConnectionProvider.connection
        .retryWhen { cause, attempt ->
            // Never give up terminally: the ack collector pins this share for the process
            // lifetime, so a completed upstream could never restart — one transient failure at
            // first use (e.g. BILLING_UNAVAILABLE while the Play Store updates itself) would
            // leave billing dead until process restart. Retry with capped backoff instead.
            if (cause is CancellationException) {
                false
            } else {
                log(TAG, WARN) { "Billing connection failed (attempt=$attempt), will retry: ${cause.asLog()}" }
                awaitReconnectBackoff(attempt)
                true
            }
        }
        .replayingShare(scope)

    val billingData: Flow<BillingData> = connectionProvider
        .flatMapLatest { it.purchases }
        .map { BillingData(purchases = it) }
        .setupCommonEventHandlers(TAG) { "billingData" }
        .replayingShare(scope)

    // Asynchronous purchase-flow failures (onPurchasesUpdated with a non-OK result), mapped like
    // the synchronous launch failures. Hot upstream — subscribers only see failures while active.
    val purchaseFailures: Flow<Throwable> = connectionProvider
        .flatMapLatest { it.purchaseFailures }
        .map { result ->
            log(TAG, WARN) { "Purchase flow failed: code=${result.responseCode}, message=${result.debugMessage}" }
            if (!BUG_REPORT_IGNORED_CODES.contains(result.responseCode)) {
                Bugs.report(RuntimeException("Purchase flow failed: code=${result.responseCode}"))
            }
            BillingResultException(result).tryMapUserFriendly()
        }
        .setupCommonEventHandlers(TAG) { "purchaseFailures" }

    init {
        connectionProvider
            .flatMapLatest { client ->
                client.purchases.map { client to it }
            }
            .onEach { (client, purchases) ->
                purchases
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .filter { !it.isAcknowledged }
                    .forEach {
                        // Keep acking unconditionally (safe over-ack bias); use the token set only to
                        // quiet idempotent-repeat logs from INFO to DEBUG.
                        if (it.purchaseToken !in loggedAckTokens) {
                            log(TAG, INFO) { "Acknowledging purchase: $it" }
                        } else {
                            log(TAG) { "Re-acknowledging (idempotent no-op until re-query): $it" }
                        }
                        client.acknowledgePurchase(it)
                        loggedAckTokens.add(it.purchaseToken)
                    }
            }
            .setupCommonEventHandlers(TAG) { "connection-acks" }
            .retryWhen { cause, attempt ->
                log(TAG, ERROR) { "Failed to acknowledge purchase: ${cause.asLog()}" }

                if (cause is CancellationException) {
                    log(TAG) { "Ack was cancelled (appScope?) cancelled." }
                    return@retryWhen false
                }

                if (attempt > 5) {
                    log(TAG, WARN) { "Reached attempt limit: $attempt due to $cause" }
                    return@retryWhen false
                }

                if (cause !is BillingException) {
                    log(TAG, WARN) { "Unknown exception type: $cause" }
                    return@retryWhen false
                }

                if (cause is BillingResultException && cause.result.isGplayUnavailablePermanent) {
                    log(TAG) { "Got BILLING_UNAVAILABLE while trying to ACK purchase." }
                    return@retryWhen false
                }

                log(TAG) { "Will retry ACK (attempt=$attempt)" }
                delay(3000 * attempt)
                true
            }
            .launchIn(scope)
    }

    // connectionProvider's retryWhen absorbs terminal errors (it never completes), so waiting on it
    // can only stall, never throw. Bound it so user actions can't hang, and kick the retry loop so a
    // connection that is mid-backoff after a failure reconnects within this call instead of waiting
    // out the capped backoff (up to 5 min).
    //
    // `first { it.isReady }` — not plain first(): after a disconnect the dead connection lingers in
    // the replay cache during the reconnect backoff. Skip it and wait for a live one (which the
    // reconnect signal above makes arrive promptly) instead of handing back an ended client.
    private suspend fun acquireConnection(): BillingClientConnection {
        requestReconnect()
        return withTimeoutOrNull(CONNECTION_TIMEOUT_MS) { connectionProvider.first { it.isReady } }
            ?: throw GplayServiceUnavailableException(
                BillingException("Timed out waiting for a billing client connection")
            )
    }

    // Buffers a reconnect request (conflated). It has an effect only when the retry loop next enters
    // its backoff — waking it so recovery doesn't wait out the capped backoff. Harmless while
    // connected: the token is consumed by the next backoff that actually occurs.
    internal fun requestReconnect() {
        reconnectSignal.trySend(Unit)
    }

    // Interruptible connection backoff: sleeps the capped linear delay, or returns early when a
    // reconnect is requested (an explicit user action). Extracted so the wake behavior is testable.
    internal suspend fun awaitReconnectBackoff(attempt: Long) {
        val backoff = (CONNECTION_RETRY_BASE_MS * (attempt + 1)).coerceAtMost(CONNECTION_RETRY_MAX_MS)
        withTimeoutOrNull(backoff) { reconnectSignal.receive() }
    }

    suspend fun querySkus(vararg skus: Sku): Collection<SkuDetails> = try {
        acquireConnection().querySkus(*skus)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.tryMapUserFriendly()
    }

    suspend fun launchBillingFlow(
        activity: Activity,
        skuDetails: SkuDetails,
        offer: Sku.Subscription.Offer? = null,
    ) {
        try {
            acquireConnection().launchBillingFlow(activity, skuDetails, offer)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to launch billing flow:\n${e.asLog()}" }
            if (e !is BillingResultException || !BUG_REPORT_IGNORED_CODES.contains(e.result.responseCode)) {
                Bugs.report(RuntimeException("Billing flow failed for ${skuDetails.sku}", e))
            }

            throw e.tryMapUserFriendly()
        }
    }

    // Queries Play now and returns the fresh purchase data directly, so callers get a real
    // happens-before instead of racing the shared billingData replay cache after a refresh.
    suspend fun refresh(): BillingData = try {
        BillingData(purchases = acquireConnection().refreshPurchases())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.tryMapUserFriendly()
    }

    // Fresh SUBS-only query for the switch-to-one-time gate. Errors propagate (mapped) so the caller
    // fails closed and does not launch the purchase.
    suspend fun querySubscriptions(): Collection<Purchase> = try {
        acquireConnection().querySubscriptions()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.tryMapUserFriendly()
    }

    companion object {
        val TAG: String = logTag("Upgrade", "Gplay", "Billing", "DataRepo")
        private const val CONNECTION_TIMEOUT_MS = 10_000L
        private const val CONNECTION_RETRY_BASE_MS = 30_000L
        private const val CONNECTION_RETRY_MAX_MS = 300_000L

        // Expected environmental/user situations get user-facing handling only, no bug report.
        private val BUG_REPORT_IGNORED_CODES = setOf(
            BillingResponseCode.USER_CANCELED,
            BillingResponseCode.BILLING_UNAVAILABLE,
            BillingResponseCode.ERROR,
            BillingResponseCode.ITEM_ALREADY_OWNED,
        )

        internal fun Throwable.tryMapUserFriendly(): Throwable = when {
            this is BillingResultException && result.responseCode == BillingResponseCode.USER_CANCELED -> {
                UserCanceledBillingException(result)
            }
            this is BillingResultException && result.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED -> {
                ItemAlreadyOwnedBillingException(result)
            }
            this is BillingResultException && this.result.isGplayUnavailableTemporary -> {
                GplayServiceUnavailableException(this)
            }
            this is BillingResultException && this.result.isGplayUnavailablePermanent -> {
                GplayServiceUnavailableException(this)
            }
            this is NoSuchElementException -> {
                // connectionProvider completed without emitting (connection retries exhausted)
                GplayServiceUnavailableException(this)
            }
            else -> this
        }
    }
}
