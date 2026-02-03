package eu.darken.myperm.apps.ui.details

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class AppDetailsFilterOptions(
    @Json(name = "filters") val keys: Set<Filter> = setOf(Filter.GRANTED, Filter.DENIED, Filter.CONFIGURABLE)
) : Parcelable {

    @JsonClass(generateAdapter = false)
    enum class Filter(
        @StringRes val labelRes: Int
    ) {
        GRANTED(R.string.filter_granted_label),
        DENIED(R.string.filter_denied_label),
        CONFIGURABLE(R.string.filter_configurable_label);

        fun matches(usesPerm: UsesPermission, basePerm: BasePermission): Boolean = when (this) {
            GRANTED -> usesPerm.status == UsesPermission.Status.GRANTED ||
                    usesPerm.status == UsesPermission.Status.GRANTED_IN_USE

            DENIED -> usesPerm.status == UsesPermission.Status.DENIED
            CONFIGURABLE -> basePerm.tags.contains(RuntimeGrant) || basePerm.tags.contains(SpecialAccess)
        }
    }
}
