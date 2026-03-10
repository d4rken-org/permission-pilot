package eu.darken.myperm.common.upgrade.core.data

import com.android.billingclient.api.Purchase
import eu.darken.myperm.common.upgrade.core.MyPermSku

data class BillingData(
    val purchases: Collection<Purchase>
) {
    val purchasedSkus: Collection<PurchasedSku>
        get() = purchases.flatMap { it.toPurchasedSku() }

    private fun Purchase.toPurchasedSku(): Collection<PurchasedSku> {
        if (purchaseState != Purchase.PurchaseState.PURCHASED) return emptyList()
        return products.mapNotNull { productId ->
            val sku = MyPermSku.resolve(productId) ?: return@mapNotNull null
            PurchasedSku(sku, this)
        }
    }
}
