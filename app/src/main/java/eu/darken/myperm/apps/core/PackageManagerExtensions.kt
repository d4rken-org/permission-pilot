package eu.darken.myperm.apps.core

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.hasApiLevel
import eu.darken.myperm.permissions.core.Permission
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

fun PackageManager.getPackageInfo2(
    packageName: String,
    flags: Int = 0
): PackageInfo? = try {
    getPackageInfo(packageName, flags)
} catch (_: PackageManager.NameNotFoundException) {
    null
}

fun PackageManager.getLabel2(
    pkgId: Pkg.Id,
): String? = getPackageInfo2(pkgId.pkgName)
    ?.applicationInfo
    ?.let {
        if (it.labelRes != 0) it.loadLabel(this).toString()
        else it.nonLocalizedLabel?.toString()
    }

fun PackageManager.getIcon2(
    pkgId: Pkg.Id,
): Drawable? = getPackageInfo2(pkgId.pkgName)
    ?.applicationInfo
    ?.let { if (it.icon != 0) it.loadIcon(this) else null }

fun PackageManager.getPermissionInfo2(
    permissionId: Permission.Id,
    flags: Int = 0
): PermissionInfo? = try {
    getPermissionInfo(permissionId.value, flags)
} catch (e: PackageManager.NameNotFoundException) {
    null
}

val GET_UNINSTALLED_PACKAGES_COMPAT: Int
    get() = when {
        hasApiLevel(Build.VERSION_CODES.N) -> PackageManager.MATCH_UNINSTALLED_PACKAGES
        else -> PackageManager.GET_UNINSTALLED_PACKAGES
    }

fun UserManager.tryCreateUserHandle(handleId: Int): UserHandle? = try {
    UserHandle::class.constructors
        .first {
            val type = it.parameters.first().type.jvmErasure
            Int::class.isSubclassOf(type)
        }
        .call(handleId)
} catch (e: Exception) {
    log(WARN) { "tryCreateUserHandle($handleId) failed: ${e.asLog()}" }
    null
}