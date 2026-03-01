package eu.darken.myperm.common.debug.recording.core

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.DynamicStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecorderModule @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) {

    private val triggerFile = try {
        File(context.getExternalFilesDir(null), FORCE_FILE)
    } catch (e: Exception) {
        File(
            Environment.getExternalStorageDirectory(),
            "/Android/data/${BuildConfigWrap.APPLICATION_ID}/files/$FORCE_FILE"
        )
    }

    private val internalState = DynamicStateFlow(TAG, appScope + dispatcherProvider.IO) {
        val triggerFileExists = triggerFile.exists()
        State(shouldRecord = triggerFileExists)
    }
    val state: Flow<State> = internalState.flow

    init {
        internalState.flow
            .onEach {
                log(TAG) { "New Recorder state: $internalState" }

                internalState.updateBlocking {
                    if (!isRecording && shouldRecord) {
                        val newRecorder = Recorder()
                        newRecorder.start(createRecordingFilePath())
                        triggerFile.createNewFile()

                        copy(
                            recorder = newRecorder,
                            recordingStartedAt = System.currentTimeMillis(),
                        )
                    } else if (!shouldRecord && isRecording) {
                        val currentLog = recorder!!.path!!
                        recorder.stop()

                        if (triggerFile.exists() && !triggerFile.delete()) {
                            log(TAG, ERROR) { "Failed to delete trigger file" }
                        }

                        copy(
                            recorder = null,
                            lastLogPath = currentLog
                        )
                    } else {
                        this
                    }
                }
            }
            .launchIn(appScope)
    }

    private fun createRecordingFilePath() = File(
        File(context.cacheDir, "debug/logs"),
        "${BuildConfigWrap.APPLICATION_ID}_logfile_${System.currentTimeMillis()}.log"
    )

    suspend fun startRecorder(): File {
        internalState.updateBlocking {
            copy(shouldRecord = true)
        }
        return internalState.flow.filter { it.isRecording }.first().currentLogPath!!
    }

    suspend fun stopRecorder(): File? {
        val currentPath = internalState.value().currentLogPath ?: return null
        internalState.updateBlocking {
            copy(shouldRecord = false)
        }
        internalState.flow.filter { !it.isRecording }.first()
        return currentPath
    }

    private val logDir = File(context.cacheDir, "debug/logs")

    fun getLogFiles(): List<File> {
        if (!logDir.exists()) return emptyList()
        return logDir.listFiles()
            ?.filter { it.extension == "log" || it.extension == "zip" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    suspend fun deleteLogFile(file: File) {
        val activePath = internalState.value().currentLogPath
        if (file == activePath) {
            log(TAG, WARN) { "deleteLogFile(): Skipping active recording file: $file" }
            return
        }
        file.delete()
        // Delete corresponding zip or log
        when (file.extension) {
            "log" -> File(file.path + ".zip").delete()
            "zip" -> File(file.path.removeSuffix(".zip")).delete()
        }
        log(TAG) { "deleteLogFile(): Deleted $file" }
    }

    suspend fun deleteAllLogFiles() {
        val activePath = internalState.value().currentLogPath
        getLogFiles().forEach { file ->
            if (file != activePath) {
                file.delete()
                log(TAG) { "deleteAllLogFiles(): Deleted $file" }
            }
        }
    }

    data class State(
        val shouldRecord: Boolean = false,
        internal val recorder: Recorder? = null,
        val lastLogPath: File? = null,
        val recordingStartedAt: Long = 0L,
    ) {
        val isRecording: Boolean
            get() = recorder != null

        val currentLogPath: File?
            get() = recorder?.path
    }

    companion object {
        internal val TAG = logTag("Debug", "Log", "Recorder", "Module")
        private const val FORCE_FILE = "force_debug_run"
    }
}
