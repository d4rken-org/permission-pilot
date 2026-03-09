package eu.darken.myperm.settings.ui.index

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.PrivacyPolicy
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.uix.ViewModel4
import javax.inject.Inject

@HiltViewModel
class SettingsIndexViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider) {

    fun openChangelog() {
        webpageTool.open("https://myperm.darken.eu/changelog")
    }

    fun openPrivacyPolicy() {
        webpageTool.open(PrivacyPolicy.URL)
    }

    companion object {
        private val TAG = logTag("Settings", "Index", "VM")
    }
}
