package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.*
import eu.darken.myperm.apps.core.features.*
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.permissions.core.AndroidPermissions
import eu.darken.myperm.permissions.core.Permission

data class SecondaryUserPkg(
    override val packageInfo: PackageInfo,
    override val installerInfo: InstallerInfo,
    override val userHandle: UserHandle,
) : BasicPkgContainer, SecondaryPkg {

    override val id: Pkg.Id = Pkg.Id(packageInfo.packageName, userHandle)

    override fun getLabel(context: Context): String =
        context.packageManager.getLabel2(id)
            ?: twins.firstNotNullOfOrNull { it.getLabel(context) }
            ?: super.getLabel(context)
            ?: id.pkgName

    override fun getIcon(context: Context): Drawable =
        context.packageManager.getIcon2(id)
            ?: twins.firstNotNullOfOrNull { it.getIcon(context) }
            ?: super.getIcon(context)
            ?: context.getDrawable(R.drawable.ic_default_app_icon_24)!!

    override val requestedPermissions: Collection<UsesPermission> by lazy {
        packageInfo.requestedPermissions?.mapIndexed { _, permissionId ->
            UsesPermission(
                id = Permission.Id(permissionId),
                flags = null,  // We don't know for secondary profiles
            )
        } ?: emptyList()
    }

    override var siblings: Collection<Pkg> = emptyList()
    override var twins: Collection<HasInstallData> = emptyList()

    override fun requestsPermission(id: Permission.Id): Boolean = requestedPermissions.any { it.id == id }

    override fun getPermission(id: Permission.Id): UsesPermission? {
        return requestedPermissions.singleOrNull { it.id == id }
    }

    override val declaredPermissions: Collection<PermissionInfo> by lazy {
        packageInfo.permissions?.toSet() ?: emptyList()
    }

    override fun declaresPermission(id: Permission.Id): Boolean = declaredPermissions.any { it.name == id.value }

    override val internetAccess: InternetAccess by lazy {
        when {
            isSystemApp || getPermission(AndroidPermissions.INTERNET.id)?.isGranted == true -> InternetAccess.DIRECT
            siblings.any { it is HasApkData && it.getPermission(AndroidPermissions.INTERNET.id)?.isGranted == true } -> InternetAccess.INDIRECT
            else -> InternetAccess.NONE
        }
    }
}

fun Context.getSecondaryPkgs(): Collection<Pkg> {
    log(AppRepo.TAG) { "getSecondaryPkgs()" }

    val normal = packageManager.getInstalledPackages(0).map { it.packageName }
    val uninstalled = packageManager.getInstalledPackages(
        PackageManager.GET_PERMISSIONS or packageManager.GET_UNINSTALLED_PACKAGES_COMPAT
    )
    val newOnes = uninstalled.filter { !normal.contains(it.packageName) }

    val userManager = getSystemService(UserManager::class.java)

    return newOnes.map { pkg ->
        SecondaryUserPkg(
            packageInfo = pkg,
            installerInfo = pkg.getInstallerInfo(packageManager),
            userHandle = userManager.tryCreateUserHandle(11) ?: Process.myUserHandle(),
        ).also { log(AppRepo.TAG) { "PKG[secondary]: $it" } }
    }
}
