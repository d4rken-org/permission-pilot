package eu.darken.myperm.settings.ui.support.contact

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
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
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import eu.darken.myperm.common.flow.DynamicStateFlow
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.support.DebugLogZipper
import eu.darken.myperm.common.support.EmailTool
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ContactFormViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val recorderModule: RecorderModule,
    private val emailTool: EmailTool,
    private val debugLogZipper: DebugLogZipper,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider) {

    enum class Category { QUESTION, FEATURE, BUG }

    data class LogSessionItem(
        val path: File,
        val size: Long,
        val lastModified: Long,
    )

    data class State(
        val category: Category = Category.QUESTION,
        val description: String = "",
        val expectedBehavior: String = "",
        val isSending: Boolean = false,
        val isRecording: Boolean = false,
        val recordingStartedAt: Long = 0L,
        val sessions: List<LogSessionItem> = emptyList(),
        val selectedSessionPath: File? = null,
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

    private val stater = DynamicStateFlow(TAG, vmScope) {
        State(sessions = loadLogSessions())
    }
    val state: Flow<State> = stater.flow

    init {
        recorderModule.state
            .onEach { recorderState ->
                stater.updateBlocking {
                    copy(
                        isRecording = recorderState.isRecording,
                        recordingStartedAt = recorderState.recordingStartedAt,
                        sessions = loadLogSessions(activeDir = recorderState.currentLogDir),
                    )
                }
            }
            .launchIn(vmScope)
    }

    private fun loadLogSessions(activeDir: File? = null): List<LogSessionItem> {
        return recorderModule.getLogDirectories()
            .flatMap { dir ->
                if (!dir.exists()) return@flatMap emptyList()
                val entries = dir.listFiles() ?: return@flatMap emptyList()
                entries.filter { it != activeDir && (it.isDirectory || (it.isFile && it.extension == "zip")) }
                    .map { entry ->
                        val size = if (entry.isDirectory) {
                            entry.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                        } else {
                            entry.length()
                        }
                        LogSessionItem(
                            path = entry,
                            size = size,
                            lastModified = entry.lastModified(),
                        )
                    }
            }
            .sortedByDescending { it.lastModified }
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

    fun selectLogSession(path: File) = launch {
        stater.updateBlocking {
            copy(selectedSessionPath = if (selectedSessionPath == path) null else path)
        }
    }

    fun deleteLogSession(path: File) = launch {
        log(TAG) { "deleteLogSession($path)" }
        if (path.isDirectory) {
            path.deleteRecursively()
            val zip = File(path.parentFile, "${path.name}.zip")
            if (zip.exists()) zip.delete()
        } else {
            path.delete()
        }
        stater.updateBlocking {
            copy(
                sessions = loadLogSessions(),
                selectedSessionPath = if (selectedSessionPath == path) null else selectedSessionPath,
            )
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
        recorderModule.startRecorder()
    }

    fun stopRecording() = launch {
        val current = stater.value()
        val duration = SystemClock.elapsedRealtime() - current.recordingStartedAt
        if (duration < SHORT_RECORDING_THRESHOLD) {
            events.tryEmit(Event.ShowShortRecordingWarning)
            return@launch
        }
        log(TAG) { "stopRecording()" }
        recorderModule.stopRecorder(showResultUi = false)
        stater.updateBlocking { copy(sessions = loadLogSessions()) }
    }

    fun forceStopRecording() = launch {
        log(TAG) { "forceStopRecording()" }
        recorderModule.stopRecorder(showResultUi = false)
        stater.updateBlocking { copy(sessions = loadLogSessions()) }
    }

    fun send() = launch {
        val currentState = stater.value()
        if (!currentState.canSend) return@launch

        stater.updateBlocking { copy(isSending = true) }

        try {
            val attachmentUri = if (currentState.isBug) {
                currentState.selectedSessionPath?.let { sessionPath ->
                    try {
                        if (sessionPath.isDirectory) {
                            debugLogZipper.zipAndGetUri(sessionPath)
                        } else if (sessionPath.extension == "zip" && sessionPath.exists()) {
                            debugLogZipper.getUriForZip(sessionPath)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        log(TAG) { "Failed to prepare attachment: $e" }
                        events.tryEmit(
                            Event.ShowSnackbar(context.getString(R.string.contact_debuglog_zip_error))
                        )
                        null
                    }
                }
            } else {
                null
            }

            val categoryTag = when (currentState.category) {
                Category.QUESTION -> "Question"
                Category.FEATURE -> "Feature"
                Category.BUG -> "Bug"
            }

            val firstWords = currentState.description.trim()
                .split("\\s+".toRegex())
                .take(8)
                .joinToString(" ")

            val subject = "[MYPERM][$categoryTag] $firstWords"

            val body = buildString {
                appendLine(currentState.description.trim())
                if (currentState.isBug && currentState.expectedBehavior.isNotBlank()) {
                    appendLine()
                    appendLine("--- Expected Behavior ---")
                    appendLine(currentState.expectedBehavior.trim())
                }
                appendLine()
                appendLine("--- Device Info ---")
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
        private const val SHORT_RECORDING_THRESHOLD = 5_000L

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
