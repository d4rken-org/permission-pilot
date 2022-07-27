package eu.darken.myperm.apps.core.features

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.UserHandle
import eu.darken.myperm.apps.core.Pkg
import java.time.Instant

interface Installed : Pkg {
    val packageInfo: PackageInfo
    val userHandle: UserHandle

    val isSystemApp: Boolean
        get() = packageInfo.applicationInfo?.run { flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: true

    val installedAt: Instant?
        get() = packageInfo.firstInstallTime.takeIf { it != 0L }?.let { Instant.ofEpochMilli(it) }

    val updatedAt: Instant?
        get() = packageInfo.lastUpdateTime.takeIf { it != 0L }?.let { Instant.ofEpochMilli(it) }

    // Same user id
    val siblings: Collection<Pkg>

    // Extra user profile
    val twins: Collection<Installed>

    val internetAccess: InternetAccess
    val installerInfo: InstallerInfo
}