package eu.darken.myperm.common.upgrade.core.client

import android.content.Context
import com.android.billingclient.api.BillingResult
import eu.darken.myperm.R
import eu.darken.myperm.common.error.HasLocalizedError
import eu.darken.myperm.common.error.LocalizedError

// Play says the user already owns what they tried to buy. Callers should attempt a restore first;
// this error surfaces only when that reconciliation fails (e.g. account mismatch).
class ItemAlreadyOwnedBillingException(
    val result: BillingResult,
) : BillingException("Item is already owned"), HasLocalizedError {

    override fun getLocalizedError(context: Context): LocalizedError = LocalizedError(
        throwable = this,
        label = context.getString(R.string.upgrades_gplay_already_owned_title),
        description = context.getString(R.string.upgrades_gplay_already_owned_error),
    )
}
