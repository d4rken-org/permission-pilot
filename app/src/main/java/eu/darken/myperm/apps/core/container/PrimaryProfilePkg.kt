package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.AccessibilityService
import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.DeviceAdmin
import eu.darken.myperm.apps.core.features.Installed
import eu.darken.myperm.apps.core.features.InstallerInfo
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.features.determineAccessibilityServices
import eu.darken.myperm.apps.core.features.determineBatteryOptimization
import eu.darken.myperm.apps.core.features.determineDeviceAdmins
import eu.darken.myperm.apps.core.features.determineSpecialPermissions
import eu.darken.myperm.apps.core.features.getInstallerInfo
import eu.darken.myperm.apps.core.features.getSpecialPermissionStatuses
import eu.darken.myperm.apps.core.features.getPermissionUses
import eu.darken.myperm.apps.core.features.isGranted
import eu.darken.myperm.apps.core.getIcon2
import eu.darken.myperm.apps.core.getLabel2
import eu.darken.myperm.apps.core.isSystemApp
import eu.darken.myperm.common.IPCFunnel
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
    override val deviceAdmins: Collection<DeviceAdmin>,
    private val specialPermissionStatuses: Map<Permission.Id, UsesPermission.Status> = emptyMap(),
) : BasePkg() {

    override val id: Pkg.Id = Pkg.Id(Pkg.Name(packageInfo.packageName), userHandle)

    private var _label: String? = null
    private var _resolvingLabel = false
    override fun getLabel(context: Context): String {
        _label?.let { return it }
        if (_resolvingLabel) return id.pkgName.value
        _resolvingLabel = true
        try {
            val newLabel = context.packageManager.getLabel2(id)
                ?: twins.firstNotNullOfOrNull { it.getLabel(context) }
                ?: super.getLabel(context)
                ?: id.pkgName.value
            _label = newLabel
            return newLabel
        } finally {
            _resolvingLabel = false
        }
    }

    private var _resolvingIcon = false
    override fun getIcon(context: Context): Drawable? {
        if (_resolvingIcon) return null
        _resolvingIcon = true
        return try {
            context.packageManager.getIcon2(id)
                ?: twins.firstNotNullOfOrNull { it.getIcon(context) }
                ?: super.getIcon(context)
        } finally {
            _resolvingIcon = false
        }
    }

    override val isSystemApp: Boolean = applicationInfo?.isSystemApp ?: true

    override var siblings: Collection<Pkg> = emptyList()
    override var twins: Collection<Installed> = emptyList()

    override val requestedPermissions: Collection<UsesPermission> by lazy {
        val base = packageInfo.requestedPermissions?.mapIndexed { index, permissionId ->
            val flags = packageInfo.requestedPermissionsFlags?.get(index) ?: 0
            val permId = Permission.Id(permissionId)
            val overrideStatus = specialPermissionStatuses[permId]
            UsesPermission.WithState(id = permId, flags = flags, overrideStatus = overrideStatus)
        } ?: emptyList()

        val acsPermissions = accessibilityServices.map {
            UsesPermission.WithState(
                id = APerm.BIND_ACCESSIBILITY_SERVICE.id,
                flags = if (it.isEnabled) PackageInfo.REQUESTED_PERMISSION_GRANTED else 0
            )
        }
        val deviceAdminPermissions = deviceAdmins.map {
            UsesPermission.WithState(
                id = APerm.BIND_DEVICE_ADMIN.id,
                flags = if (it.isActive) PackageInfo.REQUESTED_PERMISSION_GRANTED else 0
            )
        }
        base + extraPermissions + acsPermissions + deviceAdminPermissions
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

private suspend fun PackageInfo.toNormalPkg(
    ipcFunnel: IPCFunnel,
    activeAdminPkgs: Set<String>,
): PrimaryProfilePkg = PrimaryProfilePkg(
    packageInfo = this,
    installerInfo = getInstallerInfo(ipcFunnel),
    extraPermissions = determineSpecialPermissions(ipcFunnel),
    batteryOptimization = determineBatteryOptimization(ipcFunnel),
    accessibilityServices = determineAccessibilityServices(ipcFunnel),
    deviceAdmins = determineDeviceAdmins(ipcFunnel, activeAdminPkgs),
    specialPermissionStatuses = getSpecialPermissionStatuses(ipcFunnel),
)

suspend fun getNormalPkgs(ipcFunnel: IPCFunnel): Collection<BasePkg> {
    log(AppRepo.TAG) { "getNormalPkgs()" }

    val activeAdminPkgs = ipcFunnel.devicePolicyManager.getActiveAdmins()
        ?.map { it.packageName }?.toSet() ?: emptySet()

    return coroutineScope {
        ipcFunnel.packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            .map { async { it.toNormalPkg(ipcFunnel, activeAdminPkgs) } }
            .awaitAll()
    }
}