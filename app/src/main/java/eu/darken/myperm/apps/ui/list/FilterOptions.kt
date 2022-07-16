package eu.darken.myperm.apps.ui.list

import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.types.BaseApp

data class FilterOptions(
    val keys: Set<Filter> = setOf(Filter.USER_APP)
) {
    enum class Filter(
        @StringRes val labelRes: Int,
        val matches: (BaseApp) -> Boolean
    ) {
        SYSTEM_APP(
            labelRes = R.string.apps_filter_systemapps_label,
            matches = { it.isSystemApp }
        ),
        USER_APP(
            labelRes = R.string.apps_filter_userapps_label,
            matches = { !it.isSystemApp }
        ),
        NO_INTERNET(
            labelRes = R.string.apps_filter_nointernet_label,
            matches = { it.internetAccess != BaseApp.InternetAccess.DIRECT }
        ),
        ;
    }
}