package eu.darken.myperm.permissions.ui.list

import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.types.BasePermission

data class SortOptions(
    val mainSort: Sort = Sort.APPS_GRANTED,
    val reversed: Boolean = false
) {
    enum class Sort(
        @StringRes val labelRes: Int,
        val comparator: Comparator<BasePermission>
    ) {
        APPS_GRANTED(
            labelRes = R.string.permissions_sort_apps_granted_label,
            comparator = Comparator.comparing<BasePermission, Int> { perm ->
                perm.grantingPkgs.size
            }.reversed()
        ),
        APPS_REQUESTED(
            labelRes = R.string.permissions_sort_apps_requested_label,
            comparator = Comparator.comparing<BasePermission, Int> { perm ->
                perm.requestingPkgs.size
            }.reversed()
        ),

        ;
    }
}