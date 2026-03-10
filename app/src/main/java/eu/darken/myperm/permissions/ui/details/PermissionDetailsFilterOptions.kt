package eu.darken.myperm.permissions.ui.details

import android.os.Parcelable
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.Installed
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PermissionDetailsFilterOptions(
    @SerialName("filters") val keys: Set<Filter> = setOf(Filter.USER_APP, Filter.SYSTEM_APP)
) : Parcelable {

    @Serializable
    enum class Filter(
        @StringRes val labelRes: Int,
        val matches: (Pkg) -> Boolean
    ) {
        USER_APP(
            labelRes = R.string.apps_filter_userapps_label,
            matches = { it is Installed && !it.isSystemApp }
        ),
        SYSTEM_APP(
            labelRes = R.string.apps_filter_systemapps_label,
            matches = { it is Installed && it.isSystemApp }
        ),
        ;
    }
}
