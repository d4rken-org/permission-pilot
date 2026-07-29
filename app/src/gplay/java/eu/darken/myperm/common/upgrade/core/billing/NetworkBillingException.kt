package eu.darken.myperm.common.upgrade.core.billing

import android.content.Context
import eu.darken.myperm.R
import eu.darken.myperm.common.error.HasLocalizedError
import eu.darken.myperm.common.error.LocalizedError

class NetworkBillingException(cause: Throwable) :
    BillingException("Unable to connect to Google Play.", cause), HasLocalizedError {

    override fun getLocalizedError(context: Context): LocalizedError = LocalizedError(
        throwable = this,
        label = context.getString(R.string.upgrades_gplay_network_error_title),
        description = context.getString(R.string.upgrades_gplay_network_error_description),
    )
}
