package eu.darken.myperm.watcher.ui.dashboard

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.room.entity.TriggerReason
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.watcher.core.WatcherManager
import eu.darken.myperm.watcher.core.WatcherNotificationCapability
import eu.darken.myperm.watcher.core.WatcherWorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import eu.darken.myperm.common.flow.combine
import kotlinx.coroutines.flow.map
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
) : ViewModel4(dispatcherProvider) {

    data class ReportItem(
        val id: Long,
        val packageName: String,
        val appLabel: String?,
        val versionName: String?,
        val previousVersionName: String?,
        val eventType: String,
        val detectedAt: Long,
        val isSeen: Boolean,
    )

    data class State(
        val isWatcherEnabled: Boolean = false,
        val isPro: Boolean = false,
        val reports: List<ReportItem> = emptyList(),
        val showNotificationPermissionCard: Boolean = false,
        val canRequestNotificationPermission: Boolean = false,
        val refreshPhase: WatcherManager.Phase? = null,
    )

    private val notificationsAvailable = MutableStateFlow(capability.areNotificationsEnabled())

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
    ) { isEnabled, isPro, entities, notificationsEnabled, notifAvailable, phase ->
        State(
            isWatcherEnabled = isEnabled,
            isPro = isPro,
            reports = entities.map { it.toItem() },
            showNotificationPermissionCard = isEnabled && notificationsEnabled && !notifAvailable,
            canRequestNotificationPermission = capability.isRuntimePermissionDenied(),
            refreshPhase = phase,
        )
    }.asStateFlow(State())

    fun toggleWatcher() = launch {
        val isPro = upgradeRepo.upgradeInfo.value.isPro
        if (!isPro) {
            log(TAG) { "Not pro, navigating to upgrade" }
            navTo(Nav.Main.Upgrade)
            return@launch
        }

        val current = generalSettings.isWatcherEnabled.value()
        log(TAG) { "Toggling watcher: $current -> ${!current}" }
        generalSettings.isWatcherEnabled.value(!current)
        watcherWorkScheduler.ensureScheduled()
    }

    fun onReportClicked(item: ReportItem) = launch {
        changeDao.markSeen(item.id)
        navTo(Nav.Watcher.ReportDetail(item.id))
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

    private fun PermissionChangeEntity.toItem() = ReportItem(
        id = id,
        packageName = packageName,
        appLabel = appLabel,
        versionName = versionName,
        previousVersionName = previousVersionName,
        eventType = eventType,
        detectedAt = detectedAt,
        isSeen = isSeen,
    )

    companion object {
        private val TAG = logTag("Watcher", "Dashboard", "VM")
    }
}
