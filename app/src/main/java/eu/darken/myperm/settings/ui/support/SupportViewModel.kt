package eu.darken.myperm.settings.ui.support

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.SupportLinks
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.uix.ViewModel4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val webpageTool: WebpageTool,
    private val recorderModule: RecorderModule,
) : ViewModel4(dispatcherProvider) {

    val recorderState: Flow<RecorderModule.State> = recorderModule.state

    data class StoppedRecording(val path: String, val startedAt: Long)

    val stoppedRecording = SingleEventFlow<StoppedRecording>()

    private val refreshTrigger = MutableStateFlow(0)

    data class LogFolderStats(
        val fileCount: Int = 0,
        val totalSize: Long = 0L,
    )

    val logFolderStats: Flow<LogFolderStats> = combine(
        refreshTrigger,
        recorderModule.state.map { it.isRecording },
    ) { _, _ ->
        val files = recorderModule.getLogFiles()
        LogFolderStats(
            fileCount = files.size,
            totalSize = files.sumOf { it.length() },
        )
    }

    fun openIssueTracker() {
        webpageTool.open(SupportLinks.GITHUB_ISSUES_URL)
    }

    fun openDiscord() {
        webpageTool.open(SupportLinks.DISCORD_URL)
    }

    fun navigateToContactForm() {
        navTo(Nav.Settings.ContactForm)
    }

    fun startDebugLog() = launch {
        recorderModule.startRecorder()
    }

    fun stopDebugLog() = launch {
        val startedAt = recorderModule.state.first().recordingStartedAt
        val path = recorderModule.stopRecorder()
        log(TAG) { "stopDebugLog(): path=$path" }
        refreshTrigger.value++
        path?.let { stoppedRecording.emit(StoppedRecording(it.path, startedAt)) }
    }

    fun clearDebugLogs() = launch {
        recorderModule.deleteAllLogFiles()
        log(TAG) { "clearDebugLogs(): done" }
        refreshTrigger.value++
    }

    companion object {
        private val TAG = logTag("Settings", "Support", "VM")
    }
}
