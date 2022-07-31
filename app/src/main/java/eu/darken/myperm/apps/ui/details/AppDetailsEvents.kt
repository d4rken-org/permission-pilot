package eu.darken.myperm.apps.ui.details

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.permissions.core.features.PermissionAction

sealed class AppDetailsEvents {
    data class ShowAppSystemDetails(val pkg: Pkg) : AppDetailsEvents()
    data class PermissionEvent(val permAction: PermissionAction) : AppDetailsEvents()
}
