package eu.darken.myperm.common.upgrade.core.client

import com.android.billingclient.api.BillingResult

// Expected user action (backed out of the purchase dialog) — callers must stay silent.
class UserCanceledBillingException(val result: BillingResult) : BillingException("User canceled the billing flow")
