package eu.darken.myperm.settings.ui.support

import android.os.SystemClock
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.PrivacyPolicy
import eu.darken.myperm.common.SupportLinks
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import eu.darken.myperm.common.flow.DynamicStateFlow
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val webpageTool: WebpageTool,
    private val recorderModule: RecorderModule,
) : ViewModel4(dispatcherProvider) {

    data class State(
        val isRecording: Boolean = false,
        val currentLogPath: File? = null,
        val recordingStartedAt: Long = 0L,
        val logFolderSize: Long = 0L,
        val logSessionCount: Int = 0,
    )

    sealed interface Event {
        data object ShowConsentDialog : Event
        data object ShowShortRecordingWarning : Event
    }

    val events = SingleEventFlow<Event>()

    private val stater = DynamicStateFlow(TAG, vmScope) {
        State(
            logFolderSize = recorderModule.getLogFolderSize(),
            logSessionCount = recorderModule.getLogSessionCount(),
        )
    }
    val state: Flow<State> = stater.flow

    init {
        recorderModule.state
            .onEach { recorderState ->
                stater.updateBlocking {
                    copy(
                        isRecording = recorderState.isRecording,
                        currentLogPath = recorderState.currentLogPath,
                        recordingStartedAt = recorderState.recordingStartedAt,
                        logFolderSize = recorderModule.getLogFolderSize(),
                        logSessionCount = recorderModule.getLogSessionCount(),
                    )
                }
            }
            .launchIn(vmScope)
    }

    fun openIssueTracker() {
        webpageTool.open(SupportLinks.GITHUB_ISSUES_URL)
    }

    fun openDiscord() {
        webpageTool.open(SupportLinks.DISCORD_URL)
    }

    fun openPrivacyPolicy() {
        webpageTool.open(PrivacyPolicy.URL)
    }

    fun navigateToContactForm() {
        navTo(Nav.Settings.ContactForm)
    }

    fun onDebugLogToggle() = launch {
        if (stater.value().isRecording) {
            doStopDebugLog()
        } else {
            events.tryEmit(Event.ShowConsentDialog)
        }
    }

    fun startDebugLog() = launch {
        log(TAG) { "startDebugLog()" }
        recorderModule.startRecorder()
    }

    private suspend fun doStopDebugLog() {
        val duration = SystemClock.elapsedRealtime() - stater.value().recordingStartedAt
        if (duration < 5_000L) {
            events.tryEmit(Event.ShowShortRecordingWarning)
            return
        }
        log(TAG) { "stopDebugLog()" }
        recorderModule.stopRecorder()
        doRefreshLogSize()
    }

    fun forceStopDebugLog() = launch {
        log(TAG) { "forceStopDebugLog()" }
        recorderModule.stopRecorder()
        doRefreshLogSize()
    }

    fun clearDebugLogs() = launch {
        log(TAG) { "clearDebugLogs()" }
        recorderModule.deleteAllLogs()
        doRefreshLogSize()
    }

    fun refreshLogSize() = launch {
        doRefreshLogSize()
    }

    private suspend fun doRefreshLogSize() {
        stater.updateBlocking {
            copy(
                logFolderSize = recorderModule.getLogFolderSize(),
                logSessionCount = recorderModule.getLogSessionCount(),
            )
        }
    }

    companion object {
        private val TAG = logTag("Settings", "Support", "VM")
    }
}
