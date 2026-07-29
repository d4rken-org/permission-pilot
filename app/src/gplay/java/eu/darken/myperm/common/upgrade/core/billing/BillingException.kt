package eu.darken.myperm.common.upgrade.core.billing

import android.content.Context
import eu.darken.myperm.R
import eu.darken.myperm.common.error.HasLocalizedError
import eu.darken.myperm.common.error.LocalizedError

open class BillingException(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : Exception(), HasLocalizedError {

    override fun getLocalizedError(context: Context): LocalizedError = LocalizedError(
        throwable = this,
        label = context.getString(R.string.upgrades_gplay_billing_error_label),
        description = context.getString(R.string.upgrades_gplay_billing_error_description, message)
    )
}