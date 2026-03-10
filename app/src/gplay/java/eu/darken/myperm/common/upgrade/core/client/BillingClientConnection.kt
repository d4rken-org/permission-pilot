package eu.darken.myperm.common.upgrade.core.client

import android.app.Activity
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class BillingClientConnection(
    private val client: BillingClient,
    private val purchasesGlobal: Flow<Collection<Purchase>>,
) {
    private val queryCacheIaps = MutableStateFlow<Collection<Purchase>>(emptySet())
    private val queryCacheSubs = MutableStateFlow<Collection<Purchase>>(emptySet())

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

    suspend fun refreshPurchases(): Collection<Purchase> = supervisorScope {
        val iapDeferred = async {
            runCatching { queryPurchasesByType(BillingClient.ProductType.INAPP) }
        }
        val subDeferred = async {
            runCatching { queryPurchasesByType(BillingClient.ProductType.SUBS) }
        }

        val iapResult = iapDeferred.await()
        val subResult = subDeferred.await()

        iapResult.onFailure { log(TAG, WARN) { "IAP purchase query failed: ${it.asLog()}" } }
        subResult.onFailure { log(TAG, WARN) { "Sub purchase query failed: ${it.asLog()}" } }

        val iaps = iapResult.getOrDefault(emptyList())
        val subs = subResult.getOrDefault(emptyList())

        queryCacheIaps.value = iaps
        queryCacheSubs.value = subs

        iaps + subs
    }

    private suspend fun queryPurchasesByType(productType: String): Collection<Purchase> {
        val (result, purchases) = suspendCoroutine<Pair<BillingResult, Collection<Purchase>?>> { continuation ->
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

        val result = suspendCoroutine<BillingResult> { continuation ->
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

            val (result, details) = suspendCoroutine<Pair<BillingResult, Collection<ProductDetails>?>> { continuation ->
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

        return client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()
        )
    }

    companion object {
        val TAG: String = logTag("Upgrade", "Gplay", "Billing", "ClientConnection")
    }
}
