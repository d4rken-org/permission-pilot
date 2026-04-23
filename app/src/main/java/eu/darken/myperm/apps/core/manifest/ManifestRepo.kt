package eu.darken.myperm.apps.core.manifest

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManifestRepo @Inject constructor(
    private val apkManifestReader: ApkManifestReader,
    private val manifestCache: ManifestCache,
    private val dispatcherProvider: DispatcherProvider,
    @AppScope private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) {
    // Single-permit so two 40MB parses never overlap. Single-flight below deduplicates
    // concurrent callers for the same package so this isn't a throughput problem.
    private val semaphore = Semaphore(1)

    // Memory cache holds only the query projection — raw XML is NOT retained in memory.
    // Keyed by (pkg, versionCode, lastUpdate) so app updates auto-invalidate.
    // Transient failures (LOW_MEMORY, Failure) are never cached so they're retried.
    private val memoryCache = object : LinkedHashMap<ParseCacheKey, QueriesOutcome>(MEMORY_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ParseCacheKey, QueriesOutcome>?) =
            size > MEMORY_CACHE_SIZE
    }

    private val inFlightMutex = Mutex()
    private val inFlight = HashMap<ParseCacheKey, Deferred<ManifestData>>()

    /**
     * Full manifest — used by the manifest viewer. Returns raw XML.
     * Goes through the single-flight parse path so a concurrent scanner call shares the work.
     * Does not populate the memory cache with raw XML (the viewer re-reads from disk).
     */
    suspend fun getManifest(pkgName: Pkg.Name): ManifestData = withContext(dispatcherProvider.IO) {
        val appMeta = resolveAppMeta(pkgName) ?: return@withContext ManifestData(
            rawXml = RawXmlResult.Unavailable(UnavailableReason.PKG_NOT_FOUND),
            queries = QueriesResult.Error(IllegalStateException("Package not found: $pkgName")),
        )
        val key = ParseCacheKey(pkgName.value, appMeta.versionCode, appMeta.lastUpdateTime)

        manifestCache.get(pkgName, appMeta.versionCode, appMeta.lastUpdateTime)?.let {
            log(TAG) { "Full cache hit for $pkgName" }
            return@withContext it
        }

        val data = parseOrAwait(key, appMeta.sourceDir, pkgName, appMeta.versionCode, appMeta.lastUpdateTime)
        data
    }

    /**
     * Queries-only — used by the background hint scanner. Never retains raw XML on this path.
     * Memory cache holds only the [QueriesOutcome]. Disk cache (if hit) is read as the
     * queries-only sibling, no raw XML deserialization.
     */
    suspend fun getQueriesFor(pkgName: Pkg.Name): QueriesOutcome = withContext(dispatcherProvider.IO) {
        val appMeta = resolveAppMeta(pkgName) ?: return@withContext QueriesOutcome.Unavailable(UnavailableReason.PKG_NOT_FOUND)
        val key = ParseCacheKey(pkgName.value, appMeta.versionCode, appMeta.lastUpdateTime)

        synchronized(memoryCache) { memoryCache[key] }?.let {
            log(TAG) { "Queries memory cache hit for $pkgName" }
            return@withContext it
        }

        manifestCache.getQueries(pkgName, appMeta.versionCode, appMeta.lastUpdateTime)?.let { queries ->
            log(TAG) { "Queries disk cache hit for $pkgName" }
            val outcome = QueriesOutcome.Success(queries)
            synchronized(memoryCache) { memoryCache[key] = outcome }
            return@withContext outcome
        }

        val data = parseOrAwait(key, appMeta.sourceDir, pkgName, appMeta.versionCode, appMeta.lastUpdateTime)
        toOutcomeAndCache(key, data)
    }

    /**
     * Shared single-flight entry. Callers for the same [key] join the same deferred —
     * we only open and parse the APK once, and each caller takes what they need.
     */
    private suspend fun parseOrAwait(
        key: ParseCacheKey,
        sourceDir: String,
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
    ): ManifestData {
        val deferred: Deferred<ManifestData> = inFlightMutex.withLock {
            inFlight[key] ?: appScope.async(dispatcherProvider.IO) {
                try {
                    semaphore.withPermit {
                        log(TAG) { "Parsing APK for $pkgName at $sourceDir" }
                        val result = apkManifestReader.readManifest(sourceDir)
                        if (result.rawXml is RawXmlResult.Success) {
                            manifestCache.put(pkgName, versionCode, lastUpdateTime, result)
                        }
                        result
                    }
                } finally {
                    inFlightMutex.withLock { inFlight.remove(key) }
                }
            }.also { inFlight[key] = it }
        }
        return deferred.await()
    }

    private fun toOutcomeAndCache(key: ParseCacheKey, data: ManifestData): QueriesOutcome {
        val outcome = toOutcome(data)
        if (shouldMemoryCache(outcome)) {
            synchronized(memoryCache) { memoryCache[key] = outcome }
        }
        return outcome
    }

    private fun toOutcome(data: ManifestData): QueriesOutcome = when (val q = data.queries) {
        is QueriesResult.Success -> QueriesOutcome.Success(q.info)
        is QueriesResult.Error -> when (val raw = data.rawXml) {
            is RawXmlResult.Unavailable -> QueriesOutcome.Unavailable(raw.reason)
            is RawXmlResult.Error -> QueriesOutcome.Failure(raw.error)
            is RawXmlResult.Success -> QueriesOutcome.Failure(q.error)
        }
    }

    private fun shouldMemoryCache(outcome: QueriesOutcome): Boolean = when (outcome) {
        is QueriesOutcome.Success -> true
        is QueriesOutcome.Failure -> false                   // transient, let it retry
        is QueriesOutcome.Unavailable -> when (outcome.reason) {
            UnavailableReason.LOW_MEMORY -> false            // transient
            UnavailableReason.APK_NOT_FOUND,
            UnavailableReason.APK_NOT_READABLE,
            UnavailableReason.PKG_NOT_FOUND,
            UnavailableReason.APK_TOO_LARGE,
            UnavailableReason.MALFORMED_APK -> true          // stable until app updates
        }
    }

    private fun resolveAppMeta(pkgName: Pkg.Name): AppMeta? {
        return try {
            val pi = context.packageManager.getPackageInfo(pkgName.value, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pi.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pi.versionCode.toLong()
            }
            val sourceDir = pi.applicationInfo?.sourceDir ?: return null
            AppMeta(versionCode, pi.lastUpdateTime, sourceDir)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private data class AppMeta(val versionCode: Long, val lastUpdateTime: Long, val sourceDir: String)

    private data class ParseCacheKey(val pkg: String, val versionCode: Long, val lastUpdate: Long)

    companion object {
        private const val MEMORY_CACHE_SIZE = 30
        private val TAG = logTag("Apps", "Manifest", "Repo")
    }
}
