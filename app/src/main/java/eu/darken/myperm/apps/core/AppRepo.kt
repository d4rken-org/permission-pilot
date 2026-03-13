package eu.darken.myperm.apps.core

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.PermPilotDatabase
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.common.room.entity.SnapshotEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity
import eu.darken.myperm.common.room.entity.TriggerReason
import eu.darken.myperm.common.flow.shareLatest
import eu.darken.myperm.common.room.snapshot.SnapshotWorker
import eu.darken.myperm.watcher.core.PermissionWatcherWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(FlowPreview::class)
@Singleton
class AppRepo @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    packageEventListener: PackageEventListener,
    private val appSourcer: AppSourcer,
    private val database: PermPilotDatabase,
    private val snapshotDao: SnapshotDao,
    private val snapshotPkgDao: SnapshotPkgDao,
    private val snapshotMapper: SnapshotMapper,
    private val workManager: WorkManager,
) {

    // ── UI state ────────────────────────────────────────────────────────

    sealed class AppDataState {
        data object NoSnapshot : AppDataState()
        data class Ready(val apps: List<AppInfo>) : AppDataState()
    }

    val appData: Flow<AppDataState> = snapshotDao.observeLatestSnapshotId()
        .flatMapLatest { snapshotId ->
            if (snapshotId == null) return@flatMapLatest flowOf(AppDataState.NoSnapshot)
            combine(
                snapshotPkgDao.observePkgsForSnapshot(snapshotId),
                snapshotPkgDao.observePermsForSnapshot(snapshotId),
                snapshotPkgDao.observeDeclaredPermCountsForSnapshot(snapshotId),
            ) { pkgs, perms, declaredCounts ->
                if (pkgs.isEmpty()) return@combine AppDataState.NoSnapshot
                val permsByPkg = perms.groupBy { Pair(it.pkgName, it.userHandleId) }
                val declaredCountByPkg = declaredCounts.associateBy(
                    keySelector = { Pair(it.pkgName, it.userHandleId) },
                    valueTransform = { it.declaredCount },
                )
                val apps = pkgs.map { pkgEntity ->
                    val key = Pair(pkgEntity.pkgName, pkgEntity.userHandleId)
                    snapshotMapper.toAppInfo(
                        pkgEntity = pkgEntity,
                        permEntities = permsByPkg[key] ?: emptyList(),
                        declaredPermCount = declaredCountByPkg[key] ?: 0,
                    )
                }
                AppDataState.Ready(apps)
            }
        }
        .shareLatest(scope = appScope, started = SharingStarted.Lazily)

    // ── Sync / refresh ──────────────────────────────────────────────────

    private val refreshTrigger = MutableSharedFlow<TriggerReason>(extraBufferCapacity = 1)

    init {
        merge(
            refreshTrigger,
            packageEventListener.events
                .onStart { emit(PackageEventListener.Event.PackageInstalled(AKnownPkg.AndroidSystem.id)) }
                .withIndex()
                .debounce { (index, _) -> if (index == 0) 0L else 1_000L }
                .map { (index, _) ->
                    if (index == 0) TriggerReason.APP_LAUNCH else TriggerReason.PACKAGE_CHANGE
                },
        ).onEach { reason ->
            try {
                scanAndSave(reason)
                enqueuePermissionWatcher()
            } catch (e: Exception) {
                log(TAG, WARN) { "Failed to scan/save: ${e.asLog()}" }
            }
        }.launchIn(appScope)
    }

    fun refresh() {
        log(TAG) { "refresh()" }
        refreshTrigger.tryEmit(TriggerReason.MANUAL_REFRESH)
    }

    private fun enqueuePermissionWatcher() {
        val request = OneTimeWorkRequestBuilder<PermissionWatcherWorker>().build()
        workManager.enqueueUniqueWork(
            SnapshotWorker.WATCHER_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
        log(TAG) { "Enqueued PermissionWatcherWorker" }
    }

    suspend fun scanAndSave(reason: TriggerReason) {
        val start = System.currentTimeMillis()
        val allPkgs = appSourcer.scanPackages()
        val scanDurationMs = System.currentTimeMillis() - start
        log(TAG) { "Perf: Total pkgs: ${allPkgs.size} in ${scanDurationMs}ms" }
        saveSnapshot(allPkgs, reason, scanDurationMs)
    }

    // ── Snapshot persistence ────────────────────────────────────────────

    private val saveMutex = Mutex()

    private suspend fun saveSnapshot(
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
        database.inTransaction {
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

    private suspend fun pruneSnapshots(keepCount: Int = 5) {
        val oldIds = snapshotDao.getOldSnapshotIds(keepCount)
        if (oldIds.isNotEmpty()) {
            log(TAG) { "pruneSnapshots(): deleting ${oldIds.size} old snapshots" }
            snapshotDao.deleteSnapshots(oldIds)
        }
    }

    // ── Queries ─────────────────────────────────────────────────────────

    suspend fun getLatestDeclaredPerms(): List<SnapshotPkgDeclaredPermEntity> {
        val latest = snapshotDao.getLatestSnapshot() ?: return emptyList()
        return snapshotPkgDao.getDeclaredPermsForSnapshot(latest.snapshotId)
    }

    // ── Watcher domain API ──────────────────────────────────────────────

    suspend fun getLatestSnapshotId(): String? {
        return snapshotDao.getLatestSnapshot()?.snapshotId
    }

    data class SnapshotChain(
        val latestSnapshotId: String,
        val pairs: List<SnapshotPair>,
    )

    data class SnapshotPair(
        val oldSnapshotId: String,
        val newSnapshotId: String,
        val oldPkgs: List<SnapshotPkgEntity>,
        val newPkgs: List<SnapshotPkgEntity>,
    )

    suspend fun getSnapshotChainSince(anchorId: String): SnapshotChain? {
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

    data class SnapshotPermissions(
        val requested: Map<Pair<String, Int>, List<SnapshotPkgPermEntity>>,
        val declared: Map<Pair<String, Int>, List<SnapshotPkgDeclaredPermEntity>>,
    )

    suspend fun getSnapshotPermissions(snapshotId: String): SnapshotPermissions {
        val requested = snapshotPkgDao.getPermsForSnapshot(snapshotId)
            .groupBy { Pair(it.pkgName, it.userHandleId) }
        val declared = snapshotPkgDao.getDeclaredPermsForSnapshot(snapshotId)
            .groupBy { Pair(it.pkgName, it.userHandleId) }
        return SnapshotPermissions(requested, declared)
    }

    companion object {
        internal val TAG = logTag("Apps", "Repo")
    }
}
