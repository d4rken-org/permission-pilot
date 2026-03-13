package eu.darken.myperm.apps.ui.details

import android.os.Parcelable
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.PermissionUse
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AppDetailsFilterOptions(
    @SerialName("filters") val keys: Set<Filter> = setOf(Filter.GRANTED, Filter.DENIED, Filter.CONFIGURABLE)
) : Parcelable {

    @Serializable
    enum class Filter(
        @StringRes val labelRes: Int
    ) {
        GRANTED(R.string.filter_granted_label),
        DENIED(R.string.filter_denied_label),
        CONFIGURABLE(R.string.filter_configurable_label);

        fun matches(cachedPerm: PermissionUse, basePerm: BasePermission): Boolean = when (this) {
            GRANTED -> cachedPerm.status == UsesPermission.Status.GRANTED ||
                    cachedPerm.status == UsesPermission.Status.GRANTED_IN_USE

            DENIED -> cachedPerm.status == UsesPermission.Status.DENIED
            CONFIGURABLE -> basePerm.tags.contains(RuntimeGrant) || basePerm.tags.contains(SpecialAccess)
        }
    }
}
