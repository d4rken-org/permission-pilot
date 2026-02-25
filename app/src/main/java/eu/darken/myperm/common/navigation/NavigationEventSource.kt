package eu.darken.myperm.common.navigation

import eu.darken.myperm.common.flow.SingleEventFlow

interface NavigationEventSource {
    val navEvents: SingleEventFlow<NavEvent>
}
