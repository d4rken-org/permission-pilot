package eu.darken.myperm.apps.core.manifest

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
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

    suspend fun runScan(apps: List<AppInfo>) = try {
        val existingByPkg = manifestHintDao.getAll().associateBy { it.pkgName }
        val pending = apps.distinctBy { it.pkgName }.toMutableList()
        val total = pending.size
        var scanned = 0
        _scanProgress.value = ScanProgress(total, scanned)

        log(TAG) { "Starting scan of $total unique packages" }

        val newHints = mutableListOf<ManifestHintEntity>()
        val counts = OutcomeCounts()

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
            val outcome = manifestRepo.getQueriesFor(nextApp.pkgName)

            when (outcome) {
                is QueriesOutcome.Success -> {
                    counts.success++
                    val flags = manifestHintScanner.evaluate(outcome.info)
                    newHints.add(
                        ManifestHintEntity(
                            pkgName = nextApp.pkgName,
                            versionCode = nextApp.versionCode,
                            lastUpdateTime = lastUpdateTime,
                            hasActionMainQuery = flags.hasActionMainQuery,
                            packageQueryCount = flags.packageQueryCount,
                            intentQueryCount = flags.intentQueryCount,
                            providerQueryCount = flags.providerQueryCount,
                            scannedAt = System.currentTimeMillis(),
                        )
                    )

                    // Upsert in batches of 20 so hints become visible incrementally
                    if (newHints.size >= 20) {
                        manifestHintDao.upsertHints(newHints)
                        newHints.clear()
                    }
                }

                is QueriesOutcome.Unavailable -> {
                    log(TAG, WARN) { "Unavailable outcome for ${nextApp.pkgName}: reason=${outcome.reason}" }
                    when (outcome.reason) {
                        UnavailableReason.LOW_MEMORY -> counts.lowMemory++
                        UnavailableReason.APK_TOO_LARGE -> counts.apkTooLarge++
                        UnavailableReason.MALFORMED_APK,
                        UnavailableReason.APK_NOT_FOUND,
                        UnavailableReason.APK_NOT_READABLE,
                        UnavailableReason.PKG_NOT_FOUND -> counts.failure++
                    }
                    // If a prior scan succeeded for an older version, that stale hint must not
                    // outlive the new (unreadable) app. Delete so the UI falls back to the
                    // "queued/no data" state instead of showing old flags.
                    if (existing != null) {
                        manifestHintDao.deleteByPkgName(nextApp.pkgName)
                    }
                }

                is QueriesOutcome.Failure -> {
                    log(TAG, WARN) { "Failure outcome for ${nextApp.pkgName}: ${outcome.error}" }
                    // Parser errors (e.g. BinaryXmlException) are classified transient: the fault
                    // may be a bug in our new binary-XML parser, not a broken APK. Keep any
                    // existing hint so a subsequent scan can refresh it — don't delete proactively.
                    counts.failure++
                }
            }

            scanned++
            _scanProgress.value = ScanProgress(total, scanned)
        }

        if (newHints.isNotEmpty()) {
            manifestHintDao.upsertHints(newHints)
        }

        manifestHintDao.pruneStale()

        log(TAG) {
            "Scan complete: $scanned packages — success=${counts.success} lowMemory=${counts.lowMemory} " +
                "apkTooLarge=${counts.apkTooLarge} failure=${counts.failure}"
        }
    } finally {
        _currentlyScanning.value = null
        _scanProgress.value = null
    }

    private class OutcomeCounts {
        var success = 0
        var lowMemory = 0
        var apkTooLarge = 0
        var failure = 0
    }

    companion object {
        private const val WORK_NAME = "manifest_hint_scan"
        private val TAG = logTag("Apps", "Manifest", "HintRepo")
    }
}
