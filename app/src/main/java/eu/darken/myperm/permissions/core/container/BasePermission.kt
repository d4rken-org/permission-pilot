package eu.darken.myperm.permissions.core.container

import eu.darken.myperm.permissions.core.Permission

sealed class BasePermission : Permission {

    abstract val requestingApps: List<PermissionAppRef>
    abstract val grantingApps: List<PermissionAppRef>
    abstract val declaringApps: List<PermissionAppRef>

}
