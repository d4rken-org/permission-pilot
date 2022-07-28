package eu.darken.myperm.apps.core.features

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.container.UsedPermissionStateful

// A Pkg where we have access to an APK
interface HasPermissionUseInfo {

    val requestedPermissions: Collection<UsedPermissionStateful>
}

fun Pkg.getPermissionUses(id: Permission.Id): UsedPermissionStateful? =
    (this as? HasPermissionUseInfo)?.requestedPermissions?.singleOrNull { it.id == id }