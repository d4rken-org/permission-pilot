package eu.darken.myperm.permissions.ui.details

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.permissions.core.features.PermissionAction

sealed class PermissionDetailsEvents {
    data class ShowAppSystemDetails(val pkg: Pkg) : PermissionDetailsEvents()
    data class PermissionEvent(val permAction: PermissionAction) : PermissionDetailsEvents()
    data class ShowFilterDialog(val options: PermissionDetailsFilterOptions) : PermissionDetailsEvents()
}
