package eu.darken.myperm.settings.ui.watcher

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.watcher.core.WatcherNotificationCapability
import eu.darken.myperm.watcher.core.WatcherScope
import eu.darken.myperm.watcher.core.WatcherWorkScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class WatcherSettingsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val generalSettings: GeneralSettings,
    private val changeDao: PermissionChangeDao,
    private val watcherWorkScheduler: WatcherWorkScheduler,
    private val upgradeRepo: UpgradeRepo,
    private val notificationCapability: WatcherNotificationCapability,
) : ViewModel4(dispatcherProvider) {

    val isPro: StateFlow<Boolean> = upgradeRepo.upgradeInfo
        .map { it.isPro }
        .stateIn(vmScope, SharingStarted.Eagerly, upgradeRepo.upgradeInfo.value.isPro)

    val isWatcherEnabled: Flow<Boolean> = generalSettings.isWatcherEnabled.flow
    val watcherScope: Flow<WatcherScope> = generalSettings.watcherScope.flow
    val isNotificationsEnabled: Flow<Boolean> = generalSettings.isWatcherNotificationsEnabled.flow
    val isNotifyOnlyOnGained: Flow<Boolean> = generalSettings.isWatcherNotifyOnlyOnGained.flow
    val retentionDays: Flow<Int> = generalSettings.watcherRetentionDays.flow
    val pollingIntervalHours: Flow<Int> = generalSettings.watcherPollingIntervalHours.flow
    val isBatteryHintDismissed: Flow<Boolean> = generalSettings.isWatcherBatteryHintDismissed.flow
    val reportCount: Flow<Int> = changeDao.getTotalCount()

    fun setWatcherEnabled(enabled: Boolean) = launch {
        log(TAG) { "Setting watcher enabled: $enabled" }
        generalSettings.isWatcherEnabled.value(enabled)
        watcherWorkScheduler.ensureScheduled()
    }

    fun onUpgrade() {
        navTo(Nav.Main.Upgrade)
    }

    fun setWatcherScope(scope: WatcherScope) = launch {
        generalSettings.watcherScope.value(scope)
    }

    fun isNotificationPermissionDenied(): Boolean = notificationCapability.isRuntimePermissionDenied()

    fun setNotificationsEnabled(enabled: Boolean) = launch {
        generalSettings.isWatcherNotificationsEnabled.value(enabled)
    }

    fun setNotifyOnlyOnGained(enabled: Boolean) = launch {
        generalSettings.isWatcherNotifyOnlyOnGained.value(enabled)
    }

    fun setRetentionDays(days: Int) = launch {
        log(TAG) { "Setting retention to $days days" }
        generalSettings.watcherRetentionDays.value(days)
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        changeDao.deleteOlderThan(cutoff)
    }

    fun setPollingIntervalHours(hours: Int) = launch {
        log(TAG) { "Setting polling interval to $hours hours" }
        generalSettings.watcherPollingIntervalHours.value(hours)
        watcherWorkScheduler.reschedule(hours)
    }

    fun setBatteryHintDismissed(dismissed: Boolean) = launch {
        generalSettings.isWatcherBatteryHintDismissed.value(dismissed)
    }

    fun clearAllReports() = launch {
        log(TAG) { "Clearing all reports" }
        changeDao.deleteAll()
    }

    companion object {
        private val TAG = logTag("Settings", "Watcher", "VM")
    }
}
