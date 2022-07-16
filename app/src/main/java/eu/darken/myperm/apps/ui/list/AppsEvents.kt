package eu.darken.myperm.apps.ui.list

import eu.darken.myperm.permissions.core.Permission

sealed class AppsEvents {
    data class ShowFilterDialog(
        val options: FilterOptions
    ) : AppsEvents()

    data class ShowSortDialog(
        val options: SortOptions
    ) : AppsEvents()

    data class ShowPermissionSnackbar(
        val permission: Permission
    ) : AppsEvents()
}
