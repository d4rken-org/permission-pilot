package eu.darken.myperm.apps.core.features

import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.hasApiLevel
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.types.BasePermission

// A Pkg where we have access to an APK
interface HasApkData : Pkg {

    val packageInfo: PackageInfo

    val versionName: String?
        get() = packageInfo.versionName

    @Suppress("DEPRECATION")
    val versionCode: Long
        get() = if (hasApiLevel(28)) packageInfo.longVersionCode else packageInfo.versionCode.toLong()

    val sharedUserId: String?
        get() = packageInfo.sharedUserId

    fun getPermission(id: Permission.Id): UsesPermission?

    val requestedPermissions: Collection<UsesPermission>
    fun requestsPermission(id: Permission.Id): Boolean

    val declaredPermissions: Collection<PermissionInfo>
    fun declaresPermission(id: Permission.Id): Boolean

}

fun HasApkData.requestsPermission(permission: BasePermission) = requestsPermission(permission.id)