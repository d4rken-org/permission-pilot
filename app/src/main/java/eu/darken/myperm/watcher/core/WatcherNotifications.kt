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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatcherNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat,
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

    fun postChangeNotification(
        reportId: Long,
        appLabel: String?,
        packageName: String,
        diff: PermissionDiff,
    ) {
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

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_REPORT_ID, reportId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reportId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_bug_report_24)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(packageName.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "channel_permission_watcher"
        const val EXTRA_REPORT_ID = "watcher_report_id"
        private val TAG = logTag("Watcher", "Notifications")
    }
}
