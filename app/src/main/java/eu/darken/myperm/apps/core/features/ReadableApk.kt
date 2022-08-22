package eu.darken.myperm.apps.core.features

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.hasApiLevel
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.known.APerm

// A Pkg where we have access to an APK
interface ReadableApk : Pkg {

    val packageInfo: PackageInfo

    val applicationInfo: ApplicationInfo?
        get() = packageInfo.applicationInfo

    val versionName: String?
        get() = packageInfo.versionName

    val versionCode: Long
        get() = PackageInfoCompat.getLongVersionCode(packageInfo)

    val sharedUserId: String?
        get() = packageInfo.sharedUserId

    val apiTargetLevel: Int?
        get() = applicationInfo?.targetSdkVersion

    val apiCompileLevel: Int?
        get() = if (hasApiLevel(Build.VERSION_CODES.S)) applicationInfo?.compileSdkVersion else null

    val apiMinimumLevel: Int?
        get() = if (hasApiLevel(Build.VERSION_CODES.N)) applicationInfo?.minSdkVersion else null

    val requestedPermissions: Collection<UsesPermission>

    val declaredPermissions: Collection<PermissionInfo>
}

fun Pkg.getPermission(perm: APerm): UsesPermission? =
    (this as? ReadableApk)?.requestedPermissions?.singleOrNull { it.id == perm.id }

fun Pkg.getPermission(id: Permission.Id): UsesPermission? =
    (this as? ReadableApk)?.requestedPermissions?.singleOrNull { it.id == id }

fun Pkg.requestsPermission(id: Permission.Id): Boolean =
    (this as? ReadableApk)?.requestedPermissions?.any { it.id == id } ?: false

fun Pkg.requestsPermission(permission: BasePermission) = requestsPermission(permission.id)

fun Pkg.declaresPermission(id: Permission.Id): Boolean =
    (this as? ReadableApk)?.declaredPermissions?.any { it.name == id.value } ?: false
