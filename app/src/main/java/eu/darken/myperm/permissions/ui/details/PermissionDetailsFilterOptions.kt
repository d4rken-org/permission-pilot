package eu.darken.myperm.permissions.ui.details

import android.os.Parcelable
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.container.PermissionAppRef
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PermissionDetailsFilterOptions(
    val filters: Set<Filter> = setOf(Filter.USER_APP, Filter.SYSTEM_APP)
) : Parcelable {

    @Serializable
    enum class Filter(
        @StringRes val labelRes: Int,
        val matches: (PermissionAppRef) -> Boolean
    ) {
        @SerialName("USER_APP")
        USER_APP(
            labelRes = R.string.apps_filter_userapps_label,
            matches = { !it.isSystemApp }
        ),
        @SerialName("SYSTEM_APP")
        SYSTEM_APP(
            labelRes = R.string.apps_filter_systemapps_label,
            matches = { it.isSystemApp }
        ),
        ;
    }
}
