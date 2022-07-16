package eu.darken.myperm.common.upgrade.core.data

import com.android.billingclient.api.SkuDetails

data class Sku(
    val id: String
) {
    data class Details(
        val sku: Sku,
        val details: Collection<SkuDetails>,
    )
}