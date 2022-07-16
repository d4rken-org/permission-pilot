package eu.darken.myperm.common.upgrade.core.client

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult

internal val BillingResult.isSuccess: Boolean
    get() = responseCode == BillingClient.BillingResponseCode.OK