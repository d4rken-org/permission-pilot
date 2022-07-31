package eu.darken.myperm.apps.core.features

import eu.darken.myperm.permissions.core.Permission

data class UsesPermissionStateless(
    override val id: Permission.Id,
) : UsesPermission