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
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.setupCommonEventHandlers
import eu.darken.myperm.common.upgrade.core.data.Sku
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class BillingClientConnection(
    private val client: BillingClient,
    private val purchasesGlobal: Flow<Collection<Purchase>>,
) {
    private val purchasesLocal = MutableStateFlow<Collection<Purchase>>(emptySet())
    val purchases: Flow<Collection<Purchase>> = combine(purchasesGlobal, purchasesLocal) { global, local ->
        val combined = mutableMapOf<String, Purchase>()
        global.plus(local).toSet().sortedByDescending { it.purchaseTime }.forEach { purchase ->
            combined[purchase.orderId!!] = purchase
        }
        combined.values
    }
        .setupCommonEventHandlers(TAG) { "purchases" }

    suspend fun queryPurchases(): Collection<Purchase> {
        val (result: BillingResult, purchases) = suspendCoroutine<Pair<BillingResult, Collection<Purchase>?>> { continuation ->
            val params = QueryPurchasesParams.newBuilder().apply {
                setProductType(BillingClient.ProductType.INAPP)
            }.build()
            client.queryPurchasesAsync(params) { result, purchases ->
                continuation.resume(result to purchases)
            }
        }

        log(TAG) { "queryPurchases(): code=${result.responseCode}, message=${result.debugMessage}, purchases=$purchases" }

        if (!result.isSuccess) {
            log(TAG, WARN) { "queryPurchases() failed" }
            throw BillingResultException(result)
        } else {
            requireNotNull(purchases)
        }

        purchasesLocal.value = purchases
        return purchases
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

    suspend fun querySku(sku: Sku): Sku.Details {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder().apply {
                setProductId(sku.id)
                setProductType(BillingClient.ProductType.INAPP)
            }.build()
        )
        val params = QueryProductDetailsParams.newBuilder().apply {
            setProductList(productList)
        }.build()

        val (result, details) = suspendCoroutine<Pair<BillingResult, Collection<ProductDetails>?>> { continuation ->
            client.queryProductDetailsAsync(params) { result, productDetailsList ->
                continuation.resume(result to productDetailsList.productDetailsList)
            }
        }

        log(TAG) {
            "querySku(sku=$sku): code=${result.responseCode}, debug=${result.debugMessage}), productDetails=$details"
        }

        if (!result.isSuccess) throw BillingResultException(result)

        if (details.isNullOrEmpty()) throw IllegalStateException("Unknown SKU, no details available.")

        return Sku.Details(sku, details)
    }

    suspend fun launchBillingFlow(activity: Activity, sku: Sku): BillingResult {
        log(TAG) { "launchBillingFlow(activity=$activity, sku=$sku)" }
        val skuDetails = querySku(sku)
        return launchBillingFlow(activity, skuDetails)
    }

    suspend fun launchBillingFlow(activity: Activity, skuDetails: Sku.Details): BillingResult {
        log(TAG) { "launchBillingFlow(activity=$activity, skuDetails=$skuDetails)" }
        val productDetails = skuDetails.details.single()
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().apply {
                setProductDetails(productDetails)
            }.build()
        )
        return client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
        )
    }

    companion object {
        val TAG: String = logTag("Upgrade", "Gplay", "Billing", "ClientConnection")
    }
}