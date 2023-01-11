package eu.darken.myperm.settings.ui.support

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import eu.darken.myperm.common.uix.ViewModel3
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SupportFragmentVM @Inject constructor(
    @Suppress("unused") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val recorderModule: RecorderModule,
) : ViewModel3(dispatcherProvider) {

    val isRecording = recorderModule.state.map { it.isRecording }.asLiveData2()

    val isRecorderAvailable = recorderModule.state.map { it.isAvailable }.asLiveData2()

    fun startDebugLog() = launch {
        log { "startDebugLog()" }
        recorderModule.startRecorder()
    }
}