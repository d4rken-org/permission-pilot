package eu.darken.myperm.watcher.ui.dashboard

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.watcher.core.WatcherEventType

internal object WatcherDashboardPreviewData {

    fun loadedState() = WatcherDashboardViewModel.State(
        isWatcherEnabled = true,
        isUpgradeLocked = false,
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
                packageName = Pkg.Name("org.telegram.messenger"),
                appLabel = "Telegram",
                versionName = "11.5.2",
                previousVersionName = "11.4.4",
                eventType = WatcherEventType.UPDATE,
                detectedAt = 1_736_631_600_000L, // 2025-01-11 21:40 UTC
                isSeen = true,
                hasAddedPermissions = true,
                hasLostPermissions = true,
                gainedCount = 1,
                lostCount = 2,
            ),
            WatcherReportItem(
                id = 5,
                packageName = Pkg.Name("com.instagram.android"),
                appLabel = "Instagram",
                versionName = "365.0.0.36.94",
                previousVersionName = "363.1.0.29.85",
                eventType = WatcherEventType.UPDATE,
                detectedAt = 1_736_525_100_000L, // 2025-01-10 16:05 UTC
                isSeen = true,
                hasAddedPermissions = true,
                hasLostPermissions = false,
                gainedCount = 3,
            ),
            WatcherReportItem(
                id = 6,
                packageName = Pkg.Name("com.android.chrome"),
                appLabel = "Chrome",
                versionName = "131.0.6778.135",
                previousVersionName = "130.0.6723.103",
                eventType = WatcherEventType.UPDATE,
                detectedAt = 1_736_412_600_000L, // 2025-01-09 08:50 UTC
                isSeen = true,
                hasAddedPermissions = true,
                hasLostPermissions = true,
                gainedCount = 1,
                lostCount = 1,
            ),
            WatcherReportItem(
                id = 7,
                packageName = Pkg.Name("org.mozilla.firefox"),
                appLabel = "Firefox",
                versionName = "134.0.1",
                previousVersionName = "134.0.1",
                eventType = WatcherEventType.GRANT_CHANGE,
                detectedAt = 1_736_277_900_000L, // 2025-01-07 19:25 UTC
                isSeen = true,
                hasAddedPermissions = true,
                hasLostPermissions = false,
                gainedCount = 1,
            ),
            WatcherReportItem(
                id = 8,
                packageName = Pkg.Name("com.discord"),
                appLabel = "Discord",
                versionName = "252.5",
                previousVersionName = "251.14",
                eventType = WatcherEventType.UPDATE,
                detectedAt = 1_736_169_000_000L, // 2025-01-06 13:10 UTC
                isSeen = true,
                hasAddedPermissions = false,
                hasLostPermissions = true,
                lostCount = 2,
            ),
            WatcherReportItem(
                id = 9,
                packageName = Pkg.Name("com.netflix.mediaclient"),
                appLabel = "Netflix",
                versionName = "8.138.0",
                previousVersionName = null,
                eventType = WatcherEventType.INSTALL,
                detectedAt = 1_736_063_100_000L, // 2025-01-05 07:45 UTC
                isSeen = true,
                hasAddedPermissions = false,
                hasLostPermissions = false,
            ),
            WatcherReportItem(
                id = 10,
                packageName = Pkg.Name("com.google.android.apps.maps"),
                appLabel = "Maps",
                versionName = "11.114.0102",
                previousVersionName = "11.113.0101",
                eventType = WatcherEventType.UPDATE,
                detectedAt = 1_735_943_400_000L, // 2025-01-03 22:30 UTC
                isSeen = true,
                hasAddedPermissions = true,
                hasLostPermissions = true,
                gainedCount = 1,
                lostCount = 1,
            ),
            WatcherReportItem(
                id = 11,
                packageName = Pkg.Name("com.Slack"),
                appLabel = "Slack",
                versionName = "24.12.20.0",
                previousVersionName = "24.12.20.0",
                eventType = WatcherEventType.GRANT_CHANGE,
                detectedAt = 1_735_831_200_000L, // 2025-01-02 15:20 UTC
                isSeen = true,
                hasAddedPermissions = true,
                hasLostPermissions = false,
                gainedCount = 2,
            ),
            WatcherReportItem(
                id = 13,
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
        totalReportCount = 12,
        lockedReportCount = 0,
    )
}
