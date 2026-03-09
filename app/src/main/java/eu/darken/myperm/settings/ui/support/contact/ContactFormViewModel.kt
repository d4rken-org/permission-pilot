package eu.darken.myperm.settings.ui.support.contact

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.R
import eu.darken.myperm.common.BuildConfigWrap
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
import eu.darken.myperm.common.support.EmailTool
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import javax.inject.Inject

@HiltViewModel
class ContactFormViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val debugSessionManager: DebugSessionManager,
    private val emailTool: EmailTool,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider) {

    enum class Category { QUESTION, FEATURE, BUG }

    data class State(
        val category: Category = Category.QUESTION,
        val description: String = "",
        val expectedBehavior: String = "",
        val isSending: Boolean = false,
        val isRecording: Boolean = false,
        val sessions: List<DebugSession.Ready> = emptyList(),
        val selectedSessionId: String? = null,
    ) {
        val isBug: Boolean get() = category == Category.BUG
        val descriptionWords: Int get() = countWords(description)
        val expectedWords: Int get() = countWords(expectedBehavior)
        val canSend: Boolean
            get() = meetsMinimum(description, MIN_WORDS)
                    && (!isBug || meetsMinimum(expectedBehavior, MIN_WORDS_EXPECTED))
                    && !isSending
                    && !isRecording
    }

    sealed interface Event {
        data class OpenEmail(val intent: Intent) : Event
        data class ShowSnackbar(val message: String) : Event
        data object ShowConsentDialog : Event
        data object ShowShortRecordingWarning : Event
    }

    val events = SingleEventFlow<Event>()

    private val stater = DynamicStateFlow(TAG, vmScope) { State() }
    val state: Flow<State> = stater.flow

    @Volatile
    private var autoSelectSessionId: String? = null

    init {
        combine(
            debugSessionManager.recorderState,
            debugSessionManager.sessions,
        ) { recorderState, allSessions ->
            val readySessions = allSessions
                .filterIsInstance<DebugSession.Ready>()
                .take(MAX_PICKER_SESSIONS)

            stater.updateBlocking {
                val autoId = autoSelectSessionId
                val newSelectedId = when {
                    autoId != null && readySessions.any { it.id == autoId } -> {
                        autoSelectSessionId = null
                        autoId
                    }
                    selectedSessionId != null && readySessions.any { it.id == selectedSessionId } -> selectedSessionId
                    else -> selectedSessionId
                }
                copy(
                    isRecording = recorderState.isRecording,
                    sessions = readySessions,
                    selectedSessionId = if (readySessions.any { it.id == newSelectedId }) newSelectedId else null,
                )
            }
        }.launchIn(vmScope)
    }

    fun updateCategory(category: Category) = launch {
        stater.updateBlocking { copy(category = category) }
    }

    fun updateDescription(text: String) = launch {
        if (text.length <= MAX_CHARS) {
            stater.updateBlocking { copy(description = text) }
        }
    }

    fun updateExpectedBehavior(text: String) = launch {
        if (text.length <= MAX_CHARS) {
            stater.updateBlocking { copy(expectedBehavior = text) }
        }
    }

    fun selectLogSession(id: String) = launch {
        stater.updateBlocking {
            copy(selectedSessionId = if (selectedSessionId == id) null else id)
        }
    }

    fun deleteLogSession(id: String) = launch {
        log(TAG) { "deleteLogSession($id)" }
        debugSessionManager.deleteSession(id)
        stater.updateBlocking {
            copy(selectedSessionId = if (selectedSessionId == id) null else selectedSessionId)
        }
    }

    fun openPrivacyPolicy() {
        webpageTool.open(PrivacyPolicy.URL)
    }

    fun startRecording() {
        events.tryEmit(Event.ShowConsentDialog)
    }

    fun doStartRecording() = launch {
        log(TAG) { "doStartRecording()" }
        debugSessionManager.startRecording()
    }

    fun stopRecording() = launch {
        val result = debugSessionManager.requestStopRecording()
        when (result) {
            is RecorderModule.StopResult.TooShort -> {
                events.tryEmit(Event.ShowShortRecordingWarning)
            }
            is RecorderModule.StopResult.Stopped -> {
                log(TAG) { "Recording stopped: ${result.sessionId}" }
                autoSelectSessionId = result.sessionId
                debugSessionManager.zipSession(result.sessionId)
            }
            is RecorderModule.StopResult.NotRecording -> {
                log(TAG) { "stopRecording: not recording" }
            }
        }
    }

    fun forceStopRecording() = launch {
        log(TAG) { "forceStopRecording()" }
        val logDir = debugSessionManager.forceStopRecording()
        if (logDir != null) {
            val sessionId = DebugSessionManager.deriveSessionId(logDir)
            autoSelectSessionId = sessionId
            debugSessionManager.zipSession(sessionId)
        }
    }

    fun refreshLogSessions() {
        debugSessionManager.refresh()
    }

    fun confirmSent(sessionId: String?) = launch {
        if (sessionId != null) {
            debugSessionManager.deleteSession(sessionId)
        }
        navUp()
    }

    fun send() = launch {
        val currentState = stater.value()
        if (!currentState.canSend) return@launch

        stater.updateBlocking { copy(isSending = true) }

        try {
            val attachmentUri = if (currentState.isBug) {
                currentState.selectedSessionId?.let { sessionId ->
                    try {
                        val uri = debugSessionManager.getZipUri(sessionId)
                        if (uri == null) {
                            log(TAG) { "getZipUri returned null for session $sessionId" }
                            events.tryEmit(
                                Event.ShowSnackbar(context.getString(R.string.contact_debuglog_zip_error))
                            )
                            return@launch
                        }
                        uri
                    } catch (e: Exception) {
                        log(TAG) { "Failed to prepare attachment: $e" }
                        events.tryEmit(
                            Event.ShowSnackbar(context.getString(R.string.contact_debuglog_zip_error))
                        )
                        return@launch
                    }
                }
            } else {
                null
            }

            val categoryTag = when (currentState.category) {
                Category.QUESTION -> context.getString(R.string.contact_email_tag_question)
                Category.FEATURE -> context.getString(R.string.contact_email_tag_feature)
                Category.BUG -> context.getString(R.string.contact_email_tag_bug)
            }

            val firstWords = currentState.description.trim()
                .split("\\s+".toRegex())
                .take(8)
                .joinToString(" ")
                .replace("\n", " ")
                .replace("\r", "")

            val subject = "[MYPERM][$categoryTag] $firstWords"

            val body = buildString {
                appendLine(currentState.description.trim())
                if (currentState.isBug && currentState.expectedBehavior.isNotBlank()) {
                    appendLine()
                    appendLine(context.getString(R.string.contact_email_section_expected_behavior))
                    appendLine(currentState.expectedBehavior.trim())
                }
                appendLine()
                appendLine(context.getString(R.string.contact_email_section_device_info))
                appendLine("App: ${BuildConfigWrap.VERSION_DESCRIPTION}")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            }

            val intent = emailTool.build(
                EmailTool.Email(
                    recipients = listOf(SupportLinks.SUPPORT_EMAIL),
                    subject = subject,
                    body = body,
                    attachment = attachmentUri,
                )
            )

            events.tryEmit(Event.OpenEmail(intent))
        } finally {
            stater.updateBlocking { copy(isSending = false) }
        }
    }

    companion object {
        private val TAG = logTag("Settings", "Support", "ContactForm", "VM")
        const val MIN_WORDS = 20
        const val MIN_WORDS_EXPECTED = 10
        private const val MAX_CHARS = 5000
        private const val MAX_PICKER_SESSIONS = 3

        fun countWords(text: String): Int {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return 0
            return trimmed.split("\\s+".toRegex()).count { it.isNotBlank() }
        }

        fun meetsMinimum(text: String, minWords: Int): Boolean {
            val words = countWords(text)
            if (words >= minWords) return true
            // CJK fallback: character count >= minWords * 3
            val charCount = text.trim().length
            return charCount >= minWords * 3
        }
    }
}
