package eu.darken.myperm.common.pks

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import eu.darken.myperm.apps.core.types.Pkg

fun PackageManager.getPackageInfo2(
    packageName: String,
    flags: Int = 0
): PackageInfo? = try {
    getPackageInfo(packageName, flags)
} catch (_: PackageManager.NameNotFoundException) {
    null
}

fun PackageManager.getLabel(
    pkgId: Pkg.Id,
): String? = getPackageInfo2(pkgId.value)
    ?.applicationInfo
    ?.loadLabel(this)?.toString()
    ?.takeIf { it != pkgId.value }
