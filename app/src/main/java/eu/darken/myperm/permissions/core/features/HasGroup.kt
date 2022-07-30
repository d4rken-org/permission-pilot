package eu.darken.myperm.permissions.core.features

import eu.darken.myperm.permissions.core.PermissionGroup

interface HasGroup {
    val groupIds: Collection<PermissionGroup.Id>
}