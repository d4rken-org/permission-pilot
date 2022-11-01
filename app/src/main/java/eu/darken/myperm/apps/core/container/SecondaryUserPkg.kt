package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.*
import eu.darken.myperm.apps.core.features.*
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.APerm
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SecondaryUserPkg(
    override val packageInfo: PackageInfo,
    override val installerInfo: InstallerInfo,
    override val userHandle: UserHandle,
    val extraPermissions: Collection<UsesPermission>,
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

    override val requestedPermissions: Collection<UsesPermission> by lazy {
        val base = packageInfo.requestedPermissions?.mapIndexed { _, permissionId ->
            UsesPermission.Unknown(id = Permission.Id(permissionId))
        } ?: emptyList()
        val acsPermissions = accessibilityServices.map {
            UsesPermission.WithState(
                id = APerm.BIND_ACCESSIBILITY_SERVICE.id,
                flags = null
            )
        }
        base + acsPermissions
    }

    override val declaredPermissions: Collection<PermissionInfo> by lazy {
        packageInfo.permissions?.toSet() ?: emptyList()
    }

    override val internetAccess: InternetAccess = InternetAccess.UNKNOWN

    override val isSystemApp: Boolean = super.isSystemApp || twins.any { it.isSystemApp }

    override fun toString(): String = "SecondaryUserPkg(packageName=$packageName, userHandle=$userHandle)"
}

suspend fun getSecondaryUserPkgs(ipcFunnel: IPCFunnel): Collection<BasePkg> = coroutineScope {
    log(AppRepo.TAG) { "getSecondaryPkgs()" }

    val normal = ipcFunnel.packageManager.getInstalledPackages(0).map { it.packageName }
    val uninstalled = ipcFunnel.packageManager.getInstalledPackages(
        PackageManager.GET_PERMISSIONS or GET_UNINSTALLED_PACKAGES_COMPAT
    )
    val newOnes = uninstalled.filter { !normal.contains(it.packageName) }

    newOnes
        .map { pkg ->
            async {
                SecondaryUserPkg(
                    packageInfo = pkg,
                    installerInfo = pkg.getInstallerInfo(ipcFunnel),
                    userHandle = ipcFunnel.userManager.tryCreateUserHandle(11) ?: Process.myUserHandle(),
                    extraPermissions = pkg.determineSpecialPermissions(ipcFunnel),
                ).also { log(AppRepo.TAG) { "PKG[secondary]: $it" } }
            }
        }
        .awaitAll()
}
