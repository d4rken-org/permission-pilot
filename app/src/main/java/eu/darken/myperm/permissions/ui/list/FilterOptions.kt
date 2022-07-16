package eu.darken.myperm.permissions.ui.list

import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.types.BasePermission

data class FilterOptions(
    val keys: Set<Filter> = setOf(Filter.CORE)
) {
    enum class Filter(
        @StringRes val labelRes: Int,
        val matches: (BasePermission) -> Boolean
    ) {
        CORE(
            labelRes = R.string.permissions_filter_system_core_label,
            matches = { it.declaringPkgs.any { pkg -> pkg.id == eu.darken.myperm.apps.core.AndroidPkgs.ANDROID.id } }
        ),
        SYSTEM(
            labelRes = R.string.permissions_filter_system_extra_label,
            matches = { it.declaringPkgs.any { it.isSystemApp } }
        ),
        USER(
            labelRes = R.string.permissions_filter_custom_label,
            matches = { it.declaringPkgs.none { it.isSystemApp } }
        ),
        ;
    }
}