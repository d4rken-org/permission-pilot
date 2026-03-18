package eu.darken.myperm.apps.core

import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.room.entity.PkgType
import java.time.Instant

data class AppInfo(
    val pkgName: String,
    val userHandleId: Int,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val installerPkgName: String?,
    val apiTargetLevel: Int?,
    val apiCompileLevel: Int?,
    val apiMinimumLevel: Int?,
    val internetAccess: InternetAccess,
    val batteryOptimization: BatteryOptimization,
    val installedAt: Instant?,
    val updatedAt: Instant?,
    val requestedPermissions: List<PermissionUse>,
    val declaredPermissionCount: Int,
    val pkgType: PkgType,
    val twinCount: Int,
    val siblingCount: Int,
    val hasAccessibilityServices: Boolean,
    val hasDeviceAdmin: Boolean,
    val allInstallerPkgNames: List<String>,
    val sharedUserId: String? = null,
    val hasManifestFlags: Boolean? = null,
)

data class PermissionUse(
    val permissionId: String,
    val status: UsesPermission.Status,
)
