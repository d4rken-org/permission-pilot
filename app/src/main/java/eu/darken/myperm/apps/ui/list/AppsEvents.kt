package eu.darken.myperm.apps.ui.list

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionAction

sealed class AppsEvents {
    data class ShowFilterDialog(val options: FilterOptions) : AppsEvents()

    data class ShowSortDialog(val options: SortOptions) : AppsEvents()

    data class ShowPermissionSnackbar(val permission: Permission) : AppsEvents()

    data class ShowAppSystemDetails(val pkg: Pkg) : AppsEvents()

    data class RunPermAction(val permAction: PermissionAction) : AppsEvents()
}
