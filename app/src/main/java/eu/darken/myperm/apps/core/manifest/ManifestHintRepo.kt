package eu.darken.myperm.apps.core.manifest

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.room.dao.ManifestHintDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManifestHintRepo @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val manifestHintDao: ManifestHintDao,
    private val manifestRepo: ManifestRepo,
    private val manifestHintScanner: ManifestHintScanner,
    private val workManager: WorkManager,
) {
    data class ScanProgress(val total: Int, val scanned: Int)

    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()

    private val _currentlyScanning = MutableStateFlow<Pkg.Name?>(null)
    val currentlyScanning: StateFlow<Pkg.Name?> = _currentlyScanning.asStateFlow()

    private val priorityQueue = ConcurrentLinkedQueue<String>()

    val hints: StateFlow<Map<Pkg.Name, ManifestHintEntity>> = manifestHintDao.observeAll()
        .map { list -> list.associateBy { it.pkgName } }
        .stateIn(appScope, SharingStarted.Eagerly, emptyMap())

    fun enqueueHintScan() {
        log(TAG) { "enqueueHintScan()" }
        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ManifestHintWorker>().build(),
        )
    }

    fun prioritize(pkgName: Pkg.Name) {
        log(TAG) { "Prioritizing $pkgName" }
        priorityQueue.add(pkgName.value)
        if (_scanProgress.value == null) {
            log(TAG) { "No scan in progress, triggering new scan for priority" }
            enqueueHintScan()
        }
    }

    suspend fun runScan(apps: List<AppInfo>) {
        val existingByPkg = manifestHintDao.getAll().associateBy { it.pkgName }
        val pending = apps.distinctBy { it.pkgName }.toMutableList()
        val total = pending.size
        var scanned = 0
        _scanProgress.value = ScanProgress(total, scanned)

        log(TAG) { "Starting scan of $total unique packages" }

        val newHints = mutableListOf<ManifestHintEntity>()

        while (pending.isNotEmpty()) {
            val priorityPkg = priorityQueue.poll()
            val nextApp = if (priorityPkg != null) {
                val idx = pending.indexOfFirst { it.pkgName.value == priorityPkg }
                if (idx >= 0) pending.removeAt(idx) else pending.removeAt(0)
            } else {
                pending.removeAt(0)
            }

            val lastUpdateTime = nextApp.updatedAt?.toEpochMilli() ?: 0L
            val existing = existingByPkg[nextApp.pkgName]
            if (existing != null && existing.versionCode == nextApp.versionCode && existing.lastUpdateTime == lastUpdateTime) {
                scanned++
                _scanProgress.value = ScanProgress(total, scanned)
                continue
            }

            _currentlyScanning.value = nextApp.pkgName
            val manifestData = manifestRepo.getManifest(nextApp.pkgName)

            // Skip hint upsert on transient failures (LOW_MEMORY/OOM) to avoid persisting false "no queries"
            if (manifestData.rawXml is RawXmlResult.Unavailable &&
                (manifestData.rawXml as RawXmlResult.Unavailable).reason == UnavailableReason.LOW_MEMORY
            ) {
                scanned++
                _scanProgress.value = ScanProgress(total, scanned)
                continue
            }

            val queriesInfo = when (val q = manifestData.queries) {
                is QueriesResult.Success -> q.info
                is QueriesResult.Error -> QueriesInfo()
            }

            val flags = manifestHintScanner.evaluate(queriesInfo)
            val hint = ManifestHintEntity(
                pkgName = nextApp.pkgName,
                versionCode = nextApp.versionCode,
                lastUpdateTime = lastUpdateTime,
                hasActionMainQuery = flags.hasActionMainQuery,
                packageQueryCount = flags.packageQueryCount,
                intentQueryCount = flags.intentQueryCount,
                providerQueryCount = flags.providerQueryCount,
                scannedAt = System.currentTimeMillis(),
            )
            newHints.add(hint)

            // Upsert in batches of 20 so hints become visible incrementally
            if (newHints.size >= 20) {
                manifestHintDao.upsertHints(newHints)
                newHints.clear()
            }

            scanned++
            _scanProgress.value = ScanProgress(total, scanned)
        }

        if (newHints.isNotEmpty()) {
            manifestHintDao.upsertHints(newHints)
        }

        manifestHintDao.pruneStale()
        _currentlyScanning.value = null
        _scanProgress.value = null

        log(TAG) { "Scan complete: $scanned packages processed" }
    }

    companion object {
        private const val WORK_NAME = "manifest_hint_scan"
        private val TAG = logTag("Apps", "Manifest", "HintRepo")
    }
}
