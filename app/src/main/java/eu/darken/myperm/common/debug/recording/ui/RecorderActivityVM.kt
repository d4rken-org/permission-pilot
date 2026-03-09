package eu.darken.myperm.common.debug.recording.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.R
import eu.darken.myperm.common.PrivacyPolicy
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.debug.recording.core.DebugSession
import eu.darken.myperm.common.debug.recording.core.DebugSessionManager
import eu.darken.myperm.common.flow.DynamicStateFlow
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.uix.ViewModel2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecorderActivityVM @Inject constructor(
    handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val sessionManager: DebugSessionManager,
    private val webpageTool: WebpageTool,
) : ViewModel2(dispatcherProvider) {

    data class LogEntry(
        val file: File,
        val size: Long,
    )

    data class State(
        val logDir: File? = null,
        val logEntries: List<LogEntry> = emptyList(),
        val compressedSize: Long = -1L,
        val recordingDurationSecs: Long = 0L,
        val isWorking: Boolean = true,
    )

    sealed interface Event {
        data class ShareIntent(val intent: Intent) : Event
        data object Finish : Event
    }

    private val sessionId: String? = handle.get<String>(RecorderActivity.RECORD_SESSION_ID)
    private val legacyPath: String? = handle.get<String>(RecorderActivity.RECORD_PATH)

    private val stater = DynamicStateFlow(TAG, vmScope + dispatcherProvider.IO) {
        val resolved = resolveSession()
        if (resolved == null) {
            return@DynamicStateFlow State(logDir = null, isWorking = false)
        }

        val logDir = resolved.logDir
        val files = if (logDir != null) {
            logDir.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
        } else {
            listOfNotNull(resolved.zipFile)
        }
        val entries = files.map { LogEntry(it, it.length()) }

        val durationSecs = if (logDir != null) {
            val dirCreated = logDir.lastModified()
            val latestFileModified = files.maxOfOrNull { it.lastModified() } ?: dirCreated
            ((latestFileModified - dirCreated) / 1000).coerceAtLeast(0)
        } else {
            0L
        }

        State(
            logDir = logDir,
            logEntries = entries,
            compressedSize = resolved.compressedSize,
            recordingDurationSecs = durationSecs,
            isWorking = resolved.compressedSize < 0L,
        )
    }
    val state = stater.flow

    val events = SingleEventFlow<Event>()

    init {
        // Watch for compression completion
        sessionManager.sessions
            .onEach { allSessions ->
                val sid = sessionId ?: return@onEach
                val session = allSessions.firstOrNull { it.id == sid }
                when (session) {
                    is DebugSession.Ready -> {
                        stater.updateBlocking {
                            copy(
                                compressedSize = session.compressedSize,
                                isWorking = false,
                            )
                        }
                    }
                    is DebugSession.Failed -> {
                        stater.updateBlocking {
                            copy(isWorking = false)
                        }
                    }
                    is DebugSession.Compressing -> {
                        stater.updateBlocking {
                            copy(isWorking = true)
                        }
                    }
                    else -> {}
                }
            }
            .launchIn(vmScope)

        // Auto-zip if not yet zipped
        if (sessionId != null) {
            launch {
                sessionManager.zipSession(sessionId)
            }
        }
    }

    private suspend fun resolveSession(): DebugSession.Ready? {
        if (sessionId != null) {
            val sessions = sessionManager.sessions.first()
            val session = sessions.firstOrNull { it.id == sessionId }
            if (session is DebugSession.Ready) return session
            // If not yet ready (e.g. compressing), build a placeholder from legacy path
            if (legacyPath != null) {
                val dir = File(legacyPath)
                if (dir.exists()) {
                    return DebugSession.Ready(
                        id = sessionId,
                        displayName = dir.name,
                        createdAt = DebugSessionManager.parseCreatedAt(dir.name),
                        diskSize = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
                        logDir = dir,
                    )
                }
            }
        }
        // Legacy path fallback
        if (legacyPath != null) {
            val dir = File(legacyPath)
            if (dir.exists()) {
                val id = DebugSessionManager.deriveSessionId(dir)
                return DebugSession.Ready(
                    id = id,
                    displayName = dir.name,
                    createdAt = DebugSessionManager.parseCreatedAt(dir.name),
                    diskSize = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
                    logDir = dir,
                )
            }
        }
        return null
    }

    fun share() = launch {
        val sid = sessionId ?: legacyPath?.let { DebugSessionManager.deriveSessionId(File(it)) } ?: return@launch

        stater.updateBlocking { copy(isWorking = true) }

        try {
            val uri = try {
                sessionManager.getZipUri(sid)
            } catch (e: Exception) {
                log(TAG, WARN) { "Failed to get zip URI for session $sid: $e" }
                null
            }
            if (uri == null) {
                log(TAG, WARN) { "Failed to get zip URI for session $sid" }
                return@launch
            }

            val currentState = stater.flow.first()
            val dirName = currentState.logDir?.name ?: sid

            val intent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "application/zip"
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.support_debuglog_share_subject, dirName))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(intent, context.getString(R.string.support_debuglog_label))
            events.tryEmit(Event.ShareIntent(chooserIntent))
        } finally {
            stater.updateBlocking { copy(isWorking = false) }
        }
    }

    fun keep() {
        events.tryEmit(Event.Finish)
    }

    fun discard() = launch {
        val sid = sessionId ?: legacyPath?.let { DebugSessionManager.deriveSessionId(File(it)) } ?: return@launch
        sessionManager.deleteSession(sid)
        events.tryEmit(Event.Finish)
    }

    fun goPrivacyPolicy() {
        webpageTool.open(PrivacyPolicy.URL)
    }

    companion object {
        private val TAG = logTag("Debug", "Recorder", "VM")
    }
}
