package eu.darken.myperm.common.upgrade.core.billing

import android.content.Context
import eu.darken.myperm.R
import eu.darken.myperm.common.error.HasLocalizedError
import eu.darken.myperm.common.error.LocalizedError

class ItemAlreadyOwnedBillingException(cause: Throwable) :
    BillingException("Item is already owned.", cause), HasLocalizedError {

    override fun getLocalizedError(context: Context): LocalizedError = LocalizedError(
        throwable = this,
        label = context.getString(R.string.upgrades_gplay_already_owned_title),
        description = context.getString(R.string.upgrades_gplay_already_owned_error),
    )
}
