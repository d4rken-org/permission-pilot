package eu.darken.myperm.watcher.ui.dashboard

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.watcher.core.WatcherEventType

internal object WatcherDashboardPreviewData {

    fun loadedState() = WatcherDashboardViewModel.State(
        isWatcherEnabled = true,
        isPro = true,
        reports = listOf(
            WatcherReportItem(
                id = 1,
                packageName = Pkg.Name("com.whatsapp"),
                appLabel = "WhatsApp",
                versionName = "2.26.4.12",
                previousVersionName = "2.26.3.18",
                eventType = WatcherEventType.UPDATE,
                detectedAt = 1_736_942_400_000L, // 2025-01-15 12:00 UTC
                isSeen = false,
                hasAddedPermissions = true,
                hasLostPermissions = false,
                gainedCount = 2,
            ),
            WatcherReportItem(
                id = 2,
                packageName = Pkg.Name("org.thoughtcrime.securesms"),
                appLabel = "Signal",
                versionName = "7.18.0",
                previousVersionName = null,
                eventType = WatcherEventType.INSTALL,
                detectedAt = 1_736_886_600_000L, // 2025-01-14 18:30 UTC
                isSeen = false,
                hasAddedPermissions = false,
                hasLostPermissions = false,
            ),
            WatcherReportItem(
                id = 3,
                packageName = Pkg.Name("com.spotify.music"),
                appLabel = "Spotify",
                versionName = "9.0.12.567",
                previousVersionName = "9.0.12.567",
                eventType = WatcherEventType.GRANT_CHANGE,
                detectedAt = 1_736_673_300_000L, // 2025-01-12 09:15 UTC
                isSeen = false,
                hasAddedPermissions = false,
                hasLostPermissions = true,
                lostCount = 1,
            ),
            WatcherReportItem(
                id = 4,
                packageName = Pkg.Name("com.google.chrome"),
                appLabel = "Chrome",
                versionName = "131.0.6778.135",
                previousVersionName = "130.0.6723.103",
                eventType = WatcherEventType.UPDATE,
                detectedAt = 1_736_344_800_000L, // 2025-01-08 14:00 UTC
                isSeen = true,
                hasAddedPermissions = true,
                hasLostPermissions = true,
                gainedCount = 1,
                lostCount = 1,
            ),
            WatcherReportItem(
                id = 5,
                packageName = Pkg.Name("com.duckduckgo.mobile.android"),
                appLabel = "DuckDuckGo",
                versionName = "5.214.0",
                previousVersionName = "5.214.0",
                eventType = WatcherEventType.REMOVED,
                detectedAt = 1_735_556_400_000L, // 2024-12-30 11:00 UTC
                isSeen = true,
                hasAddedPermissions = false,
                hasLostPermissions = false,
            ),
        ),
        hasUnseen = true,
        totalReportCount = 5,
        lockedReportCount = 0,
    )
}
