package eu.darken.myperm.watcher.core

import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatcherDiffRunner @Inject constructor(
    private val appRepo: AppRepo,
    private val snapshotDiffer: SnapshotDiffer,
    private val changeDao: PermissionChangeDao,
    private val watcherNotifications: WatcherNotifications,
    private val generalSettings: GeneralSettings,
    private val json: Json,
) {

    /**
     * Processes all new snapshots since the last diffed snapshot.
     * Updates [GeneralSettings.lastDiffedSnapshotId] progressively after each pair.
     * Returns the number of reports created.
     */
    suspend fun processNewSnapshots(): Int {
        val isEnabled = generalSettings.isWatcherEnabled.value()
        if (!isEnabled) {
            log(TAG) { "Watcher is disabled, skipping" }
            return 0
        }

        val scope = generalSettings.watcherScope.value()
        log(TAG) { "processNewSnapshots: scope=$scope" }

        val lastDiffedId = generalSettings.lastDiffedSnapshotId.value()
        val latestSnapshotId = appRepo.getLatestSnapshotId()

        if (latestSnapshotId == null) {
            log(TAG) { "No snapshots available, skipping" }
            return 0
        }

        // Fast-forward: first run or just enabled — set baseline without replaying history
        if (lastDiffedId == null) {
            log(TAG) { "First run, fast-forwarding to snapshot $latestSnapshotId" }
            generalSettings.lastDiffedSnapshotId.update { latestSnapshotId }
            return 0
        }

        val chain = appRepo.getSnapshotChainSince(lastDiffedId)
        if (chain == null) {
            log(TAG, WARN) { "Anchor snapshot $lastDiffedId not found (pruned?), fast-forwarding" }
            generalSettings.lastDiffedSnapshotId.update { latestSnapshotId }
            return 0
        }

        if (chain.pairs.isEmpty()) {
            log(TAG) { "No new snapshots since $lastDiffedId" }
            return 0
        }

        log(TAG) { "Processing ${chain.pairs.size} new snapshot pair(s)" }

        var totalReports = 0
        val reportedPackages = mutableSetOf<Pair<String, Int>>()

        for (pair in chain.pairs) {
            totalReports += diffSnapshotPair(pair, scope, reportedPackages)
            // Progressive update: advance after each pair so cancellation only re-processes the current pair
            generalSettings.lastDiffedSnapshotId.update { pair.newSnapshotId }
        }

        if (totalReports > 1) {
            watcherNotifications.postSummaryNotification(totalReports)
        }

        log(TAG) { "Processed ${chain.pairs.size} pair(s), created $totalReports report(s)" }
        return totalReports
    }

    private suspend fun diffSnapshotPair(
        pair: AppRepo.SnapshotPair,
        scope: WatcherScope,
        reportedPackages: MutableSet<Pair<String, Int>>,
    ): Int {
        var reportsCreated = 0

        val oldPkgMap = pair.oldPkgs.associateBy { Pair(it.pkgName, it.userHandleId) }
        val newPkgMap = pair.newPkgs.associateBy { Pair(it.pkgName, it.userHandleId) }

        val oldPermsAll = appRepo.getSnapshotPermissions(pair.oldSnapshotId)
        val newPermsAll = appRepo.getSnapshotPermissions(pair.newSnapshotId)

        val allPkgKeys = oldPkgMap.keys + newPkgMap.keys

        for (key in allPkgKeys) {
            val (pkgName, userHandleId) = key
            if (key in reportedPackages) continue

            val oldPkg = oldPkgMap[key]
            val newPkg = newPkgMap[key]

            val isSystemApp = (newPkg ?: oldPkg)?.isSystemApp ?: false
            if (scope == WatcherScope.NON_SYSTEM && isSystemApp) continue

            // Idempotent: skip if already reported for this snapshot
            if (changeDao.existsByPackageAndSnapshot(pkgName, userHandleId, pair.newSnapshotId)) {
                log(TAG) { "Skipping $pkgName — already reported for snapshot ${pair.newSnapshotId}" }
                reportedPackages.add(key)
                continue
            }

            val eventType: String
            val diff: PermissionDiff

            when {
                oldPkg == null && newPkg != null -> {
                    eventType = "INSTALL"
                    val requested = newPermsAll.requested[key] ?: emptyList()
                    val declared = newPermsAll.declared[key] ?: emptyList()
                    diff = PermissionDiff(
                        addedPermissions = requested.map { it.permissionId },
                        addedDeclared = declared.map { it.permissionId },
                    )
                }
                oldPkg != null && newPkg == null -> {
                    eventType = "REMOVED"
                    val requested = oldPermsAll.requested[key] ?: emptyList()
                    val declared = oldPermsAll.declared[key] ?: emptyList()
                    diff = PermissionDiff(
                        removedPermissions = requested.map { it.permissionId },
                        removedDeclared = declared.map { it.permissionId },
                    )
                }
                oldPkg != null && newPkg != null -> {
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

                    val isVersionUnchanged = oldPkg.versionCode == newPkg.versionCode
                    val isGrantChangeOnly = diff.addedPermissions.isEmpty()
                            && diff.removedPermissions.isEmpty()
                            && diff.addedDeclared.isEmpty()
                            && diff.removedDeclared.isEmpty()
                            && diff.grantChanges.isNotEmpty()

                    eventType = if (isVersionUnchanged && isGrantChangeOnly) "GRANT_CHANGE" else "UPDATE"
                }
                else -> continue
            }

            if (diff.isEmpty && eventType != "INSTALL") continue

            log(TAG) { "Permission changes detected for $pkgName: $diff" }

            val reportId = changeDao.insert(
                PermissionChangeEntity(
                    packageName = pkgName,
                    userHandleId = userHandleId,
                    appLabel = newPkg?.cachedLabel ?: oldPkg?.cachedLabel,
                    versionCode = newPkg?.versionCode ?: oldPkg?.versionCode ?: 0L,
                    versionName = newPkg?.versionName ?: oldPkg?.versionName,
                    previousVersionCode = oldPkg?.versionCode,
                    previousVersionName = oldPkg?.versionName,
                    eventType = eventType,
                    changesJson = json.encodeToString(diff),
                    detectedAt = System.currentTimeMillis(),
                    sourceSnapshotId = pair.newSnapshotId,
                )
            )

            watcherNotifications.postChangeNotification(
                reportId = reportId,
                appLabel = newPkg?.cachedLabel ?: oldPkg?.cachedLabel,
                packageName = pkgName,
                diff = diff,
            )

            reportedPackages.add(key)
            reportsCreated++
        }

        return reportsCreated
    }

    companion object {
        private val TAG = logTag("Watcher", "DiffRunner")
    }
}
