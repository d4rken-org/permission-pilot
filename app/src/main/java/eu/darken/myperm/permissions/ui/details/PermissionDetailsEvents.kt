package eu.darken.myperm.permissions.ui.details

import eu.darken.myperm.apps.core.Pkg

sealed class PermissionDetailsEvents {
    data class ShowAppSystemDetails(val pkg: Pkg) : PermissionDetailsEvents()
}
