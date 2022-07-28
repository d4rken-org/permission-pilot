package eu.darken.myperm.permissions.core.container

import eu.darken.myperm.permissions.core.Permission

data class UsesPermission(
    override val id: Permission.Id,
) : Permission