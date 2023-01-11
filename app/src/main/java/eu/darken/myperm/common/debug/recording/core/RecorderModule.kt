package eu.darken.myperm.common.debug.recording.core

import kotlinx.coroutines.flow.Flow
import java.io.File

interface RecorderModule {

    val state: Flow<State>

    suspend fun startRecorder(): File

    suspend fun stopRecorder(): File?

    data class State(
        val isAvailable: Boolean,
        val shouldRecord: Boolean = false,
        internal val recorder: Recorder? = null,
        val lastLogPath: File? = null,
    ) {
        val isRecording: Boolean
            get() = recorder != null

        val currentLogPath: File?
            get() = recorder?.path
    }
}