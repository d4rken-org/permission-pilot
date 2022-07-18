package eu.darken.myperm.apps.core.features

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.apps.core.known.toKnownPkg
import eu.darken.myperm.apps.core.toContainer
import eu.darken.myperm.apps.core.tryIcon
import eu.darken.myperm.common.HasLabel
import eu.darken.myperm.common.hasApiLevel

data class InstallerInfo(
    val installingPkg: Pkg?,
    val initiatingPkg: Pkg? = null,
    val originatingPkg: Pkg? = null,
) {

    val allInstallers: List<Pkg>
        get() = listOfNotNull(installingPkg, initiatingPkg, originatingPkg)

    val installer: Pkg?
        get() = allInstallers.firstOrNull()

    fun getLabel(context: Context): String {
        if (installer == null) {
            return context.getString(R.string.apps_details_installer_manual_label)
        }

        return (installingPkg as? HasLabel)?.getLabel(context) ?: installer!!.id.value
    }

    fun getIcon(context: Context): Drawable {
        if (installer == null) return ContextCompat.getDrawable(context, R.drawable.ic_baseline_user_24)!!

        installer!!.tryIcon(context)?.let { return it }

        return ContextCompat.getDrawable(context, R.drawable.ic_default_app_icon_24)!!
    }
}

fun InstalledApp.isSideloaded(): Boolean {
    if (isSystemApp) return false
    return installerInfo.allInstallers.none { it.id == AKnownPkg.GooglePlay.id }
}

fun PackageInfo.getInstallerInfo(
    packageManager: PackageManager,
): InstallerInfo = if (hasApiLevel(Build.VERSION_CODES.R)) {
    getInstallerInfoApi30(packageManager)
} else {
    getInstallerInfoLegacy(packageManager)
}

private fun PackageInfo.getInstallerInfoApi30(packageManager: PackageManager): InstallerInfo {
    val installingPkg = packageManager.getInstallerPackageName(packageName)
        ?.let { Pkg.Id(it) }
        ?.let { it.toKnownPkg() ?: it.toContainer() }

    return InstallerInfo(
        installingPkg = installingPkg,
    )
}

private fun PackageInfo.getInstallerInfoLegacy(packageManager: PackageManager): InstallerInfo {
    val sourceInfo = try {
        packageManager.getInstallSourceInfo(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
    val initiatingPkg = sourceInfo?.initiatingPackageName
        ?.let { Pkg.Id(it) }
        ?.let { it.toKnownPkg() ?: it.toContainer() }

    val installingPkg = sourceInfo?.installingPackageName
        ?.let { Pkg.Id(it) }
        ?.let { it.toKnownPkg() ?: it.toContainer() }

    val originatingPkg = sourceInfo?.originatingPackageName
        ?.let { Pkg.Id(it) }
        ?.let { it.toKnownPkg() ?: it.toContainer() }

    return InstallerInfo(
        initiatingPkg = initiatingPkg,
        installingPkg = installingPkg,
        originatingPkg = originatingPkg,
    )
}