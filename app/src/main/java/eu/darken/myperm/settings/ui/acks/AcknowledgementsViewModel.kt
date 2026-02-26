package eu.darken.myperm.settings.ui.acks

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.uix.ViewModel4
import javax.inject.Inject

@HiltViewModel
class AcknowledgementsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider) {

    fun openUrl(url: String) {
        webpageTool.open(url)
    }

    companion object {
        private val TAG = logTag("Settings", "Acknowledgements", "VM")
    }
}
