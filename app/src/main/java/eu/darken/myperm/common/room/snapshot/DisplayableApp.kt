package eu.darken.myperm.common.room.snapshot

import eu.darken.myperm.apps.core.features.UsesPermission
import java.time.Instant

interface DisplayableApp {
    val pkgName: String
    val userHandleId: Int
    val label: String
    val versionName: String?
    val versionCode: Long
    val isSystemApp: Boolean
    val installerPkgName: String?
    val apiTargetLevel: Int?
    val apiCompileLevel: Int?
    val apiMinimumLevel: Int?
    val internetAccess: String
    val batteryOptimization: String
    val installedAt: Instant?
    val updatedAt: Instant?
    val requestedPermissions: List<DisplayablePermissionUse>
    val declaredPermissionCount: Int
    val pkgType: String
    val twinCount: Int
    val siblingCount: Int
    val hasAccessibilityServices: Boolean
    val allInstallerPkgNames: List<String>
}

interface DisplayablePermissionUse {
    val permissionId: String
    val status: UsesPermission.Status
}
