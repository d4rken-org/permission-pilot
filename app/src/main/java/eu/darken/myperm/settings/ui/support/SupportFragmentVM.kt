package eu.darken.myperm.settings.ui.support

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.EmailTool
import eu.darken.myperm.common.InstallId
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import eu.darken.myperm.common.livedata.SingleLiveEvent
import eu.darken.myperm.common.uix.ViewModel3
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SupportFragmentVM @Inject constructor(
    private val handle: SavedStateHandle,
    private val emailTool: EmailTool,
    private val installId: InstallId,
    private val dispatcherProvider: DispatcherProvider,
    private val recorderModule: RecorderModule,
) : ViewModel3(dispatcherProvider) {

    val emailEvent = SingleLiveEvent<Intent>()
    val clipboardEvent = SingleLiveEvent<String>()

    fun copyInstallID() = launch {
        log { "copyInstallID()" }
        clipboardEvent.postValue(installId.id)
    }

    val isRecording = recorderModule.state.map { it.isRecording }.asLiveData2()

    fun startDebugLog() = launch {
        log { "startDebugLog()" }
        recorderModule.startRecorder()
    }
}