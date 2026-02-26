package eu.darken.myperm.settings.ui.support

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val webpageTool: WebpageTool,
    private val recorderModule: RecorderModule,
) : ViewModel4(dispatcherProvider) {

    val recorderState: Flow<RecorderModule.State> = recorderModule.state

    fun openIssueTracker() {
        webpageTool.open("https://github.com/d4rken-org/permission-pilot/issues")
    }

    fun openDiscord() {
        webpageTool.open("https://discord.gg/7gGWxfM5yv")
    }

    fun startDebugLog() = launch {
        recorderModule.startRecorder()
    }

    companion object {
        private val TAG = logTag("Settings", "Support", "VM")
    }
}
