package eu.darken.myperm.common.upgrade.core.billing

import android.content.Context
import eu.darken.myperm.R
import eu.darken.myperm.common.error.HasLocalizedError
import eu.darken.myperm.common.error.LocalizedError

class GplayServiceUnavailableException(cause: Throwable) :
    BillingException("Google Play services are unavailable.", cause), HasLocalizedError {

    override fun getLocalizedError(context: Context): LocalizedError = LocalizedError(
        throwable = this,
        label = context.getString(R.string.upgrades_gplay_unavailable_error),
        description = context.getString(R.string.upgrades_gplay_unavailable_error_description),
    )
}
