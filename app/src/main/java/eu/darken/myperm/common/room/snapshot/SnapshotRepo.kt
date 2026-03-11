package eu.darken.myperm.common.room.snapshot

import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import androidx.room.withTransaction
import eu.darken.myperm.common.room.PermPilotDatabase
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.common.room.entity.SnapshotEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity
import eu.darken.myperm.common.room.entity.TriggerReason
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepo @Inject constructor(
    private val database: PermPilotDatabase,
    private val snapshotDao: SnapshotDao,
    private val snapshotPkgDao: SnapshotPkgDao,
    private val snapshotMapper: SnapshotMapper,
) {

    private val saveMutex = Mutex()

    suspend fun saveSnapshot(
        pkgs: Collection<BasePkg>,
        reason: TriggerReason,
        durationMs: Long,
    ) = saveMutex.withLock {
        val totalStart = System.currentTimeMillis()
        val snapshotId = UUID.randomUUID().toString()
        log(TAG) { "saveSnapshot($snapshotId, reason=$reason, pkgs=${pkgs.size})" }

        val mapStart = System.currentTimeMillis()
        val allPkgEntities = ArrayList<SnapshotPkgEntity>(pkgs.size)
        val allPermEntities = ArrayList<SnapshotPkgPermEntity>()
        val allDeclaredPermEntities = ArrayList<SnapshotPkgDeclaredPermEntity>()
        for (pkg in pkgs) {
            val entities = snapshotMapper.toEntities(snapshotId, pkg)
            allPkgEntities.add(entities.pkg)
            allPermEntities.addAll(entities.permissions)
            allDeclaredPermEntities.addAll(entities.declaredPermissions)
        }
        log(TAG) { "Perf: saveSnapshot() mapped ${pkgs.size} pkgs in ${System.currentTimeMillis() - mapStart}ms" }

        val txStart = System.currentTimeMillis()
        database.withTransaction {
            snapshotDao.insertSnapshot(
                SnapshotEntity(
                    snapshotId = snapshotId,
                    createdAt = System.currentTimeMillis(),
                    triggerReason = reason.name,
                    pkgCount = pkgs.size,
                    durationMs = durationMs,
                )
            )
            snapshotPkgDao.insertPkgs(allPkgEntities)
            snapshotPkgDao.insertPermissions(allPermEntities)
            snapshotPkgDao.insertDeclaredPermissions(allDeclaredPermEntities)
        }
        log(TAG) { "Perf: saveSnapshot() DB transaction in ${System.currentTimeMillis() - txStart}ms" }

        pruneSnapshots()
        log(TAG) { "Perf: saveSnapshot() total in ${System.currentTimeMillis() - totalStart}ms" }
    }

    suspend fun loadCachedApps(): List<CachedAppInfo>? {
        val start = System.currentTimeMillis()
        val latest = snapshotDao.getLatestSnapshot() ?: run {
            log(TAG) { "Perf: loadCachedApps() no snapshot found in ${System.currentTimeMillis() - start}ms" }
            return null
        }
        log(TAG) { "loadCachedApps() from snapshot ${latest.snapshotId}" }

        val (pkgEntities, allPerms, declaredCounts) = coroutineScope {
            val pkgsDeferred = async { snapshotPkgDao.getPkgsForSnapshot(latest.snapshotId) }
            val permsDeferred = async { snapshotPkgDao.getPermsForSnapshot(latest.snapshotId) }
            val declaredDeferred = async { snapshotPkgDao.getDeclaredPermCountsForSnapshot(latest.snapshotId) }
            Triple(pkgsDeferred.await(), permsDeferred.await(), declaredDeferred.await())
        }

        if (pkgEntities.isEmpty()) return null

        val permsByPkg = allPerms.groupBy { Pair(it.pkgName, it.userHandleId) }
        val declaredCountByPkg = declaredCounts.associateBy(
            keySelector = { Pair(it.pkgName, it.userHandleId) },
            valueTransform = { it.declaredCount },
        )

        val result = pkgEntities.map { pkgEntity ->
            val key = Pair(pkgEntity.pkgName, pkgEntity.userHandleId)
            snapshotMapper.toCachedAppInfo(
                pkgEntity = pkgEntity,
                permEntities = permsByPkg[key] ?: emptyList(),
                declaredPermCount = declaredCountByPkg[key] ?: 0,
            )
        }
        log(TAG) { "Perf: loadCachedApps() loaded ${result.size} apps in ${System.currentTimeMillis() - start}ms" }
        return result
    }

    suspend fun getLatestPkgSnapshot(
        pkgName: String,
        userHandleId: Int,
    ): PkgSnapshotData? {
        val latest = snapshotDao.getLatestSnapshot() ?: return null
        val pkgEntity = snapshotPkgDao.getPkgByName(latest.snapshotId, pkgName, userHandleId) ?: return null
        val perms = snapshotPkgDao.getPermsForPkg(latest.snapshotId, pkgName, userHandleId)
        val declaredPerms = snapshotPkgDao.getDeclaredPermsForPkg(latest.snapshotId, pkgName, userHandleId)
        return PkgSnapshotData(pkgEntity, perms, declaredPerms)
    }

    private suspend fun pruneSnapshots(keepCount: Int = 5) {
        val oldIds = snapshotDao.getOldSnapshotIds(keepCount)
        if (oldIds.isNotEmpty()) {
            log(TAG) { "pruneSnapshots(): deleting ${oldIds.size} old snapshots" }
            snapshotDao.deleteSnapshots(oldIds)
        }
    }

    companion object {
        private val TAG = logTag("Snapshot", "Repo")
    }
}
