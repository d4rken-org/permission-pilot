package eu.darken.myperm.settings.ui.watcher

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.watcher.core.WatcherScope
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class WatcherSettingsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val generalSettings: GeneralSettings,
    private val changeDao: PermissionChangeDao,
) : ViewModel4(dispatcherProvider) {

    val isWatcherEnabled: Flow<Boolean> = generalSettings.isWatcherEnabled.flow
    val watcherScope: Flow<WatcherScope> = generalSettings.watcherScope.flow
    val isNotificationsEnabled: Flow<Boolean> = generalSettings.isWatcherNotificationsEnabled.flow
    val retentionDays: Flow<Int> = generalSettings.watcherRetentionDays.flow
    val reportCount: Flow<Int> = changeDao.getTotalCount()

    fun setWatcherEnabled(enabled: Boolean) = launch {
        log(TAG) { "Setting watcher enabled: $enabled" }
        generalSettings.isWatcherEnabled.value(enabled)
    }

    fun setWatcherScope(scope: WatcherScope) = launch {
        generalSettings.watcherScope.value(scope)
    }

    fun setNotificationsEnabled(enabled: Boolean) = launch {
        generalSettings.isWatcherNotificationsEnabled.value(enabled)
    }

    fun setRetentionDays(days: Int) = launch {
        log(TAG) { "Setting retention to $days days" }
        generalSettings.watcherRetentionDays.value(days)
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        changeDao.deleteOlderThan(cutoff)
    }

    fun clearAllReports() = launch {
        log(TAG) { "Clearing all reports" }
        changeDao.deleteAll()
    }

    companion object {
        private val TAG = logTag("Settings", "Watcher", "VM")
    }
}
