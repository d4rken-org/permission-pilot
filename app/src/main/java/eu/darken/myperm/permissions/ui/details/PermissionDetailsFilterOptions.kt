package eu.darken.myperm.permissions.ui.details

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.Installed
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class PermissionDetailsFilterOptions(
    @Json(name = "filters") val keys: Set<Filter> = setOf(Filter.USER_APP, Filter.SYSTEM_APP)
) : Parcelable {

    @JsonClass(generateAdapter = false)
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
