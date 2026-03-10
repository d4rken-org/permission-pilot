package eu.darken.myperm.common.upgrade.core.data

import com.android.billingclient.api.ProductDetails

interface Sku {
    val id: String
    val type: Type

    enum class Type {
        IAP,
        SUBSCRIPTION,
    }

    interface Iap : Sku {
        override val type: Type get() = Type.IAP
    }

    interface Subscription : Sku {
        override val type: Type get() = Type.SUBSCRIPTION

        data class Offer(
            val basePlanId: String,
            val offerId: String? = null,
        ) {
            fun matches(offerDetails: ProductDetails.SubscriptionOfferDetails): Boolean =
                offerDetails.basePlanId == basePlanId && offerDetails.offerId == offerId
        }
    }
}
