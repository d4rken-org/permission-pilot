package eu.darken.myperm.apps.core

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

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
