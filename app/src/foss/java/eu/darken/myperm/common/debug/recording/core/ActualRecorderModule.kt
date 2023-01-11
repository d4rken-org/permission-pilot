package eu.darken.myperm.common.debug.recording.core

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.debug.recording.core.RecorderModule.State
import eu.darken.myperm.common.debug.recording.ui.RecorderActivity
import eu.darken.myperm.common.flow.DynamicStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActualRecorderModule @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) : RecorderModule {

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
        State(isAvailable = true, shouldRecord = triggerFileExists)
    }
    override val state: Flow<State> = internalState.flow

    init {
        internalState.flow
            .onEach {
                log(TAG) { "New Recorder state: $internalState" }

                internalState.updateBlocking {
                    if (!isRecording && shouldRecord) {
                        val newRecorder = Recorder()
                        newRecorder.start(createRecordingFilePath())
                        triggerFile.createNewFile()

                        ContextCompat.startForegroundService(context, Intent(context, RecorderService::class.java))

                        copy(
                            recorder = newRecorder
                        )
                    } else if (!shouldRecord && isRecording) {
                        val currentLog = recorder!!.path!!
                        recorder.stop()

                        if (triggerFile.exists() && !triggerFile.delete()) {
                            log(TAG, ERROR) { "Failed to delete trigger file" }
                        }

                        val intent = RecorderActivity.getLaunchIntent(context, currentLog.path).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)

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

    override suspend fun startRecorder(): File {
        internalState.updateBlocking {
            copy(shouldRecord = true)
        }
        return internalState.flow.filter { it.isRecording }.first().currentLogPath!!
    }

    override suspend fun stopRecorder(): File? {
        val currentPath = internalState.value().currentLogPath ?: return null
        internalState.updateBlocking {
            copy(shouldRecord = false)
        }
        internalState.flow.filter { !it.isRecording }.first()
        return currentPath
    }

    companion object {
        internal val TAG = logTag("Debug", "Log", "Recorder", "Module")
        private const val FORCE_FILE = "force_debug_run"
    }
}