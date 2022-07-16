package eu.darken.myperm.common.upgrade.core.client

import android.content.Context
import eu.darken.myperm.R
import eu.darken.myperm.common.error.HasLocalizedError
import eu.darken.myperm.common.error.LocalizedError

class GplayServiceUnavailableException(cause: Throwable) : Exception("Google Play services are unavailable.", cause),
    HasLocalizedError {
    override fun getLocalizedError(context: Context): LocalizedError {
        return LocalizedError(
            throwable = this,
            label = "Google Play Services Unavailable",
            description = context.getString(R.string.upgrades_gplay_unavailable_error)
        )
    }


}