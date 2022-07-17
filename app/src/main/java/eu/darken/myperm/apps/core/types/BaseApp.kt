package eu.darken.myperm.apps.core.types

import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import eu.darken.myperm.apps.core.InternetAccess
import eu.darken.myperm.apps.core.UsesPermission
import eu.darken.myperm.apps.core.installer.InstallerInfo
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.types.BasePermission
import java.time.Instant

sealed class BaseApp : Pkg {

    abstract val label: String?
    abstract val packageInfo: PackageInfo
    abstract val isSystemApp: Boolean

    abstract val installedAt: Instant
    abstract val updatedAt: Instant

    abstract val requestedPermissions: Collection<UsesPermission>
    abstract fun requestsPermission(id: Permission.Id): Boolean

    abstract val declaredPermissions: Collection<PermissionInfo>
    abstract fun declaresPermission(id: Permission.Id): Boolean

    abstract fun getPermission(id: Permission.Id): UsesPermission?

    abstract val internetAccess: InternetAccess
    abstract val installerInfo: InstallerInfo

    abstract val sharedUserId: String?

}

fun BaseApp.requestsPermission(permission: BasePermission) = requestsPermission(permission.id)