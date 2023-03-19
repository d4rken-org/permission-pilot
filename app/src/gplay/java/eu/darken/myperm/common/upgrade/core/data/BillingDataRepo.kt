package eu.darken.myperm.common.upgrade.core.data

import android.app.Activity
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.Bugs
import eu.darken.myperm.common.debug.logging.Logging.Priority.*
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.replayingShare
import eu.darken.myperm.common.flow.setupCommonEventHandlers
import eu.darken.myperm.common.upgrade.core.client.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingDataRepo @Inject constructor(
    clientConnectionProvider: BillingClientConnectionProvider,
    @AppScope private val scope: CoroutineScope,
) {

    private val connectionProvider = clientConnectionProvider.connection
        .catch { log(TAG, ERROR) { "Unable to provide client connection:\n${it.asLog()}" } }
        .replayingShare(scope)

    val billingData: Flow<BillingData> = connectionProvider
        .flatMapLatest { it.purchases }
        .map { BillingData(purchases = it) }
        .setupCommonEventHandlers(TAG) { "iapData" }
        .replayingShare(scope)

    init {
        connectionProvider
            .flatMapLatest { client ->
                client.purchases.map { client to it }
            }
            .onEach { (client, purchases) ->
                purchases
                    .filter {
                        val needsAck = !it.isAcknowledged

                        if (needsAck) log(TAG, INFO) { "Needs ACK: $it" }
                        else log(TAG) { "Already ACK'ed: $it" }

                        needsAck
                    }
                    .forEach {
                        log(TAG, INFO) { "Acknowledging purchase: $it" }
                        client.acknowledgePurchase(it)
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

    suspend fun getIapData(): BillingData = try {
        val clientConnection = connectionProvider.first()
        val iaps = clientConnection.queryPurchases()

        BillingData(
            purchases = iaps
        )
    } catch (e: Exception) {
        throw e.tryMapUserFriendly()
    }

    suspend fun startIapFlow(activity: Activity, sku: Sku) {
        try {
            val clientConnection = connectionProvider.first()
            clientConnection.launchBillingFlow(activity, sku)
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to start IAP flow:\n${e.asLog()}" }
            val ignoredCodes = listOf(3, 6)
            if (e !is BillingResultException || !e.result.responseCode.let { ignoredCodes.contains(it) }) {
                Bugs.report(RuntimeException("IAP flow failed for $sku", e))
            }

            throw e.tryMapUserFriendly()
        }
    }

    companion object {
        val TAG: String = logTag("Upgrade", "Gplay", "Billing", "DataRepo")

        internal fun Throwable.tryMapUserFriendly(): Throwable = when {
            this is BillingResultException && this.result.isGplayUnavailableTemporary -> {
                GplayServiceUnavailableException(this)
            }
            this is BillingResultException && this.result.isGplayUnavailablePermanent -> {
                GplayServiceUnavailableException(this)
            }
            else -> this
        }
    }
}
