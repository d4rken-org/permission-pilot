package eu.darken.myperm.settings.ui.watcher

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.watcher.core.WatcherScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class WatcherSettingsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val generalSettings: GeneralSettings,
) : ViewModel4(dispatcherProvider) {

    val isWatcherEnabled: Flow<Boolean> = generalSettings.isWatcherEnabled.flow
    val watcherScope: Flow<WatcherScope> = generalSettings.watcherScope.flow

    fun setWatcherEnabled(enabled: Boolean) = launch {
        generalSettings.isWatcherEnabled.value(enabled)
    }

    fun setWatcherScope(scope: WatcherScope) = launch {
        generalSettings.watcherScope.value(scope)
    }

    fun goToReports() {
        navTo(Nav.Watcher.Reports)
    }

    companion object {
        private val TAG = logTag("Settings", "Watcher", "VM")
    }
}
