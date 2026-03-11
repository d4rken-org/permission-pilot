package eu.darken.myperm.common.room.snapshot

import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import androidx.room.withTransaction
import eu.darken.myperm.common.room.PermPilotDatabase
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.common.room.entity.SnapshotEntity
import eu.darken.myperm.common.room.entity.TriggerReason
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
        val mapped = pkgs.map { snapshotMapper.toEntities(snapshotId, it) }
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
            snapshotPkgDao.insertPkgs(mapped.map { it.pkg })
            snapshotPkgDao.insertPermissions(mapped.flatMap { it.permissions })
            snapshotPkgDao.insertDeclaredPermissions(mapped.flatMap { it.declaredPermissions })
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

        val pkgEntities = snapshotPkgDao.getPkgsForSnapshot(latest.snapshotId)
        if (pkgEntities.isEmpty()) return null

        val allPerms = snapshotPkgDao.getPermsForSnapshot(latest.snapshotId)
        val allDeclaredPerms = snapshotPkgDao.getDeclaredPermsForSnapshot(latest.snapshotId)

        val permsByPkg = allPerms.groupBy { Triple(it.snapshotId, it.pkgName, it.userHandleId) }
        val declaredCountByPkg = allDeclaredPerms.groupBy { Triple(it.snapshotId, it.pkgName, it.userHandleId) }

        val result = pkgEntities.map { pkgEntity ->
            val key = Triple(pkgEntity.snapshotId, pkgEntity.pkgName, pkgEntity.userHandleId)
            snapshotMapper.toCachedAppInfo(
                pkgEntity = pkgEntity,
                permEntities = permsByPkg[key] ?: emptyList(),
                declaredPermCount = declaredCountByPkg[key]?.size ?: 0,
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
