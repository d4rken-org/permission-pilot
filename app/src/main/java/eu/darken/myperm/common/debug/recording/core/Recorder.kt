package eu.darken.myperm.common.debug.recording.core

import eu.darken.myperm.common.debug.logging.FileLogger
import eu.darken.myperm.common.debug.logging.Logging
import eu.darken.myperm.common.debug.logging.Logging.Priority.INFO
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class Recorder @Inject constructor() {
    private val mutex = Mutex()
    private var fileLogger: FileLogger? = null

    val isRecording: Boolean
        get() = path != null

    var path: File? = null
        private set

    // Nothing is published until the logger actually writes: a failed start leaves neither an
    // installed logger nor a recorder that claims to be recording.
    suspend fun start(path: File) = mutex.withLock {
        if (fileLogger != null) return@withLock
        val logger = FileLogger(path)
        logger.start()
        try {
            Logging.install(logger)
            fileLogger = logger
            this.path = path
            log(TAG, INFO) { "Now logging to file!" }
        } catch (e: Throwable) {
            // Publication is announced through the loggers that are ALREADY installed, so one of
            // them throwing lands here with this logger installed and open while nothing references
            // it: the caller's rollback would find a recorder that never started. Undo it here.
            try {
                Logging.remove(logger)
            } catch (inner: Throwable) {
                if (inner !== e) e.addSuppressed(inner)
            }
            try {
                logger.stop()
            } catch (inner: Throwable) {
                if (inner !== e) e.addSuppressed(inner)
            }
            fileLogger = null
            this.path = null
            throw e
        }
    }

    /**
     * Teardown has to complete once it starts: this runs on the rollback path of a failed start,
     * where a skipped step means a globally installed logger writing into a session nobody tracks.
     * Every step is therefore guaranteed, and none of them may be cancelled halfway.
     */
    suspend fun stop() = mutex.withLock {
        withContext(NonCancellable) {
            val logger = fileLogger ?: return@withContext
            try {
                log(TAG, INFO) { "Stopping file-logger-tree: $logger" }
            } finally {
                try {
                    Logging.remove(logger)
                } finally {
                    try {
                        logger.stop()
                    } finally {
                        fileLogger = null
                        this@Recorder.path = null
                    }
                }
            }
        }
    }

    companion object {
        internal val TAG = logTag("Debug", "Log", "eu.darken.androidstarter.common.debug.recording.core.Recorder")
    }

}