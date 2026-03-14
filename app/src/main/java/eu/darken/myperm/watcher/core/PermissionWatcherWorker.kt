package eu.darken.myperm.watcher.core

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag

@HiltWorker
class PermissionWatcherWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val watcherManager: WatcherManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            watcherManager.processChanges()
            Result.success()
        } catch (e: Exception) {
            log(TAG, WARN) { "Error during snapshot diff: ${e.asLog()}" }
            Result.retry()
        }
    }

    companion object {
        private val TAG = logTag("Watcher", "Worker")
    }
}
