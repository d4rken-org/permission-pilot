package eu.darken.myperm.watcher.core

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatcherDiffRunner @Inject constructor(
    private val snapshotDao: SnapshotDao,
    private val snapshotPkgDao: SnapshotPkgDao,
    private val snapshotDiffer: SnapshotDiffer,
    private val changeDao: PermissionChangeDao,
    private val watcherNotifications: WatcherNotifications,
    private val generalSettings: GeneralSettings,
    private val upgradeRepo: UpgradeRepo,
    private val json: Json,
) {

    // ── Snapshot queries ─────────────────────────────────────────────

    private data class SnapshotChain(
        val latestSnapshotId: String,
        val pairs: List<SnapshotPair>,
    )

    private data class SnapshotPair(
        val oldSnapshotId: String,
        val newSnapshotId: String,
        val oldPkgs: List<SnapshotPkgEntity>,
        val newPkgs: List<SnapshotPkgEntity>,
    )

    private data class SnapshotPermissions(
        val requested: Map<Pair<Pkg.Name, Int>, List<SnapshotPkgPermEntity>>,
        val declared: Map<Pair<Pkg.Name, Int>, List<SnapshotPkgDeclaredPermEntity>>,
    )

    private suspend fun getLatestSnapshotId(): String? {
        return snapshotDao.getLatestSnapshot()?.snapshotId
    }

    private suspend fun getSnapshotChainSince(anchorId: String): SnapshotChain? {
        val anchor = snapshotDao.getSnapshotById(anchorId) ?: return null
        val newerSnapshots = snapshotDao.getSnapshotsAfter(anchorId)
        if (newerSnapshots.isEmpty()) return null

        val chain = listOf(anchor) + newerSnapshots
        val pairs = (0 until chain.size - 1).map { i ->
            val oldSnapshot = chain[i]
            val newSnapshot = chain[i + 1]
            SnapshotPair(
                oldSnapshotId = oldSnapshot.snapshotId,
                newSnapshotId = newSnapshot.snapshotId,
                oldPkgs = snapshotPkgDao.getPkgsForSnapshot(oldSnapshot.snapshotId),
                newPkgs = snapshotPkgDao.getPkgsForSnapshot(newSnapshot.snapshotId),
            )
        }

        return SnapshotChain(
            latestSnapshotId = newerSnapshots.last().snapshotId,
            pairs = pairs,
        )
    }

    private suspend fun getSnapshotPermissions(snapshotId: String): SnapshotPermissions {
        val requested = snapshotPkgDao.getPermsForSnapshot(snapshotId)
            .groupBy { Pair(it.pkgName, it.userHandleId) }
        val declared = snapshotPkgDao.getDeclaredPermsForSnapshot(snapshotId)
            .groupBy { Pair(it.pkgName, it.userHandleId) }
        return SnapshotPermissions(requested, declared)
    }

    // ── Public API ───────────────────────────────────────────────────

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
        val latestSnapshotId = getLatestSnapshotId()

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

        val chain = getSnapshotChainSince(lastDiffedId)
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

        val isPro = upgradeRepo.upgradeInfo.value.isPro
        var totalReports = 0
        var totalNotified = 0
        val reportedPackages = mutableSetOf<Pair<Pkg.Name, Int>>()

        for (pair in chain.pairs) {
            val (reports, notified) = diffSnapshotPair(pair, scope, reportedPackages, isPro)
            totalReports += reports
            totalNotified += notified
            // Progressive update: advance after each pair so cancellation only re-processes the current pair
            generalSettings.lastDiffedSnapshotId.update { pair.newSnapshotId }
        }

        if (totalNotified > 1 && isPro) {
            watcherNotifications.postSummaryNotification(totalNotified)
        }

        log(TAG) { "Processed ${chain.pairs.size} pair(s), created $totalReports report(s), notified $totalNotified" }
        return totalReports
    }

    private suspend fun diffSnapshotPair(
        pair: SnapshotPair,
        scope: WatcherScope,
        reportedPackages: MutableSet<Pair<Pkg.Name, Int>>,
        isPro: Boolean,
    ): Pair<Int, Int> {
        var reportsCreated = 0
        var notifiedCount = 0

        val oldPkgMap = pair.oldPkgs.associateBy { Pair(it.pkgName, it.userHandleId) }
        val newPkgMap = pair.newPkgs.associateBy { Pair(it.pkgName, it.userHandleId) }

        val oldPermsAll = getSnapshotPermissions(pair.oldSnapshotId)
        val newPermsAll = getSnapshotPermissions(pair.newSnapshotId)

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

            val eventType: WatcherEventType
            val diff: PermissionDiff

            when {
                oldPkg == null && newPkg != null -> {
                    eventType = WatcherEventType.INSTALL
                    val requested = newPermsAll.requested[key] ?: emptyList()
                    val declared = newPermsAll.declared[key] ?: emptyList()
                    diff = PermissionDiff(
                        addedPermissions = requested.map { it.permissionId },
                        addedDeclared = declared.map { it.permissionId },
                    )
                }
                oldPkg != null && newPkg == null -> {
                    eventType = WatcherEventType.REMOVED
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

                    eventType = if (isVersionUnchanged && isGrantChangeOnly) WatcherEventType.GRANT_CHANGE else WatcherEventType.UPDATE
                }
                else -> continue
            }

            if (diff.isEmpty && eventType != WatcherEventType.INSTALL && eventType != WatcherEventType.REMOVED) continue

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

            val notified = if (isPro) {
                watcherNotifications.postChangeNotification(
                    reportId = reportId,
                    appLabel = newPkg?.cachedLabel ?: oldPkg?.cachedLabel,
                    packageName = pkgName,
                    diff = diff,
                )
            } else {
                false
            }

            reportedPackages.add(key)
            reportsCreated++
            if (notified) notifiedCount++
        }

        return Pair(reportsCreated, notifiedCount)
    }

    companion object {
        private val TAG = logTag("Watcher", "DiffRunner")
    }
}
