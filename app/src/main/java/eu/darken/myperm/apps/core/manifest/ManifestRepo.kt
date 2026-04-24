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
    // Bounded concurrency for unrelated-package parses. The streaming parser uses ~200 KB per
    // parse, so 4 concurrent parses cost well under 1 MB — the limit exists to keep a runaway
    // number of coroutines from all hitting disk at once, not for memory. Same-key callers
    // dedup through the in-flight maps below regardless.
    private val semaphore = Semaphore(MAX_CONCURRENT_PARSES)

    // Memory cache holds only the queries projection — raw XML is NOT retained in memory.
    // Keyed by (pkg, versionCode, lastUpdate) so app updates auto-invalidate.
    // Transient outcomes (LOW_MEMORY, Failure) are never cached so they're retried.
    private val memoryCache = object : LinkedHashMap<ParseCacheKey, QueriesOutcome>(MEMORY_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ParseCacheKey, QueriesOutcome>?) =
            size > MEMORY_CACHE_SIZE
    }

    private val inFlightMutex = Mutex()
    private val fullInFlight = HashMap<ParseCacheKey, Deferred<ManifestData>>()
    private val queriesInFlight = HashMap<ParseCacheKey, Deferred<QueriesOutcome>>()

    /**
     * Full manifest — used by the manifest viewer. Returns raw XML.
     * Concurrent [getQueriesFor] callers can piggyback on this parse via the shared [fullInFlight] map.
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

        parseFullOrAwait(key, appMeta.sourceDir, pkgName, appMeta.versionCode, appMeta.lastUpdateTime)
    }

    /**
     * Queries-only — used by the background hint scanner. Never retains raw XML on this path.
     * If a viewer parse is already in flight for the same key, await it and extract queries from
     * the shared result. Otherwise runs the lightweight scanner-only path.
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

        parseQueriesOrAwait(key, appMeta.sourceDir, pkgName, appMeta.versionCode, appMeta.lastUpdateTime)
    }

    private suspend fun parseFullOrAwait(
        key: ParseCacheKey,
        sourceDir: String,
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
    ): ManifestData {
        val deferred: Deferred<ManifestData> = inFlightMutex.withLock {
            fullInFlight[key] ?: appScope.async(dispatcherProvider.IO) {
                try {
                    semaphore.withPermit {
                        log(TAG) { "Parsing full manifest for $pkgName at $sourceDir" }
                        val data = apkManifestReader.readFullManifest(sourceDir, pkgName)
                        if (data.rawXml is RawXmlResult.Success) {
                            manifestCache.put(pkgName, versionCode, lastUpdateTime, data)
                        }
                        val outcome = toOutcome(data)
                        if (shouldMemoryCache(outcome)) {
                            synchronized(memoryCache) { memoryCache[key] = outcome }
                        }
                        data
                    }
                } finally {
                    inFlightMutex.withLock { fullInFlight.remove(key) }
                }
            }.also { fullInFlight[key] = it }
        }
        return deferred.await()
    }

    private suspend fun parseQueriesOrAwait(
        key: ParseCacheKey,
        sourceDir: String,
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
    ): QueriesOutcome {
        // If the viewer is already parsing this package, piggyback on its work — we can extract
        // queries from the full result without running a second parse.
        val fullShared = inFlightMutex.withLock { fullInFlight[key] }
        if (fullShared != null) {
            log(TAG) { "Piggybacking queries on in-flight viewer parse for $pkgName" }
            return toOutcome(fullShared.await())
        }

        val deferred: Deferred<QueriesOutcome> = inFlightMutex.withLock {
            queriesInFlight[key] ?: appScope.async(dispatcherProvider.IO) {
                try {
                    semaphore.withPermit {
                        log(TAG) { "Parsing queries for $pkgName at $sourceDir" }
                        val result = apkManifestReader.readQueries(sourceDir)
                        val outcome = queriesResultToOutcome(result)
                        if (result is QueriesReadResult.Success) {
                            manifestCache.putQueries(pkgName, versionCode, lastUpdateTime, result.info)
                        }
                        if (shouldMemoryCache(outcome)) {
                            synchronized(memoryCache) { memoryCache[key] = outcome }
                        }
                        outcome
                    }
                } finally {
                    inFlightMutex.withLock { queriesInFlight.remove(key) }
                }
            }.also { queriesInFlight[key] = it }
        }
        return deferred.await()
    }

    private fun queriesResultToOutcome(result: QueriesReadResult): QueriesOutcome = when (result) {
        is QueriesReadResult.Success -> QueriesOutcome.Success(result.info)
        is QueriesReadResult.Unavailable -> QueriesOutcome.Unavailable(result.reason)
        is QueriesReadResult.Error -> QueriesOutcome.Failure(result.error)
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
        private const val MAX_CONCURRENT_PARSES = 4
        private val TAG = logTag("Apps", "Manifest", "Repo")
    }
}
