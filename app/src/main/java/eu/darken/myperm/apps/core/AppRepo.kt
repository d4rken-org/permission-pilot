package eu.darken.myperm.apps.core

import android.content.Context
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.apps.core.manifest.ManifestHintRepo
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.shareLatest
import eu.darken.myperm.common.room.PermPilotDatabase
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.common.room.entity.SnapshotEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.TriggerReason
import eu.darken.myperm.watcher.core.WatcherWorkScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
    packageEventListener: PackageEventListener,
    private val appSourcer: AppSourcer,
    private val database: PermPilotDatabase,
    private val snapshotDao: SnapshotDao,
    private val snapshotPkgDao: SnapshotPkgDao,
    private val snapshotMapper: SnapshotMapper,
    private val workManager: WorkManager,
    private val manifestHintRepo: ManifestHintRepo,
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
                manifestHintRepo.hints,
            ) { pkgs, perms, declaredCounts, hints ->
                if (pkgs.isEmpty()) return@combine AppDataState.Ready(emptyList())
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
                        manifestHint = hints[pkgEntity.pkgName],
                    )
                }
                AppDataState.Ready(apps)
            }
        }
        .shareLatest(scope = appScope, started = SharingStarted.Lazily)

    // ── Sync / refresh ──────────────────────────────────────────────────

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<Throwable?>(null)
    val scanError: StateFlow<Throwable?> = _scanError.asStateFlow()

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
            _isScanning.value = true
            try {
                try {
                    scanAndSave(reason)
                    _scanError.value = null
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    log(TAG, WARN) { "Failed to scan/save: ${e.asLog()}" }
                    _scanError.value = e
                }
                try {
                    enqueuePermissionWatcher()
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    log(TAG, WARN) { "Watcher enqueue failed: ${e.asLog()}" }
                }
                try {
                    manifestHintRepo.enqueueHintScan()
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    log(TAG, WARN) { "Hint scan enqueue failed: ${e.asLog()}" }
                }
            } finally {
                _isScanning.value = false
            }
        }.launchIn(appScope)
    }

    fun refresh() {
        log(TAG) { "refresh()" }
        refreshTrigger.tryEmit(TriggerReason.MANUAL_REFRESH)
    }

    private fun enqueuePermissionWatcher() = WatcherWorkScheduler.enqueueWatcher(workManager)

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

        // Map + insert in chunks. Labels are resolved per-chunk (outside the DB insert)
        // to cap peak memory at ~50 packages worth of transient Resources objects.
        val txStart = System.currentTimeMillis()
        val pkgList = pkgs.toList()
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
            for (chunk in pkgList.chunked(50)) {
                // Pre-resolve labels for this chunk before mapping to entities.
                // loadLabel() triggers IPC + resource loading, so keep it outside entity mapping.
                for (pkg in chunk) {
                    try {
                        pkg.getLabel(context)
                    } catch (e: Exception) {
                        log(TAG, WARN) { "Failed to pre-resolve label for ${pkg.id}: $e" }
                    }
                }
                val entities = chunk.map { snapshotMapper.toEntities(snapshotId, it) }
                snapshotPkgDao.insertPkgs(entities.map { it.pkg })
                snapshotPkgDao.insertPermissions(entities.flatMap { it.permissions })
                snapshotPkgDao.insertDeclaredPermissions(entities.flatMap { it.declaredPermissions })
            }
        }
        log(TAG) { "Perf: saveSnapshot() DB transaction in ${System.currentTimeMillis() - txStart}ms" }

        pruneSnapshots()
        log(TAG) { "Perf: saveSnapshot() total in ${System.currentTimeMillis() - totalStart}ms" }
    }

    suspend fun pruneSnapshots(keepCount: Int = 20) {
        val oldIds = snapshotDao.getOldSnapshotIds(keepCount)
        if (oldIds.isNotEmpty()) {
            log(TAG) { "pruneSnapshots(): deleting ${oldIds.size} old snapshots" }
            snapshotDao.deleteSnapshots(oldIds)
        }
    }

    suspend fun pruneSnapshotsBefore(anchorId: String) {
        val oldIds = snapshotDao.getSnapshotIdsBefore(anchorId)
        if (oldIds.isNotEmpty()) {
            log(TAG) { "pruneSnapshotsBefore($anchorId): deleting ${oldIds.size} old snapshots" }
            snapshotDao.deleteSnapshots(oldIds)
        }
    }

    // ── Queries ─────────────────────────────────────────────────────────

    suspend fun getLatestDeclaredPerms(): List<SnapshotPkgDeclaredPermEntity> {
        val latest = snapshotDao.getLatestSnapshot() ?: return emptyList()
        return snapshotPkgDao.getDeclaredPermsForSnapshot(latest.snapshotId)
    }

    companion object {
        internal val TAG = logTag("Apps", "Repo")
    }
}
