package eu.darken.myperm.permissions.ui.list

import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.types.BasePermission

data class FilterOptions(
    val keys: Set<Filter> = setOf(Filter.AOSP_PERMISSION)
) {
    enum class Filter(
        @StringRes val labelRes: Int,
        val matches: (BasePermission) -> Boolean
    ) {
        AOSP_PERMISSION(
            labelRes = R.string.permissions_filter_aosp_label,
            matches = { it.isAospPermission }
        ),
        CUSTOM_PERMISSION(
            labelRes = R.string.permissions_filter_custom_label,
            matches = { !it.isAospPermission }
        ),
        ;
    }
}