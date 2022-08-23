package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.*
import eu.darken.myperm.apps.core.getIcon2
import eu.darken.myperm.apps.core.getLabel2
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.APerm
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class PrimaryProfilePkg(
    override val packageInfo: PackageInfo,
    override val userHandle: UserHandle = Process.myUserHandle(),
    override val installerInfo: InstallerInfo,
    private val extraPermissions: Collection<UsesPermission>,
    override val batteryOptimization: BatteryOptimization,
    override val accessibilityServices: Collection<AccessibilityService>,
) : BasePkg() {

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
        val base = packageInfo.requestedPermissions?.mapIndexed { index, permissionId ->
            val flags = packageInfo.requestedPermissionsFlags[index]
            UsesPermission.WithState(id = Permission.Id(permissionId), flags = flags)
        } ?: emptyList()

        val acsPermissions = accessibilityServices.map {
            UsesPermission.WithState(
                id = APerm.BIND_ACCESSIBILITY_SERVICE.id,
                flags = if (it.isEnabled) PackageInfo.REQUESTED_PERMISSION_GRANTED else 0
            )
        }
        base + extraPermissions + acsPermissions
    }

    override val declaredPermissions: Collection<PermissionInfo> by lazy {
        packageInfo.permissions?.toSet() ?: emptyList()
    }

    override val internetAccess: InternetAccess by lazy {
        when {
            isSystemApp || getPermissionUses(APerm.INTERNET.id).isGranted -> InternetAccess.DIRECT
            siblings.any { it.getPermissionUses(APerm.INTERNET.id).isGranted } -> InternetAccess.INDIRECT
            else -> InternetAccess.NONE
        }
    }

    override fun toString(): String = "PrimaryProfilePkg(packageName=$packageName, userHandle=$userHandle)"
}

private fun PackageInfo.toNormalPkg(context: Context): PrimaryProfilePkg = PrimaryProfilePkg(
    packageInfo = this,
    installerInfo = getInstallerInfo(context.packageManager),
    extraPermissions = determineSpecialPermissions(context),
    batteryOptimization = determineBatteryOptimization(context),
    accessibilityServices = determineAccessibilityServices(context),
)

suspend fun getNormalPkgs(context: Context): Collection<BasePkg> {
    log(AppRepo.TAG) { "getNormalPkgs()" }

    return coroutineScope {
        context.packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            .map { async { it.toNormalPkg(context) } }
            .awaitAll()
    }
}