package eu.darken.myperm.watcher.ui.dashboard

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.combine
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.room.entity.TriggerReason
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.watcher.core.PermissionDiff
import eu.darken.myperm.watcher.core.WatcherManager
import eu.darken.myperm.watcher.core.WatcherNotificationCapability
import eu.darken.myperm.watcher.core.WatcherWorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class WatcherDashboardViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val generalSettings: GeneralSettings,
    private val changeDao: PermissionChangeDao,
    private val upgradeRepo: UpgradeRepo,
    private val capability: WatcherNotificationCapability,
    private val watcherWorkScheduler: WatcherWorkScheduler,
    private val watcherManager: WatcherManager,
    private val json: Json,
) : ViewModel4(dispatcherProvider) {

    data class State(
        val isWatcherEnabled: Boolean = false,
        val isPro: Boolean = false,
        val reports: List<WatcherReportItem> = emptyList(),
        val showNotificationPermissionCard: Boolean = false,
        val canRequestNotificationPermission: Boolean = false,
        val refreshPhase: WatcherManager.Phase? = null,
        val filterOptions: WatcherFilterOptions = WatcherFilterOptions(),
        val hasUnseen: Boolean = false,
        val totalReportCount: Int = 0,
        val lockedReportCount: Int = 0,
    )

    private val notificationsAvailable = MutableStateFlow(capability.areNotificationsEnabled())
    private val searchTerm = MutableStateFlow<String?>(null)

    fun refreshNotificationState() {
        notificationsAvailable.value = capability.areNotificationsEnabled()
    }

    val state = combine(
        generalSettings.isWatcherEnabled.flow,
        upgradeRepo.upgradeInfo.map { it.isPro },
        changeDao.getAll(),
        generalSettings.isWatcherNotificationsEnabled.flow,
        notificationsAvailable,
        watcherManager.phase,
        searchTerm,
        generalSettings.watcherFilterOptions.flow,
    ) { isEnabled, isPro, entities, notificationsEnabled, notifAvailable, phase, search, filterOpts ->
        val allItems = entities.map { it.toItem() }
        val filteredItems = allItems
            .filter { filterOpts.matches(it) }
            .filter {
                val term = search?.lowercase() ?: return@filter true
                if (it.packageName.lowercase().contains(term)) return@filter true
                if (it.appLabel?.lowercase()?.contains(term) == true) return@filter true
                false
            }

        val duplicateLabels = filteredItems
            .filter { it.appLabel != null }
            .groupBy { it.appLabel }
            .filterValues { items -> items.distinctBy { it.packageName }.size > 1 }
            .keys

        val allReports = filteredItems.map { item ->
            item.copy(showPkgName = item.appLabel in duplicateLabels)
        }

        val reports = if (!isPro && allReports.size > FREE_REPORT_LIMIT) {
            allReports.take(FREE_REPORT_LIMIT)
        } else {
            allReports
        }
        val lockedCount = if (!isPro) (allReports.size - reports.size).coerceAtLeast(0) else 0

        State(
            isWatcherEnabled = isEnabled,
            isPro = isPro,
            reports = reports,
            showNotificationPermissionCard = isEnabled && notificationsEnabled && !notifAvailable && isPro,
            canRequestNotificationPermission = capability.isRuntimePermissionDenied(),
            refreshPhase = phase,
            filterOptions = filterOpts,
            hasUnseen = reports.any { !it.isSeen },
            totalReportCount = allItems.size,
            lockedReportCount = lockedCount,
        )
    }.asStateFlow(State())

    fun toggleWatcher() = launch {
        val current = generalSettings.isWatcherEnabled.value()
        log(TAG) { "Toggling watcher: $current -> ${!current}" }
        generalSettings.isWatcherEnabled.value(!current)
        watcherWorkScheduler.ensureScheduled()
    }

    fun onReportClicked(item: WatcherReportItem) = launch {
        if (!upgradeRepo.upgradeInfo.value.isPro) {
            log(TAG) { "Not pro, navigating to upgrade instead of detail" }
            navTo(Nav.Main.Upgrade)
            return@launch
        }
        changeDao.markSeen(item.id)
        navTo(Nav.Watcher.ReportDetail(item.id))
    }

    fun goToUpgrade() {
        navTo(Nav.Main.Upgrade)
    }

    fun refreshNow() = launch {
        log(TAG) { "refreshNow()" }
        try {
            watcherManager.scanDiffAndPrune(TriggerReason.MANUAL_REFRESH)
        } catch (e: Exception) {
            log(TAG, WARN) { "Refresh failed: ${e.asLog()}" }
        }
    }

    fun markAllSeen() = launch {
        changeDao.markAllSeen()
    }

    fun disableNotifications() = launch {
        generalSettings.isWatcherNotificationsEnabled.value(false)
    }

    fun goToSettings() {
        navTo(Nav.Settings.Index)
    }

    fun onSearchInputChanged(term: String?) {
        log(TAG) { "onSearchInputChanged(term=$term)" }
        searchTerm.value = term
    }

    fun updateFilterOptions(action: (WatcherFilterOptions) -> WatcherFilterOptions) = launch {
        generalSettings.watcherFilterOptions.update { action(it) }
    }

    private fun PermissionChangeEntity.toItem(): WatcherReportItem {
        val diff = runCatching {
            json.decodeFromString<PermissionDiff>(changesJson)
        }.onFailure { e ->
            log(TAG, WARN) { "Failed to deserialize changesJson for report $id: ${e.asLog()}" }
        }.getOrNull()

        return WatcherReportItem(
            id = id,
            packageName = packageName,
            appLabel = appLabel,
            versionName = versionName,
            previousVersionName = previousVersionName,
            eventType = eventType,
            detectedAt = detectedAt,
            isSeen = isSeen,
            hasAddedPermissions = diff?.let { it.addedPermissions.isNotEmpty() || it.addedDeclared.isNotEmpty() } ?: false,
            hasLostPermissions = diff?.let { it.removedPermissions.isNotEmpty() || it.removedDeclared.isNotEmpty() } ?: false,
            gainedCount = diff?.gainedCount ?: 0,
            lostCount = diff?.lostCount ?: 0,
        )
    }

    companion object {
        private const val FREE_REPORT_LIMIT = 5
        private val TAG = logTag("Watcher", "Dashboard", "VM")
    }
}
