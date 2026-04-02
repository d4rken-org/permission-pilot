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
import eu.darken.myperm.common.room.entity.TriggerReason
import eu.darken.myperm.settings.core.GeneralSettings

@HiltWorker
class PermissionPollWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val watcherManager: WatcherManager,
    private val generalSettings: GeneralSettings,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        log(TAG) { "doWork() started" }

        val isEnabled = generalSettings.isWatcherEnabled.value()
        if (!isEnabled) {
            log(TAG) { "Watcher is disabled, skipping" }
            return Result.success()
        }

        return try {
            watcherManager.scanDiffAndPrune(TriggerReason.PERMISSION_POLL)
            generalSettings.watcherLastSuccessfulPollAt.value(System.currentTimeMillis())
            log(TAG) { "doWork() completed" }
            Result.success()
        } catch (e: Exception) {
            log(TAG, WARN) { "Error during permission poll: ${e.asLog()}" }
            Result.retry()
        }
    }

    companion object {
        internal val TAG = logTag("Watcher", "Poll", "Worker")
    }
}
