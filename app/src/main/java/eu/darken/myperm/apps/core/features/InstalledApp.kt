package eu.darken.myperm.apps.core.features

import java.time.Instant

interface InstalledApp {
    val isSystemApp: Boolean
    val installedAt: Instant
    val updatedAt: Instant

    val internetAccess: InternetAccess
    val installerInfo: InstallerInfo
}