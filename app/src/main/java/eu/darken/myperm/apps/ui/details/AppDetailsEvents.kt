package eu.darken.myperm.apps.ui.details

import eu.darken.myperm.apps.core.Pkg

sealed class AppDetailsEvents {
    data class ShowAppSystemDetails(
        val pkg: Pkg
    ) : AppDetailsEvents()
}
