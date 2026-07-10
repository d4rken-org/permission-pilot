package eu.darken.myperm.common.upgrade.core.client

import android.app.Activity
import android.os.SystemClock
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import eu.darken.myperm.common.debug.logging.Logging.Priority.INFO
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.setupCommonEventHandlers
import eu.darken.myperm.common.upgrade.core.data.Sku
import eu.darken.myperm.common.upgrade.core.data.SkuDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class BillingClientConnection(
    private val client: BillingClient,
    private val purchasesGlobal: Flow<Collection<Purchase>>,
    private val purchaseFailuresGlobal: Flow<BillingResult>,
) {
    private val queryCacheIaps = MutableStateFlow<Collection<Purchase>>(emptySet())
    private val queryCacheSubs = MutableStateFlow<Collection<Purchase>>(emptySet())

    // responseCode + monotonic (elapsedRealtime) time of the last synchronous launch failure.
    private val lastSyncLaunchFailure = MutableStateFlow<Pair<Int, Long>?>(null)

    // Non-OK results from onPurchasesUpdated — asynchronous purchase-flow failures. The billing
    // client may post an immediate launch failure to the listener AND return it from
    // launchBillingFlow; suppress the listener echo so each failure is handled exactly once
    // (the synchronous path already threw it). One-shot: the marker is consumed by its echo, so
    // a later same-code failure from a new billing flow is treated as a new event.
    val purchaseFailures: Flow<BillingResult> = purchaseFailuresGlobal.filter { result ->
        val syncFailure = lastSyncLaunchFailure.value
        val isEcho = isSyncLaunchFailureEcho(result, syncFailure, SystemClock.elapsedRealtime())
        if (isEcho) {
            lastSyncLaunchFailure.compareAndSet(syncFailure, null)
            log(TAG) { "Suppressing listener echo of a synchronous launch failure: code=${result.responseCode}" }
        }
        !isEcho
    }

    val purchases: Flow<Collection<Purchase>> = combine(
        purchasesGlobal, queryCacheIaps, queryCacheSubs
    ) { global, iaps, subs ->
        val combined = mutableMapOf<String, Purchase>()
        (global + iaps + subs).forEach { purchase ->
            val existing = combined[purchase.purchaseToken]
            if (existing == null) {
                combined[purchase.purchaseToken] = purchase
            } else {
                val existingPurchased = existing.purchaseState == Purchase.PurchaseState.PURCHASED
                val newPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                when {
                    newPurchased && !existingPurchased -> combined[purchase.purchaseToken] = purchase
                    newPurchased == existingPurchased && purchase.purchaseTime > existing.purchaseTime ->
                        combined[purchase.purchaseToken] = purchase
                }
            }
        }
        combined.values
    }
        .setupCommonEventHandlers(TAG) { "purchases" }

    // Returns the freshly queried PURCHASED purchases so callers get a guaranteed happens-before
    // relation instead of racing the shared purchases/billingData replay caches after a refresh.
    suspend fun refreshPurchases(): Collection<Purchase> = coroutineScope {
        log(TAG) { "refreshPurchases()" }
        val iapJob = async { queryPurchasedProducts(BillingClient.ProductType.INAPP) { queryCacheIaps.value = it } }
        val subJob = async { queryPurchasedProducts(BillingClient.ProductType.SUBS) { queryCacheSubs.value = it } }
        val iaps = iapJob.await()
        val subs = subJob.await()
        log(TAG) { "Refreshed IAPs=${iaps.getOrNull()}, SUBs=${subs.getOrNull()}" }
        combinePurchaseResults(iaps, subs)
    }

    // Never throws except on cancellation, so a single failing product-type query doesn't cancel
    // the sibling query. The query cache is only updated on success — a failed query must not wipe
    // previously known purchases (the grace period covers "couldn't verify").
    private suspend fun queryPurchasedProducts(
        productType: String,
        cache: (Collection<Purchase>) -> Unit,
    ): Result<Collection<Purchase>> = try {
        val purchases = queryPurchasesByType(productType)
        cache(purchases)
        Result.success(purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log(TAG, WARN) { "$productType purchase query failed: ${e.asLog()}" }
        Result.failure(e)
    }

    private suspend fun queryPurchasesByType(productType: String): Collection<Purchase> {
        val (result, purchases) = suspendCancellableCoroutine<Pair<BillingResult, Collection<Purchase>?>> { continuation ->
            val params = QueryPurchasesParams.newBuilder().apply {
                setProductType(productType)
            }.build()
            client.queryPurchasesAsync(params) { billingResult, purchaseList ->
                continuation.resume(billingResult to purchaseList)
            }
        }

        log(TAG) {
            "queryPurchasesByType($productType): code=${result.responseCode}, message=${result.debugMessage}, purchases=$purchases"
        }

        if (!result.isSuccess) {
            log(TAG, WARN) { "queryPurchasesByType($productType) failed" }
            throw BillingResultException(result)
        }

        return purchases ?: emptyList()
    }

    suspend fun acknowledgePurchase(purchase: Purchase) {
        val ack = AcknowledgePurchaseParams.newBuilder().apply {
            setPurchaseToken(purchase.purchaseToken)
        }.build()

        val result = suspendCancellableCoroutine<BillingResult> { continuation ->
            client.acknowledgePurchase(ack) { continuation.resume(it) }
        }

        log(TAG, INFO) { "acknowledgePurchase($purchase): code=${result.responseCode} (${result.debugMessage})" }

        if (!result.isSuccess) throw BillingResultException(result)
    }

    suspend fun querySkus(vararg skus: Sku): Collection<SkuDetails> {
        return skus.groupBy { it.type }.flatMap { (type, skuGroup) ->
            val productType = when (type) {
                Sku.Type.IAP -> BillingClient.ProductType.INAPP
                Sku.Type.SUBSCRIPTION -> BillingClient.ProductType.SUBS
            }
            val productList = skuGroup.map { sku ->
                QueryProductDetailsParams.Product.newBuilder().apply {
                    setProductId(sku.id)
                    setProductType(productType)
                }.build()
            }
            val params = QueryProductDetailsParams.newBuilder().apply {
                setProductList(productList)
            }.build()

            val (result, details) = suspendCancellableCoroutine<Pair<BillingResult, Collection<ProductDetails>?>> { continuation ->
                client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    continuation.resume(billingResult to productDetailsList.productDetailsList)
                }
            }

            log(TAG) {
                "querySkus(${skuGroup.map { it.id }}): code=${result.responseCode}, debug=${result.debugMessage}, details=$details"
            }

            if (!result.isSuccess) throw BillingResultException(result)

            details?.map { pd ->
                val sku = skuGroup.first { it.id == pd.productId }
                SkuDetails(sku, pd)
            } ?: emptyList()
        }
    }

    suspend fun launchBillingFlow(
        activity: Activity,
        skuDetails: SkuDetails,
        offer: Sku.Subscription.Offer? = null,
    ): BillingResult {
        log(TAG) { "launchBillingFlow(activity=$activity, skuDetails=$skuDetails, offer=$offer)" }
        // A new launch invalidates any stale echo marker from a previous attempt.
        lastSyncLaunchFailure.value = null

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder().apply {
            setProductDetails(skuDetails.details)
            if (offer != null && skuDetails.sku is Sku.Subscription) {
                val offerDetails = skuDetails.details.subscriptionOfferDetails
                    ?.firstOrNull { offer.matches(it) }
                if (offerDetails != null) {
                    setOfferToken(offerDetails.offerToken)
                } else {
                    log(TAG, WARN) { "Offer $offer not found, falling back to default offer" }
                    val fallback = skuDetails.details.subscriptionOfferDetails?.firstOrNull()
                    if (fallback != null) setOfferToken(fallback.offerToken)
                }
            }
        }.build()

        // The RETURNED result reports whether the flow could be launched at all (ITEM_ALREADY_OWNED,
        // BILLING_UNAVAILABLE, DEVELOPER_ERROR, ...) — failures arrive here, not as exceptions.
        val result = client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()
        )
        log(TAG) {
            "launchBillingFlow(sku=${skuDetails.sku}): code=${result.responseCode}, message=${result.debugMessage}"
        }
        if (!result.isSuccess) {
            lastSyncLaunchFailure.value = result.responseCode to SystemClock.elapsedRealtime()
            throw BillingResultException(result)
        }

        return result
    }

    companion object {
        val TAG: String = logTag("Upgrade", "Gplay", "Billing", "ClientConnection")
        internal const val SYNC_LAUNCH_FAILURE_ECHO_WINDOW_MS = 5_000L

        // A listener failure matching a just-thrown synchronous launch failure is an echo of the
        // same event, not a new one. Times are monotonic (elapsedRealtime); a negative age is
        // never an echo. Pure and unit-tested.
        internal fun isSyncLaunchFailureEcho(
            result: BillingResult,
            syncFailure: Pair<Int, Long>?,
            now: Long,
        ): Boolean = syncFailure != null &&
            syncFailure.first == result.responseCode &&
            (now - syncFailure.second) in 0 until SYNC_LAUNCH_FAILURE_ECHO_WINDOW_MS

        // Combines the two product-type query results: a purchase found by either type is
        // authoritative; an error is only propagated when nothing was found, so callers can tell
        // "not owned" apart from "couldn't verify one product type". Pure and unit-tested.
        internal fun combinePurchaseResults(
            iaps: Result<Collection<Purchase>>,
            subs: Result<Collection<Purchase>>,
        ): Collection<Purchase> {
            val found = iaps.getOrNull().orEmpty() + subs.getOrNull().orEmpty()
            return when {
                found.isNotEmpty() -> found.sortedByDescending { it.purchaseTime }
                else -> {
                    (iaps.exceptionOrNull() ?: subs.exceptionOrNull())?.let { throw it }
                    emptyList()
                }
            }
        }
    }
}
