package eu.darken.myperm.settings.ui.support.contact

import android.content.Intent
import android.os.Build
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.support.DebugLogZipper
import eu.darken.myperm.common.support.EmailTool
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ContactFormViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val recorderModule: RecorderModule,
    private val emailTool: EmailTool,
    private val debugLogZipper: DebugLogZipper,
) : ViewModel4(dispatcherProvider) {

    enum class Category { QUESTION, FEATURE, BUG }

    val category = MutableStateFlow(Category.QUESTION)
    val description = MutableStateFlow("")
    val expectedBehavior = MutableStateFlow("")
    val selectedLogFile = MutableStateFlow<File?>(null)

    val emailEvent = SingleEventFlow<Intent>()

    private val refreshTrigger = MutableStateFlow(0)

    val isRecording: Flow<Boolean> = recorderModule.state.map { it.isRecording }

    val logFiles: Flow<List<File>> = combine(
        refreshTrigger,
        recorderModule.state,
    ) { _, state ->
        recorderModule.getLogFiles()
            .filter { it.extension == "log" }
            .filter { it != state.currentLogPath }
    }

    val descriptionWordCount: Flow<Int> = description.map { countWords(it) }
    val expectedBehaviorWordCount: Flow<Int> = expectedBehavior.map { countWords(it) }

    private val isSending = MutableStateFlow(false)

    val canSend: Flow<Boolean> = combine(
        combine(description, expectedBehavior, category) { desc, expected, cat ->
            Triple(desc, expected, cat)
        },
        isRecording,
        isSending,
    ) { (desc, expected, cat), recording, sending ->
        val descOk = meetsMinimum(desc, MIN_WORDS)
        val expectedOk = cat != Category.BUG || meetsMinimum(expected, MIN_WORDS_EXPECTED)
        descOk && expectedOk && !recording && !sending
    }

    fun setCategory(cat: Category) {
        category.value = cat
    }

    fun setDescription(text: String) {
        description.value = text
    }

    fun setExpectedBehavior(text: String) {
        expectedBehavior.value = text
    }

    fun selectLogFile(file: File?) {
        selectedLogFile.value = file
    }

    fun deleteLogFile(file: File) = launch {
        if (selectedLogFile.value == file) selectedLogFile.value = null
        recorderModule.deleteLogFile(file)
        refreshTrigger.value++
        log(TAG) { "deleteLogFile(): $file" }
    }

    fun startRecording() = launch {
        recorderModule.startRecorder()
    }

    fun stopRecording() = launch {
        recorderModule.stopRecorder()
        refreshTrigger.value++
        log(TAG) { "stopRecording(): done" }
    }

    fun send() = launch {
        isSending.value = true
        try {
            val cat = category.value
            val desc = description.value.trim()
            val expected = expectedBehavior.value.trim()
            val logFile = selectedLogFile.value

            val categoryTag = when (cat) {
                Category.QUESTION -> "Question"
                Category.FEATURE -> "Feature"
                Category.BUG -> "Bug"
            }

            val firstWords = desc.split("\\s+".toRegex()).take(8).joinToString(" ")
            val subject = "[MYPERM][$categoryTag] $firstWords"

            val body = buildString {
                appendLine(desc)
                if (cat == Category.BUG && expected.isNotBlank()) {
                    appendLine()
                    appendLine("--- Expected Behavior ---")
                    appendLine(expected)
                }
                appendLine()
                appendLine("--- Device Info ---")
                appendLine("App: ${BuildConfigWrap.VERSION_DESCRIPTION}")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            }

            val attachmentUri = if (logFile != null && logFile.exists()) {
                try {
                    debugLogZipper.zipAndGetUri(logFile)
                } catch (e: Exception) {
                    log(TAG) { "Failed to zip log file: $e" }
                    null
                }
            } else null

            val intent = emailTool.build(
                recipient = "support@darken.eu",
                subject = subject,
                body = body,
                attachmentUri = attachmentUri,
            )

            emailEvent.emit(intent)
        } finally {
            isSending.value = false
        }
    }

    companion object {
        private val TAG = logTag("Settings", "Support", "ContactForm", "VM")
        const val MIN_WORDS = 20
        const val MIN_WORDS_EXPECTED = 10

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
