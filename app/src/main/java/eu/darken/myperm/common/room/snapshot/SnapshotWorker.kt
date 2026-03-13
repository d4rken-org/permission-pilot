package eu.darken.myperm.common.room.snapshot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.room.entity.TriggerReason
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.dao.PendingSnapshotEventDao
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.watcher.core.PermissionWatcherWorker

@HiltWorker
class SnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appRepo: AppRepo,
    private val pendingEventDao: PendingSnapshotEventDao,
    private val snapshotDao: SnapshotDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        log(TAG) { "doWork() started" }

        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to set foreground: ${e.asLog()}" }
        }

        // TTL cleanup: discard stale events older than 1 hour
        pendingEventDao.deleteOlderThan(System.currentTimeMillis() - TTL_MS)

        val pendingEvents = pendingEventDao.getAll()
        if (pendingEvents.isEmpty()) {
            log(TAG) { "No pending events, skipping" }
            enqueueWatcher()
            return Result.success()
        }

        val maxId = pendingEvents.last().id

        val oldestEventTime = pendingEvents.first().createdAt
        val latestSnapshot = snapshotDao.getLatestSnapshot()

        if (latestSnapshot != null && latestSnapshot.createdAt >= oldestEventTime) {
            log(TAG) { "Snapshot ${latestSnapshot.snapshotId} already covers pending events, skipping scan" }
        } else {
            log(TAG) { "Processing ${pendingEvents.size} pending events (maxId=$maxId), scanning packages" }
            appRepo.scanAndSave(TriggerReason.PACKAGE_CHANGE)
        }

        pendingEventDao.deleteByMaxId(maxId)

        enqueueWatcher()
        log(TAG) { "doWork() completed" }
        return Result.success()
    }

    private fun enqueueWatcher() = enqueueWatcher(WorkManager.getInstance(applicationContext))

    private fun createForegroundInfo(): ForegroundInfo {
        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_bug_report_24)
            .setContentTitle(applicationContext.getString(R.string.snapshot_sync_notification_title))
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.snapshot_sync_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = applicationContext.getString(R.string.snapshot_sync_channel_description)
            }
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val WORK_NAME = "snapshot_sync"
        const val WATCHER_WORK_NAME = "permission_watcher"
        private const val CHANNEL_ID = "channel_snapshot_sync"
        private const val NOTIFICATION_ID = 42_001
        private const val TTL_MS = 60 * 60 * 1000L // 1 hour
        internal val TAG = logTag("Snapshot", "Worker")

        fun enqueueWatcher(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<PermissionWatcherWorker>().build()
            workManager.enqueueUniqueWork(WATCHER_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
            log(TAG) { "Enqueued PermissionWatcherWorker" }
        }
    }
}
