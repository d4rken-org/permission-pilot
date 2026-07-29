package eu.darken.myperm.common.upgrade.core.billing

import com.android.billingclient.api.Purchase

data class BillingData(
    val purchases: Collection<Purchase>
)