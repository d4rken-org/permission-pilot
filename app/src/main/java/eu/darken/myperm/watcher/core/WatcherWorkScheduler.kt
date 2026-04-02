package eu.darken.myperm.watcher.core

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.settings.core.GeneralSettings
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatcherWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val generalSettings: GeneralSettings,
) {

    suspend fun ensureScheduled() {
        val isEnabled = generalSettings.isWatcherEnabled.value()
        if (isEnabled) {
            val intervalHours = generalSettings.watcherPollingIntervalHours.value()
            enqueue(intervalHours, ExistingPeriodicWorkPolicy.KEEP)
        } else {
            cancel()
        }
    }

    fun reschedule(intervalHours: Int) {
        log(TAG) { "reschedule(intervalHours=$intervalHours)" }
        enqueue(intervalHours, ExistingPeriodicWorkPolicy.UPDATE)
    }

    fun cancel() {
        log(TAG) { "cancel()" }
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private fun enqueue(intervalHours: Int, policy: ExistingPeriodicWorkPolicy) {
        log(TAG) { "enqueue(intervalHours=$intervalHours, policy=$policy)" }
        // Explicit: work should run even on low battery (this is the default,
        // but documenting intent prevents future regressions)
        val request = PeriodicWorkRequestBuilder<PermissionPollWorker>(
            intervalHours.toLong(), TimeUnit.HOURS,
        ).setConstraints(
            Constraints.Builder().setRequiresBatteryNotLow(false).build()
        ).build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, policy, request)
    }

    companion object {
        private const val WORK_NAME = "permission_poll"
        private const val WATCHER_WORK_NAME = "permission_watcher"
        private val TAG = logTag("Watcher", "WorkScheduler")

        fun enqueueWatcher(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<PermissionWatcherWorker>().build()
            workManager.enqueueUniqueWork(WATCHER_WORK_NAME, ExistingWorkPolicy.KEEP, request)
            log(TAG) { "Enqueued PermissionWatcherWorker" }
        }
    }
}
