package eu.darken.myperm.settings.ui.support

import android.os.SystemClock
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.PrivacyPolicy
import eu.darken.myperm.common.SupportLinks
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.debug.recording.core.DebugSessionManager
import eu.darken.myperm.common.flow.DynamicStateFlow
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val webpageTool: WebpageTool,
    private val debugSessionManager: DebugSessionManager,
) : ViewModel4(dispatcherProvider) {

    data class State(
        val isRecording: Boolean = false,
        val currentLogPath: File? = null,
        val recordingStartedAt: Long = 0L,
        val sessions: List<DebugSessionManager.LogSession> = emptyList(),
    ) {
        val hasAnySessions: Boolean get() = sessions.isNotEmpty()
        val storedSessionCount: Int get() = sessions.count { it !is DebugSessionManager.LogSession.Recording }
        val storedSessionSize: Long get() = sessions.filterNot { it is DebugSessionManager.LogSession.Recording }.sumOf { it.size }
    }

    sealed interface Event {
        data object ShowConsentDialog : Event
        data object ShowShortRecordingWarning : Event
        data class OpenRecorderActivity(val path: File) : Event
    }

    val events = SingleEventFlow<Event>()

    private val stater = DynamicStateFlow(TAG, vmScope) { State() }
    val state: Flow<State> = stater.flow

    init {
        combine(
            debugSessionManager.recorderState,
            debugSessionManager.sessions,
        ) { recorderState, sessions ->
            stater.updateBlocking {
                copy(
                    isRecording = recorderState.isRecording,
                    currentLogPath = recorderState.currentLogPath,
                    recordingStartedAt = recorderState.recordingStartedAt,
                    sessions = sessions,
                )
            }
        }.launchIn(vmScope)
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
        val current = stater.value()
        if (current.isRecording) {
            doStopDebugLog(current)
        } else {
            events.tryEmit(Event.ShowConsentDialog)
        }
    }

    fun startDebugLog() = launch {
        log(TAG) { "startDebugLog()" }
        debugSessionManager.startRecorder()
    }

    private suspend fun doStopDebugLog(current: State) {
        val duration = SystemClock.elapsedRealtime() - current.recordingStartedAt
        if (duration < 5_000L) {
            events.tryEmit(Event.ShowShortRecordingWarning)
            return
        }
        log(TAG) { "stopDebugLog()" }
        debugSessionManager.stopRecorder(showResultUi = true)
    }

    fun forceStopDebugLog() = launch {
        log(TAG) { "forceStopDebugLog()" }
        debugSessionManager.stopRecorder(showResultUi = true)
    }

    fun stopActiveRecording() = launch {
        log(TAG) { "stopActiveRecording()" }
        debugSessionManager.stopActiveRecording()
    }

    fun openSession(session: DebugSessionManager.LogSession.Ready) {
        events.tryEmit(Event.OpenRecorderActivity(session.path))
    }

    fun deleteLogSession(session: DebugSessionManager.LogSession) = launch {
        log(TAG) { "deleteLogSession(${session.path})" }
        debugSessionManager.deleteLogSession(session)
    }

    fun clearDebugLogs() = launch {
        log(TAG) { "clearDebugLogs()" }
        debugSessionManager.deleteAllLogs()
    }

    companion object {
        private val TAG = logTag("Settings", "Support", "VM")
    }
}
