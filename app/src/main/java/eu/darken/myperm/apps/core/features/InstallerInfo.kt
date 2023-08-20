package eu.darken.myperm.apps.core.features

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.apps.core.known.toKnownPkg
import eu.darken.myperm.apps.core.toContainer
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
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
            return context.getString(R.string.apps_details_installer_unknown_label)
        }

        return installingPkg?.getLabel(context) ?: installer!!.id.pkgName
    }

    fun getIcon(context: Context): Drawable {
        if (installer == null) return ContextCompat.getDrawable(context, R.drawable.ic_baseline_user_24)!!

        installer!!.getIcon(context)?.let { return it }

        return ContextCompat.getDrawable(context, R.drawable.ic_default_app_icon_24)!!
    }
}

fun Installed.isSideloaded(): Boolean {
    if (isSystemApp) return false
    return installerInfo.allInstallers.none { it.id == AKnownPkg.GooglePlay.id }
}

suspend fun PackageInfo.getInstallerInfo(
    ipcFunnel: IPCFunnel,
): InstallerInfo = if (hasApiLevel(Build.VERSION_CODES.R)) {
    @Suppress("NewApi")
    getInstallerInfoApi30(ipcFunnel)
} else {
    getInstallerInfoLegacy(ipcFunnel)
}

@RequiresApi(Build.VERSION_CODES.R)
private suspend fun PackageInfo.getInstallerInfoApi30(ipcFunnel: IPCFunnel): InstallerInfo {
    val sourceInfo = try {
        ipcFunnel.packageManager.getInstallSourceInfo(packageName)
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

private suspend fun PackageInfo.getInstallerInfoLegacy(ipcFunnel: IPCFunnel): InstallerInfo {
    val installingPkg = try {
        ipcFunnel.packageManager.getInstallerPackageName(packageName)
            ?.let { Pkg.Id(it) }
            ?.let { it.toKnownPkg() ?: it.toContainer() }
    } catch (e: IllegalArgumentException) {
        log(WARN) { "OS race condition, package ($packageName) was uninstalled?: ${e.asLog()}" }
        null
    }

    return InstallerInfo(
        installingPkg = installingPkg,
    )
}