package eu.darken.myperm.permissions.core.types

import eu.darken.myperm.apps.core.types.BaseApp

sealed class BasePermission {

    abstract val id: String
    abstract val requestingApps: Collection<BaseApp>
    abstract val grantedApps: Collection<BaseApp>
}