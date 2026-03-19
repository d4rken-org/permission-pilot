package eu.darken.myperm.watcher.core

import eu.darken.myperm.apps.core.Pkg
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.os.UserHandle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.dao.PendingSnapshotEventDao
import eu.darken.myperm.common.room.entity.PendingSnapshotEventEntity
import eu.darken.myperm.common.room.snapshot.SnapshotWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PackageChangeReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun pendingSnapshotEventDao(): PendingSnapshotEventDao
        @AppScope fun appScope(): CoroutineScope
    }

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = Pkg.Name(intent.data?.schemeSpecificPart ?: return)
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        val eventType = when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> if (isReplacing) WatcherEventType.UPDATE else WatcherEventType.INSTALL
            Intent.ACTION_PACKAGE_REMOVED -> if (isReplacing) return else WatcherEventType.REMOVED
            else -> return
        }

        val uid = intent.getIntExtra(Intent.EXTRA_UID, -1)
        val userHandleId = if (uid != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            UserHandle.getUserHandleForUid(uid).hashCode()
        } else {
            Process.myUserHandle().hashCode()
        }

        log(TAG) { "onReceive: $eventType for $packageName (userHandle=$userHandleId)" }

        val pendingResult = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java,
        )
        val dao = entryPoint.pendingSnapshotEventDao()

        entryPoint.appScope().launch {
            try {
                dao.insert(
                    PendingSnapshotEventEntity(
                        packageName = packageName,
                        eventType = eventType,
                        userHandleId = userHandleId,
                        createdAt = System.currentTimeMillis(),
                    )
                )

                // Starvation check: if events have been waiting too long, don't debounce further
                val oldestEvent = dao.getOldestCreatedAt()
                val delay = if (oldestEvent != null && System.currentTimeMillis() - oldestEvent > STARVATION_THRESHOLD_MS) {
                    log(TAG) { "Starvation detected, using immediate delay" }
                    0L
                } else {
                    DEBOUNCE_DELAY_MS
                }

                val request = OneTimeWorkRequestBuilder<SnapshotWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    SnapshotWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
            } catch (e: Exception) {
                log(TAG, WARN) { "Failed to process event: ${e.asLog()}" }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 5_000L
        private const val STARVATION_THRESHOLD_MS = 30_000L
        private val TAG = logTag("Watcher", "PackageChangeReceiver")
    }
}
