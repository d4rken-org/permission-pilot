package eu.darken.myperm.watcher.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        val eventType = when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> if (isReplacing) "UPDATE" else "INSTALL"
            Intent.ACTION_PACKAGE_REMOVED -> if (isReplacing) return else "REMOVED"
            else -> return
        }

        log(TAG) { "onReceive: $eventType for $packageName" }

        val inputData = Data.Builder()
            .putString(PermissionWatcherWorker.KEY_PACKAGE_NAME, packageName)
            .putString(PermissionWatcherWorker.KEY_EVENT_TYPE, eventType)
            .build()

        val request = OneTimeWorkRequestBuilder<PermissionWatcherWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "watcher_$packageName",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        private val TAG = logTag("Watcher", "PackageChangeReceiver")
    }
}
