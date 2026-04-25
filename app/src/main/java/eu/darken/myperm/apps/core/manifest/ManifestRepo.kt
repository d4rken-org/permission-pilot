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

    // Serializes access to memoryCache. LinkedHashMap(accessOrder = true) mutates on read
    // (LRU touch), so reads must be serialized too. Kept separate from inFlightMutex to keep
    // in-flight bookkeeping isolated from cache access.
    private val cacheMutex = Mutex()

    /**
     * Full manifest — used by the manifest viewer. Returns raw XML.
     * Concurrent [getQueriesFor] callers can piggyback on this parse via the shared [fullInFlight] map.
     */
    suspend fun getManifest(pkgName: Pkg.Name): ManifestData = withContext(dispatcherProvider.IO) {
        val appMeta = resolveAppMeta(pkgName) ?: return@withContext ManifestData(
            rawXml = RawXmlResult.Unavailable(UnavailableReason.PKG_NOT_FOUND),
            queries = QueriesOutcome.Unavailable(UnavailableReason.PKG_NOT_FOUND),
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

        cacheMutex.withLock { memoryCache[key] }?.let {
            log(TAG) { "Queries memory cache hit for $pkgName" }
            return@withContext it
        }

        manifestCache.getQueries(pkgName, appMeta.versionCode, appMeta.lastUpdateTime)?.let { queries ->
            log(TAG) { "Queries disk cache hit for $pkgName" }
            val outcome = QueriesOutcome.Success(queries)
            cacheMutex.withLock { memoryCache[key] = outcome }
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
                        if (shouldMemoryCache(data.queries)) {
                            cacheMutex.withLock { memoryCache[key] = data.queries }
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
        // Atomic lookup across both in-flight maps — otherwise a viewer parse registering between
        // the `fullInFlight` check and the `queriesInFlight` insertion would be missed, causing a
        // duplicate parse for the same key.
        val parse: InFlight = inFlightMutex.withLock {
            fullInFlight[key]?.let { return@withLock InFlight.Full(it) }
            queriesInFlight[key]?.let { return@withLock InFlight.Queries(it) }
            val newDeferred = appScope.async(dispatcherProvider.IO) {
                try {
                    semaphore.withPermit {
                        log(TAG) { "Parsing queries for $pkgName at $sourceDir" }
                        val outcome = apkManifestReader.readQueries(sourceDir)
                        if (outcome is QueriesOutcome.Success) {
                            manifestCache.putQueries(pkgName, versionCode, lastUpdateTime, outcome.info)
                        }
                        if (shouldMemoryCache(outcome)) {
                            cacheMutex.withLock { memoryCache[key] = outcome }
                        }
                        outcome
                    }
                } finally {
                    inFlightMutex.withLock { queriesInFlight.remove(key) }
                }
            }
            queriesInFlight[key] = newDeferred
            InFlight.Queries(newDeferred)
        }
        return when (parse) {
            is InFlight.Full -> {
                log(TAG) { "Piggybacking queries on in-flight viewer parse for $pkgName" }
                parse.deferred.await().queries
            }
            is InFlight.Queries -> parse.deferred.await()
        }
    }

    private sealed class InFlight {
        data class Full(val deferred: Deferred<ManifestData>) : InFlight()
        data class Queries(val deferred: Deferred<QueriesOutcome>) : InFlight()
    }

    private fun shouldMemoryCache(outcome: QueriesOutcome): Boolean = when (outcome) {
        is QueriesOutcome.Success -> true
        is QueriesOutcome.Failure -> false                       // transient, let it retry
        is QueriesOutcome.Unavailable -> !outcome.reason.isTransient
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
