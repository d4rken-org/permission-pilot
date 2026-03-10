package eu.darken.myperm.settings.ui.support

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.PrivacyPolicy
import eu.darken.myperm.common.SupportLinks
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.debug.recording.core.DebugSession
import eu.darken.myperm.common.debug.recording.core.DebugSessionManager
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import eu.darken.myperm.common.flow.DynamicStateFlow
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val webpageTool: WebpageTool,
    private val debugSessionManager: DebugSessionManager,
) : ViewModel4(dispatcherProvider) {

    data class State(
        val isRecording: Boolean = false,
        val sessions: List<DebugSession> = emptyList(),
    ) {
        val hasAnySessions: Boolean get() = sessions.isNotEmpty()
        val logSessionCount: Int get() = sessions.count { it !is DebugSession.Recording }
        val logFolderSize: Long get() = sessions.filterNot { it is DebugSession.Recording }.sumOf { it.diskSize }
        val failedSessions: List<DebugSession.Failed> get() = sessions.filterIsInstance<DebugSession.Failed>()
    }

    sealed interface Event {
        data object ShowConsentDialog : Event
        data object ShowShortRecordingWarning : Event
        data class OpenRecorderActivity(val sessionId: String, val legacyPath: String?) : Event
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

    fun openFaq() {
        webpageTool.open(SupportLinks.FAQ_URL)
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
            doStopDebugLog()
        } else {
            events.tryEmit(Event.ShowConsentDialog)
        }
    }

    fun startDebugLog() = launch {
        log(TAG) { "startDebugLog()" }
        debugSessionManager.startRecording()
    }

    private suspend fun doStopDebugLog() {
        val result = debugSessionManager.requestStopRecording()
        when (result) {
            is RecorderModule.StopResult.TooShort -> {
                events.tryEmit(Event.ShowShortRecordingWarning)
            }
            is RecorderModule.StopResult.Stopped -> {
                log(TAG) { "Recording stopped: ${result.sessionId}" }
                events.tryEmit(Event.OpenRecorderActivity(result.sessionId, result.logDir.path))
            }
            is RecorderModule.StopResult.NotRecording -> {
                log(TAG) { "doStopDebugLog: not recording" }
            }
        }
    }

    fun forceStopDebugLog() = launch {
        log(TAG) { "forceStopDebugLog()" }
        val result = debugSessionManager.forceStopRecording()
        if (result != null) {
            events.tryEmit(Event.OpenRecorderActivity(result.sessionId, result.logDir.path))
        }
    }

    fun openSession(sessionId: String) = launch {
        events.tryEmit(Event.OpenRecorderActivity(sessionId, null))
    }

    fun deleteSession(sessionId: String) = launch {
        log(TAG) { "deleteSession($sessionId)" }
        debugSessionManager.deleteSession(sessionId)
    }

    fun deleteAllSessions() = launch {
        log(TAG) { "deleteAllSessions()" }
        debugSessionManager.deleteAllSessions()
    }

    fun refreshSessions() {
        debugSessionManager.refresh()
    }

    companion object {
        private val TAG = logTag("Settings", "Support", "VM")
    }
}
