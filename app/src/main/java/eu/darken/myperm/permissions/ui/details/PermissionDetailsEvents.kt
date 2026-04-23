package eu.darken.myperm.permissions.ui.details

import eu.darken.myperm.apps.core.Pkg

sealed class PermissionDetailsEvents {
    data class ShowAppSystemDetails(val pkg: Pkg) : PermissionDetailsEvents()
    data class ShowFilterDialog(val options: PermissionDetailsFilterOptions) : PermissionDetailsEvents()
}
