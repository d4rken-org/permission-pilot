package eu.darken.myperm.apps.core.features

import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.types.BasePermission

interface ApkPkg : Pkg {

    val packageInfo: PackageInfo

    val sharedUserId: String?

    fun getPermission(id: Permission.Id): UsesPermission?

    val requestedPermissions: Collection<UsesPermission>
    fun requestsPermission(id: Permission.Id): Boolean

    val declaredPermissions: Collection<PermissionInfo>
    fun declaresPermission(id: Permission.Id): Boolean

}

fun ApkPkg.requestsPermission(permission: BasePermission) = requestsPermission(permission.id)