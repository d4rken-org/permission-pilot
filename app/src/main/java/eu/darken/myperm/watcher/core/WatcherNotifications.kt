package eu.darken.myperm.watcher.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.R
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.main.ui.MainActivity
import eu.darken.myperm.settings.core.GeneralSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatcherNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat,
    private val generalSettings: GeneralSettings,
) {

    private var channelCreated = false

    private fun ensureChannel() {
        if (channelCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.watcher_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.watcher_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
        channelCreated = true
    }

    suspend fun postChangeNotification(
        reportId: Long,
        appLabel: String?,
        packageName: String,
        diff: PermissionDiff,
    ) {
        if (!generalSettings.isWatcherNotificationsEnabled.value()) {
            log(TAG) { "Notifications disabled, skipping for $packageName" }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (granted != PackageManager.PERMISSION_GRANTED) {
                log(TAG) { "POST_NOTIFICATIONS not granted, skipping notification for $packageName" }
                return
            }
        }

        ensureChannel()

        val displayName = appLabel ?: packageName
        val totalChanges = diff.addedPermissions.size + diff.removedPermissions.size +
                diff.grantChanges.size + diff.addedDeclared.size + diff.removedDeclared.size

        val title = if (diff.addedPermissions.isNotEmpty()) {
            context.resources.getQuantityString(
                R.plurals.watcher_notification_new_permissions,
                diff.addedPermissions.size,
                displayName,
                diff.addedPermissions.size,
            )
        } else {
            context.resources.getQuantityString(
                R.plurals.watcher_notification_changed_permissions,
                totalChanges,
                displayName,
                totalChanges,
            )
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_REPORT_ID, reportId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reportId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val markSeenIntent = Intent(context, WatcherActionReceiver::class.java).apply {
            action = WatcherActionReceiver.ACTION_MARK_SEEN
            putExtra(WatcherActionReceiver.EXTRA_REPORT_ID, reportId)
            putExtra(WatcherActionReceiver.EXTRA_PACKAGE_NAME, packageName)
        }
        val markSeenPendingIntent = PendingIntent.getBroadcast(
            context,
            ("mark_$reportId").hashCode(),
            markSeenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val deleteIntent = Intent(context, WatcherActionReceiver::class.java).apply {
            action = WatcherActionReceiver.ACTION_DELETE
            putExtra(WatcherActionReceiver.EXTRA_REPORT_ID, reportId)
            putExtra(WatcherActionReceiver.EXTRA_PACKAGE_NAME, packageName)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            ("delete_$reportId").hashCode(),
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_bug_report_24)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setGroup(GROUP_KEY)
            .addAction(0, context.getString(R.string.watcher_notification_action_mark_seen), markSeenPendingIntent)
            .addAction(0, context.getString(R.string.watcher_notification_action_delete), deletePendingIntent)
            .build()

        notificationManager.notify(packageName.hashCode(), notification)
    }

    suspend fun postSummaryNotification(reportCount: Int) {
        if (!generalSettings.isWatcherNotificationsEnabled.value()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (granted != PackageManager.PERMISSION_GRANTED) return
        }

        ensureChannel()

        val title = context.resources.getQuantityString(
            R.plurals.watcher_notification_summary_title,
            reportCount,
            reportCount,
        )

        val summaryIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val summaryPendingIntent = PendingIntent.getActivity(
            context,
            SUMMARY_NOTIFICATION_ID,
            summaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_bug_report_24)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentIntent(summaryPendingIntent)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setStyle(NotificationCompat.InboxStyle())
            .build()

        notificationManager.notify(SUMMARY_NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "channel_permission_watcher"
        const val EXTRA_REPORT_ID = "watcher_report_id"
        private const val GROUP_KEY = "permission_watcher_reports"
        private const val SUMMARY_NOTIFICATION_ID = 42_100
        private val TAG = logTag("Watcher", "Notifications")
    }
}
