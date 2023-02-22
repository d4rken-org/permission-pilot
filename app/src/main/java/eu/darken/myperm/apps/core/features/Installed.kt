package eu.darken.myperm.apps.core.features

import android.content.pm.PackageInfo
import android.os.UserHandle
import eu.darken.myperm.apps.core.Pkg
import java.time.Instant

interface Installed : Pkg {
    val packageInfo: PackageInfo
    val userHandle: UserHandle

    // Weird overflow when using default interface impl here?
    // https://github.com/d4rken-org/permission-pilot/issues/173
    val isSystemApp: Boolean

    val installedAt: Instant?
        get() = packageInfo.firstInstallTime.takeIf { it != 0L }?.let { Instant.ofEpochMilli(it) }

    val updatedAt: Instant?
        get() = packageInfo.lastUpdateTime.takeIf { it != 0L }?.let { Instant.ofEpochMilli(it) }

    // Same user id
    val siblings: Collection<Pkg>

    // Extra user profile
    val twins: Collection<Installed>

    val installerInfo: InstallerInfo

    val internetAccess: InternetAccess

    val batteryOptimization: BatteryOptimization
        get() = BatteryOptimization.MANAGED_BY_SYSTEM

    val accessibilityServices: Collection<AccessibilityService>
        get() = emptyList()
}