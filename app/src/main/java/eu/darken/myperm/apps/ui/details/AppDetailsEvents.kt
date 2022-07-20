package eu.darken.myperm.apps.ui.details

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.permissions.core.PermissionAction

sealed class AppDetailsEvents {
    data class ShowAppSystemDetails(val pkg: Pkg) : AppDetailsEvents()
    data class RunPermAction(val permAction: PermissionAction) : AppDetailsEvents()
}
