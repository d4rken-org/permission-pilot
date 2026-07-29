package eu.darken.myperm.watcher.ui.dashboard

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.flow.combine
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.room.entity.TriggerReason
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.common.upgrade.isProForUi
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.watcher.core.PermissionDiff
import eu.darken.myperm.watcher.core.WatcherManager
import eu.darken.myperm.watcher.core.WatcherBatteryCapability
import eu.darken.myperm.watcher.core.WatcherNotificationCapability
import eu.darken.myperm.watcher.core.WatcherNotifications
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
    private val watcherNotifications: WatcherNotifications,
    private val batteryCapability: WatcherBatteryCapability,
    private val json: Json,
) : ViewModel4(dispatcherProvider) {

    val events = SingleEventFlow<Event>()

    sealed interface Event {
        /**
         * Emitted only after the entitlement gate authorized the premium notification feature — the
         * screen must not launch the OS permission dialog (or the notification settings intent)
         * before this arrives.
         */
        data object RequestNotificationPermission : Event
    }

    data class State(
        val isWatcherEnabled: Boolean = false,
        /**
         * True only when billing settled without an error and reports no entitlement — the
         * presentation mirror of what `isProForUi` would deny. While billing is still connecting
         * (GPlay cold-start seed) a paying user keeps the full report list instead of seeing it
         * truncated; the tap handlers re-check via `isProForUi` before acting.
         */
        val isUpgradeLocked: Boolean = false,
        val reports: List<WatcherReportItem> = emptyList(),
        val showNotificationPermissionCard: Boolean = false,
        val canRequestNotificationPermission: Boolean = false,
        val showBatteryOptimizationCard: Boolean = false,
        val refreshPhase: WatcherManager.Phase? = null,
        val filterOptions: WatcherFilterOptions = WatcherFilterOptions(),
        val hasUnseen: Boolean = false,
        val totalReportCount: Int = 0,
        val lockedReportCount: Int = 0,
    )

    private val notificationsAvailable = MutableStateFlow(capability.areNotificationsEnabled())
    private val showBatteryCard = MutableStateFlow(computeBatteryCard())
    private val searchTerm = MutableStateFlow<String?>(null)

    fun refreshNotificationState() {
        notificationsAvailable.value = capability.areNotificationsEnabled()
    }

    private fun computeBatteryCard(): Boolean {
        val isOptimized = !batteryCapability.isBatteryOptimizationIgnored()
        if (!isOptimized) return false
        val lastPoll = generalSettings.watcherLastSuccessfulPollAt.valueBlocking
        val intervalMs = generalSettings.watcherPollingIntervalHours.valueBlocking.toLong() * 3_600_000L
        val staleSince = System.currentTimeMillis() - lastPoll
        return lastPoll > 0L && staleSince > intervalMs * 4
    }

    fun refreshBatteryState() {
        showBatteryCard.value = computeBatteryCard()
    }

    fun dismissBatteryHint() = launch {
        generalSettings.isWatcherBatteryHintDismissed.value(true)
    }

    val state = combine(
        generalSettings.isWatcherEnabled.flow,
        // Hard-locked = settled, error-free and no entitlement. Anything else (unsettled seed,
        // error) keeps the pro presentation, so the cold-start race can't truncate a paying user's
        // reports or hide their notification card.
        upgradeRepo.upgradeInfo.map { it.error == null && it.isSettled && !it.isPro },
        changeDao.getAll(),
        generalSettings.isWatcherNotificationsEnabled.flow,
        notificationsAvailable,
        watcherManager.phase,
        searchTerm,
        generalSettings.watcherFilterOptions.flow,
        showBatteryCard,
        generalSettings.isWatcherBatteryHintDismissed.flow,
    ) { isEnabled, isUpgradeLocked, entities, notificationsEnabled, notifAvailable, phase, search, filterOpts, batteryCardVisible, batteryDismissed ->
        val allItems = entities.map { it.toItem() }
        val filteredItems = allItems
            .filter { filterOpts.matches(it) }
            .filter {
                val term = search?.lowercase() ?: return@filter true
                if (it.packageName.value.lowercase().contains(term)) return@filter true
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

        val reports = if (isUpgradeLocked && allReports.size > FREE_REPORT_LIMIT) {
            allReports.take(FREE_REPORT_LIMIT)
        } else {
            allReports
        }
        val lockedCount = if (isUpgradeLocked) (allReports.size - reports.size).coerceAtLeast(0) else 0

        State(
            isWatcherEnabled = isEnabled,
            isUpgradeLocked = isUpgradeLocked,
            reports = reports,
            showNotificationPermissionCard = isEnabled && notificationsEnabled && !notifAvailable && !isUpgradeLocked,
            canRequestNotificationPermission = capability.isRuntimePermissionDenied(),
            showBatteryOptimizationCard = isEnabled && batteryCardVisible && !batteryDismissed,
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
        // Interactive gate: waits out the cold-start handshake instead of bouncing a paying user
        // to the upgrade screen, and resolves immediately for a settled free user.
        if (!upgradeRepo.isProForUi()) {
            log(TAG) { "Not pro, navigating to upgrade instead of detail" }
            navTo(Nav.Main.Upgrade())
            return@launch
        }
        changeDao.markSeen(item.id)
        watcherNotifications.cancelForPackage(item.packageName)
        navTo(Nav.Watcher.ReportDetail(item.id))
    }

    fun requestNotificationPermission() = launch {
        // The notification card renders during the unsettled/error window (see [State.isUpgradeLocked]),
        // so the OS prompt for this premium feature must pass the interactive gate first.
        if (!upgradeRepo.isProForUi()) {
            log(TAG) { "Not pro, navigating to upgrade instead of requesting notification permission" }
            navTo(Nav.Main.Upgrade())
            return@launch
        }
        events.emit(Event.RequestNotificationPermission)
    }

    fun goToUpgrade() {
        navTo(Nav.Main.Upgrade())
    }

    fun refreshNow() = launch {
        log(TAG) { "refreshNow()" }
        try {
            watcherManager.scanDiffAndPrune(TriggerReason.MANUAL_REFRESH)
            generalSettings.watcherLastSuccessfulPollAt.value(System.currentTimeMillis())
        } catch (e: Exception) {
            log(TAG, WARN) { "Refresh failed: ${e.asLog()}" }
        }
    }

    fun markAllSeen() = launch {
        changeDao.markAllSeen()
        watcherNotifications.cancelAllChangeNotifications()
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
