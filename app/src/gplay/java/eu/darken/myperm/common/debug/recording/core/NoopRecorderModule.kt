package eu.darken.myperm.common.debug.recording.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoopRecorderModule @Inject constructor() : RecorderModule {
    override val state: Flow<RecorderModule.State> = flowOf(RecorderModule.State(isAvailable = false))

    override suspend fun startRecorder(): File {
        throw NotImplementedError()
    }

    override suspend fun stopRecorder(): File? {
        throw NotImplementedError()
    }
}
