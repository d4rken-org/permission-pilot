package eu.darken.myperm.watcher.ui.dashboard

data class WatcherReportItem(
    val id: Long,
    val packageName: String,
    val appLabel: String?,
    val showPkgName: Boolean = false,
    val versionName: String?,
    val previousVersionName: String?,
    val eventType: String,
    val detectedAt: Long,
    val isSeen: Boolean,
    val hasAddedPermissions: Boolean,
    val hasLostPermissions: Boolean,
)
