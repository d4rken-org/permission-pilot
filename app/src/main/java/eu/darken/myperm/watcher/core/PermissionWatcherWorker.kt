package eu.darken.myperm.watcher.core

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.room.snapshot.SnapshotRepo
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltWorker
class PermissionWatcherWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ipcFunnel: IPCFunnel,
    private val snapshotRepo: SnapshotRepo,
    private val snapshotDiffer: SnapshotDiffer,
    private val changeDao: PermissionChangeDao,
    private val watcherNotifications: WatcherNotifications,
    private val generalSettings: GeneralSettings,
    private val json: Json,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val isEnabled = generalSettings.isWatcherEnabled.value()
        if (!isEnabled) {
            log(TAG) { "Watcher is disabled, skipping" }
            return Result.success()
        }

        val packageName = inputData.getString(KEY_PACKAGE_NAME) ?: return Result.failure()
        val eventType = inputData.getString(KEY_EVENT_TYPE) ?: return Result.failure()

        log(TAG) { "doWork: $eventType for $packageName" }

        if (eventType == "REMOVED") {
            return handleRemoval(packageName)
        }

        val pkgInfo: PackageInfo = try {
            ipcFunnel.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                ?: return Result.retry()
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to get PackageInfo for $packageName: ${e.asLog()}" }
            return Result.retry()
        }

        val isSystemApp = pkgInfo.applicationInfo?.let {
            it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
        } ?: false

        val scope = generalSettings.watcherScope.value()
        if (scope == WatcherScope.NON_SYSTEM && isSystemApp) {
            log(TAG) { "Skipping system app: $packageName" }
            return Result.success()
        }

        val userHandleId = android.os.Process.myUserHandle().hashCode()
        val previousSnapshot = snapshotRepo.getLatestPkgSnapshot(packageName, userHandleId)

        if (previousSnapshot == null) {
            log(TAG) { "No previous snapshot for $packageName, skipping diff (first baseline)" }
            return Result.success()
        }

        val currentPerms = pkgInfo.requestedPermissions?.mapIndexed { index, permId ->
            val flags = pkgInfo.requestedPermissionsFlags?.get(index) ?: 0
            val status = when {
                flags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 -> "GRANTED"
                else -> "DENIED"
            }
            SnapshotDiffer.CurrentPermission(permId, status)
        } ?: emptyList()

        val currentDeclared = pkgInfo.permissions?.map { it.name } ?: emptyList()

        val diff = snapshotDiffer.diff(
            previousPerms = previousSnapshot.permissions,
            previousDeclared = previousSnapshot.declaredPermissions,
            currentPerms = currentPerms,
            currentDeclared = currentDeclared,
        )

        if (diff.isEmpty) {
            log(TAG) { "No permission changes for $packageName" }
            return Result.success()
        }

        log(TAG) { "Permission changes detected for $packageName: $diff" }

        val appLabel = try {
            pkgInfo.applicationInfo?.let {
                applicationContext.packageManager.getApplicationLabel(it).toString()
            }
        } catch (_: Exception) {
            null
        }

        val versionCode = try {
            androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pkgInfo)
        } catch (_: Exception) {
            0L
        }

        val reportId = changeDao.insert(
            PermissionChangeEntity(
                packageName = packageName,
                userHandleId = userHandleId,
                appLabel = appLabel,
                versionCode = versionCode,
                versionName = pkgInfo.versionName,
                eventType = eventType,
                changesJson = json.encodeToString(diff),
                detectedAt = System.currentTimeMillis(),
            )
        )

        watcherNotifications.postChangeNotification(
            reportId = reportId,
            appLabel = appLabel,
            packageName = packageName,
            diff = diff,
        )

        return Result.success()
    }

    private suspend fun handleRemoval(packageName: String): Result {
        val userHandleId = android.os.Process.myUserHandle().hashCode()
        val previousSnapshot = snapshotRepo.getLatestPkgSnapshot(packageName, userHandleId) ?: return Result.success()

        val diff = PermissionDiff(
            removedPermissions = previousSnapshot.permissions.map { it.permissionId },
            removedDeclared = previousSnapshot.declaredPermissions.map { it.permissionId },
        )

        if (diff.isEmpty) return Result.success()

        val reportId = changeDao.insert(
            PermissionChangeEntity(
                packageName = packageName,
                userHandleId = userHandleId,
                appLabel = previousSnapshot.pkg.cachedLabel,
                versionCode = previousSnapshot.pkg.versionCode,
                versionName = previousSnapshot.pkg.versionName,
                eventType = "REMOVED",
                changesJson = json.encodeToString(diff),
                detectedAt = System.currentTimeMillis(),
            )
        )

        watcherNotifications.postChangeNotification(
            reportId = reportId,
            appLabel = previousSnapshot.pkg.cachedLabel,
            packageName = packageName,
            diff = diff,
        )

        return Result.success()
    }

    companion object {
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_EVENT_TYPE = "event_type"
        private val TAG = logTag("Watcher", "Worker")
    }
}
