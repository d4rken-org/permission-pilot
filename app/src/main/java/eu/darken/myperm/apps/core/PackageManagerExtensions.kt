package eu.darken.myperm.apps.core

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import eu.darken.myperm.permissions.core.Permission

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
): String? = getPackageInfo2(pkgId.value)
    ?.applicationInfo
    ?.let { if (it.labelRes != 0) it.loadLabel(this).toString() else null }

fun PackageManager.getIcon2(
    pkgId: Pkg.Id,
): Drawable? = getPackageInfo2(pkgId.value)
    ?.applicationInfo
    ?.let { if (it.icon != 0) it.loadIcon(this) else null }

val PackageInfo.pkgId
    get() = Pkg.Id(packageName)

fun PackageManager.getPermissionInfo2(
    permissionId: Permission.Id,
    flags: Int = 0
): PermissionInfo? = try {
    getPermissionInfo(permissionId.value, flags)
} catch (e: PackageManager.NameNotFoundException) {
    null
}