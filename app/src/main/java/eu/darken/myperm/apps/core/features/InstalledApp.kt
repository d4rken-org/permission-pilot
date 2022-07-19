package eu.darken.myperm.apps.core.features

import android.content.pm.PackageInfo
import java.time.Instant

interface InstalledApp {
    val packageInfo: PackageInfo

    val isSystemApp: Boolean

    val installedAt: Instant?
        get() = packageInfo.firstInstallTime.takeIf { it != 0L }?.let { Instant.ofEpochMilli(it) }

    val updatedAt: Instant?
        get() = packageInfo.lastUpdateTime.takeIf { it != 0L }?.let { Instant.ofEpochMilli(it) }

    val internetAccess: InternetAccess
    val installerInfo: InstallerInfo
}