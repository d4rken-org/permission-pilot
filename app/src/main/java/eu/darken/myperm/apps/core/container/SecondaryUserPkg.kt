package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.*
import eu.darken.myperm.apps.core.features.*
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.permissions.core.Permission

data class SecondaryUserPkg(
    override val packageInfo: PackageInfo,
    override val installerInfo: InstallerInfo,
    override val userHandle: UserHandle,
) : BasePkg(), SecondaryPkg {

    override val id: Pkg.Id = Pkg.Id(packageInfo.packageName, userHandle)

    private var _label: String? = null
    override fun getLabel(context: Context): String {
        _label?.let { return it }
        val newLabel = context.packageManager.getLabel2(id)
            ?: twins.firstNotNullOfOrNull { it.getLabel(context) }
            ?: super.getLabel(context)
            ?: id.pkgName
        _label = newLabel
        return newLabel
    }

    override fun getIcon(context: Context): Drawable =
        context.packageManager.getIcon2(id)
            ?: twins.firstNotNullOfOrNull { it.getIcon(context) }
            ?: super.getIcon(context)
            ?: context.getDrawable(R.drawable.ic_default_app_icon_24)!!

    override var siblings: Collection<Pkg> = emptyList()
    override var twins: Collection<Installed> = emptyList()

    override val requestedPermissions: Collection<UsedPermissionStateful> by lazy {
        packageInfo.requestedPermissions?.mapIndexed { _, permissionId ->
            UsedPermissionStateful(
                id = Permission.Id(permissionId),
                flags = null,  // We don't know for secondary profiles
            )
        } ?: emptyList()
    }

    override val declaredPermissions: Collection<PermissionInfo> by lazy {
        packageInfo.permissions?.toSet() ?: emptyList()
    }

    override val internetAccess: InternetAccess = InternetAccess.UNKNOWN
}

fun Context.getSecondaryUserPkgs(): Collection<BasePkg> {
    log(AppRepo.TAG) { "getSecondaryPkgs()" }

    val normal = packageManager.getInstalledPackages(0).map { it.packageName }
    val uninstalled = packageManager.getInstalledPackages(
        PackageManager.GET_PERMISSIONS or packageManager.GET_UNINSTALLED_PACKAGES_COMPAT
    )
    val newOnes = uninstalled.filter { !normal.contains(it.packageName) }

    val userManager = ContextCompat.getSystemService(this, UserManager::class.java)!!

    return newOnes.map { pkg ->
        SecondaryUserPkg(
            packageInfo = pkg,
            installerInfo = pkg.getInstallerInfo(packageManager),
            userHandle = userManager.tryCreateUserHandle(11) ?: Process.myUserHandle(),
        ).also { log(AppRepo.TAG) { "PKG[secondary]: $it" } }
    }
}
