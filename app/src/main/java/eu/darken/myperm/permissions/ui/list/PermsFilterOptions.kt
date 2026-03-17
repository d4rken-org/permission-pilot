package eu.darken.myperm.permissions.ui.list

import android.os.Parcelable
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.features.InstallTimeGrant
import eu.darken.myperm.permissions.core.features.ManifestDoc
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PermsFilterOptions(
    val filters: Set<Filter> = setOf(
        Filter.MANIFEST,
        Filter.SYSTEM,
        Filter.NOT_INSTALLTIME,
    )
) : Parcelable {

    enum class Group(@StringRes val labelRes: Int) {
        SOURCE(R.string.permissions_filter_section_source),
        TYPE(R.string.permissions_filter_section_type),
    }

    @Serializable
    enum class Filter(
        val group: Group,
        @StringRes val labelRes: Int,
        val matches: (BasePermission) -> Boolean,
    ) {
        @SerialName("MANIFEST")
        MANIFEST(
            group = Group.SOURCE,
            labelRes = R.string.permissions_filter_manifest_label,
            matches = { it.tags.any { tag -> tag is ManifestDoc } },
        ),
        @SerialName("SYSTEM")
        SYSTEM(
            group = Group.SOURCE,
            labelRes = R.string.permissions_filter_system_label,
            matches = { pkg -> pkg.declaringApps.any { it.isSystemApp } },
        ),
        @SerialName("USER")
        USER(
            group = Group.SOURCE,
            labelRes = R.string.permissions_filter_user_label,
            matches = { pkg -> pkg.declaringApps.none { it.isSystemApp } },
        ),

        @SerialName("RUNTIME")
        RUNTIME(
            group = Group.TYPE,
            labelRes = R.string.permissions_filter_runtime_label,
            matches = { it.tags.any { tag -> tag is RuntimeGrant } },
        ),
        @SerialName("INSTALLTIME")
        INSTALLTIME(
            group = Group.TYPE,
            labelRes = R.string.permissions_filter_installtime_label,
            matches = { it.tags.any { tag -> tag is InstallTimeGrant } },
        ),
        @SerialName("NOT_INSTALLTIME")
        NOT_INSTALLTIME(
            group = Group.TYPE,
            labelRes = R.string.permissions_filter_installtime_hide_label,
            matches = { it.tags.none { tag -> tag is InstallTimeGrant } },
        ),
        @SerialName("SPECIAL_ACCESS")
        SPECIAL_ACCESS(
            group = Group.TYPE,
            labelRes = R.string.permissions_filter_special_label,
            matches = { it.tags.any { tag -> tag is SpecialAccess } },
        ),
        ;
    }

    fun matches(perm: BasePermission): Boolean {
        if (filters.isEmpty()) return true
        return filters.groupBy { it.group }.all { (_, groupFilters) ->
            groupFilters.any { it.matches(perm) }
        }
    }
}
