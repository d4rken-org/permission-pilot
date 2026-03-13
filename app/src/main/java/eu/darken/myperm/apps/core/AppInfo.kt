package eu.darken.myperm.apps.core

import eu.darken.myperm.apps.core.features.UsesPermission
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
    val internetAccess: String,
    val batteryOptimization: String,
    val installedAt: Instant?,
    val updatedAt: Instant?,
    val requestedPermissions: List<PermissionUse>,
    val declaredPermissionCount: Int,
    val pkgType: String,
    val twinCount: Int,
    val siblingCount: Int,
    val hasAccessibilityServices: Boolean,
    val allInstallerPkgNames: List<String>,
    val sharedUserId: String? = null,
)

data class PermissionUse(
    val permissionId: String,
    val status: UsesPermission.Status,
)
