package eu.darken.myperm.settings.ui.general

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.autoreport.DebugSettings
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.uix.ViewModel3
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsFragmentVM @Inject constructor(
    private val handle: SavedStateHandle,
    private val dispatcherProvider: DispatcherProvider,
    private val debugSettings: DebugSettings,
) : ViewModel3(dispatcherProvider) {

    val isAutoReporting = debugSettings.isAutoReportingEnabled.flow.asLiveData2()

    companion object {
        private val TAG = logTag("Settings", "General", "VM")
    }
}