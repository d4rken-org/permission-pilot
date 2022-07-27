package eu.darken.myperm.apps.core.features

import android.content.pm.PermissionInfo
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.container.BasePermission

// A Pkg where we have access to an APK
interface HasPermissions : ReadableApk {

    fun getPermission(id: Permission.Id): UsesPermission?

    val requestedPermissions: Collection<UsesPermission>
    fun requestsPermission(id: Permission.Id): Boolean

    val declaredPermissions: Collection<PermissionInfo>
    fun declaresPermission(id: Permission.Id): Boolean

}

fun HasPermissions.requestsPermission(permission: BasePermission) = requestsPermission(permission.id)