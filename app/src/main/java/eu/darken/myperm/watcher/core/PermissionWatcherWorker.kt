package eu.darken.myperm.watcher.core

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltWorker
class PermissionWatcherWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appRepo: AppRepo,
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

        val scope = generalSettings.watcherScope.value()
        log(TAG) { "doWork: scope=$scope" }

        val lastDiffedId = generalSettings.lastDiffedSnapshotId.value()
        val latestSnapshotId = appRepo.getLatestSnapshotId()

        if (latestSnapshotId == null) {
            log(TAG) { "No snapshots available, skipping" }
            return Result.success()
        }

        // Fast-forward: first run or just enabled — set baseline without replaying history
        if (lastDiffedId == null) {
            log(TAG) { "First run, fast-forwarding to snapshot $latestSnapshotId" }
            generalSettings.lastDiffedSnapshotId.update { latestSnapshotId }
            return Result.success()
        }

        val chain = appRepo.getSnapshotChainSince(lastDiffedId)
        if (chain == null) {
            log(TAG, WARN) { "Anchor snapshot $lastDiffedId not found (pruned?), fast-forwarding" }
            generalSettings.lastDiffedSnapshotId.update { latestSnapshotId }
            return Result.success()
        }

        if (chain.pairs.isEmpty()) {
            log(TAG) { "No new snapshots since $lastDiffedId" }
            return Result.success()
        }

        log(TAG) { "Processing ${chain.pairs.size} new snapshot pair(s)" }

        val reportedPackages = mutableSetOf<Pair<String, Int>>()
        val permCache = mutableMapOf<String, AppRepo.SnapshotPermissions>()

        try {
            for (pair in chain.pairs) {
                diffSnapshotPair(pair, scope, reportedPackages, permCache)
            }
        } catch (e: Exception) {
            log(TAG, WARN) { "Error during snapshot diff: ${e.asLog()}" }
            return Result.retry()
        }

        generalSettings.lastDiffedSnapshotId.update { chain.latestSnapshotId }
        log(TAG) { "Updated lastDiffedSnapshotId to ${chain.latestSnapshotId}" }
        return Result.success()
    }

    private suspend fun diffSnapshotPair(
        pair: AppRepo.SnapshotPair,
        scope: WatcherScope,
        reportedPackages: MutableSet<Pair<String, Int>>,
        permCache: MutableMap<String, AppRepo.SnapshotPermissions>,
    ) {
        val oldPkgMap = pair.oldPkgs.associateBy { Pair(it.pkgName, it.userHandleId) }
        val newPkgMap = pair.newPkgs.associateBy { Pair(it.pkgName, it.userHandleId) }

        // Bulk-fetch permissions for both snapshots (avoids per-package N+1 queries)
        val oldPermsAll = permCache.getOrPut(pair.oldSnapshotId) { appRepo.getSnapshotPermissions(pair.oldSnapshotId) }
        val newPermsAll = permCache.getOrPut(pair.newSnapshotId) { appRepo.getSnapshotPermissions(pair.newSnapshotId) }

        val allPkgKeys = (oldPkgMap.keys + newPkgMap.keys).toSet()

        for (key in allPkgKeys) {
            val (pkgName, userHandleId) = key
            if (key in reportedPackages) continue

            val oldPkg = oldPkgMap[key]
            val newPkg = newPkgMap[key]

            // Filter by watcher scope
            val isSystemApp = (newPkg ?: oldPkg)?.isSystemApp ?: false
            if (scope == WatcherScope.NON_SYSTEM && isSystemApp) continue

            val eventType: String
            val diff: PermissionDiff

            when {
                // New package appeared
                oldPkg == null && newPkg != null -> {
                    // First time seeing this package — baseline, no report
                    continue
                }
                // Package removed
                oldPkg != null && newPkg == null -> {
                    eventType = "REMOVED"
                    val requested = oldPermsAll.requested[key] ?: emptyList()
                    val declared = oldPermsAll.declared[key] ?: emptyList()
                    diff = PermissionDiff(
                        removedPermissions = requested.map { it.permissionId },
                        removedDeclared = declared.map { it.permissionId },
                    )
                }
                // Package exists in both — check for changes
                oldPkg != null && newPkg != null -> {
                    eventType = "UPDATE"
                    val oldRequested = oldPermsAll.requested[key] ?: emptyList()
                    val oldDeclared = oldPermsAll.declared[key] ?: emptyList()
                    val newRequested = newPermsAll.requested[key] ?: emptyList()
                    val newDeclared = newPermsAll.declared[key] ?: emptyList()

                    diff = snapshotDiffer.diff(
                        previousPerms = oldRequested,
                        previousDeclared = oldDeclared,
                        currentPerms = newRequested.map {
                            SnapshotDiffer.CurrentPermission(it.permissionId, UsesPermission.Status.valueOf(it.status))
                        },
                        currentDeclared = newDeclared.map { it.permissionId },
                    )
                }
                else -> continue
            }

            if (diff.isEmpty) continue

            log(TAG) { "Permission changes detected for $pkgName: $diff" }

            val reportId = changeDao.insert(
                PermissionChangeEntity(
                    packageName = pkgName,
                    userHandleId = userHandleId,
                    appLabel = newPkg?.cachedLabel ?: oldPkg?.cachedLabel,
                    versionCode = newPkg?.versionCode ?: oldPkg?.versionCode ?: 0L,
                    versionName = newPkg?.versionName ?: oldPkg?.versionName,
                    eventType = eventType,
                    changesJson = json.encodeToString(diff),
                    detectedAt = System.currentTimeMillis(),
                )
            )

            watcherNotifications.postChangeNotification(
                reportId = reportId,
                appLabel = newPkg?.cachedLabel ?: oldPkg?.cachedLabel,
                packageName = pkgName,
                diff = diff,
            )

            reportedPackages.add(key)
        }
    }

    companion object {
        private val TAG = logTag("Watcher", "Worker")
    }
}
