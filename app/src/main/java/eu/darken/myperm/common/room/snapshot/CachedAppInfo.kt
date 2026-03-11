package eu.darken.myperm.common.room.snapshot

import eu.darken.myperm.apps.core.features.UsesPermission
import java.time.Instant

data class CachedAppInfo(
    override val pkgName: String,
    override val userHandleId: Int,
    override val label: String,
    override val versionName: String?,
    override val versionCode: Long,
    override val isSystemApp: Boolean,
    override val installerPkgName: String?,
    override val apiTargetLevel: Int?,
    override val apiCompileLevel: Int?,
    override val apiMinimumLevel: Int?,
    override val internetAccess: String,
    override val batteryOptimization: String,
    override val installedAt: Instant?,
    override val updatedAt: Instant?,
    override val requestedPermissions: List<CachedPermissionUse>,
    override val declaredPermissionCount: Int,
    override val pkgType: String,
    override val twinCount: Int,
    override val siblingCount: Int,
    override val hasAccessibilityServices: Boolean,
    override val allInstallerPkgNames: List<String>,
) : DisplayableApp

data class CachedPermissionUse(
    override val permissionId: String,
    override val status: UsesPermission.Status,
) : DisplayablePermissionUse
