package eu.darken.myperm.apps.core.manifest

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-tier on-disk cache:
 * - `<pkg>.json` — full manifest including raw XML, used by the manifest viewer.
 * - `<pkg>.queries.json` — queries projection only, used by the hint scanner.
 *
 * The full cache carries a [CachedManifest.formatVersion] to guard against format drift.
 * Entries that predate v2 (written by the old `apk-parser`-based reader) are deleted on read
 * — their attribute/reference formatting is not guaranteed to match the new renderer, and
 * blindly trusting them would leak old-format XML to the viewer.
 *
 * The queries sibling is a pure projection and is NOT version-gated — its schema has not
 * changed with the parser rewrite.
 *
 * Corruption of one file does not evict the valid sibling.
 */
@Singleton
class ManifestCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val cacheDir: File
        get() = File(context.cacheDir, "manifests").also { it.mkdirs() }

    /**
     * Full cache read. Returns null on miss, version mismatch, corruption, or pre-v2 format.
     * Pre-v2 entries are deleted to stop them being considered on subsequent reads.
     */
    fun get(
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
    ): ManifestData? {
        val file = fullCacheFile(pkgName)
        if (!file.exists()) return null

        return try {
            val cached = json.decodeFromString<CachedManifest>(file.readText())
            if (cached.formatVersion != CURRENT_FORMAT_VERSION) {
                log(TAG) { "Full cache format stale for $pkgName: v${cached.formatVersion} != v$CURRENT_FORMAT_VERSION" }
                file.delete()
                return null
            }
            if (cached.versionCode != versionCode || cached.lastUpdateTime != lastUpdateTime) {
                log(TAG) { "Full cache stale for $pkgName: version/time mismatch" }
                file.delete()
                return null
            }
            ManifestData(
                rawXml = RawXmlResult.Success(cached.rawXml),
                queries = cached.queries?.let { QueriesOutcome.Success(it) }
                    ?: QueriesOutcome.Failure(IllegalStateException("Queries not cached")),
            )
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to read full cache for $pkgName: $e" }
            file.delete()
            null
        }
    }

    /**
     * Queries-only cache read. Never deserializes raw XML; never backfills from the full cache
     * (would defeat the memory win — a backfill would deserialize the old rawXml just to extract
     * queries, which is exactly what we're trying to avoid).
     *
     * On a miss, callers re-parse the APK via the lightweight [ApkManifestReader.readQueries] path.
     */
    fun getQueries(
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
    ): QueriesInfo? {
        val siblingFile = queriesCacheFile(pkgName)
        if (!siblingFile.exists()) return null

        return try {
            val cached = json.decodeFromString<CachedQueries>(siblingFile.readText())
            if (cached.versionCode != versionCode || cached.lastUpdateTime != lastUpdateTime) {
                log(TAG) { "Queries cache stale for $pkgName: version/time mismatch" }
                siblingFile.delete()
                null
            } else {
                cached.queries
            }
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to read queries cache for $pkgName: $e" }
            siblingFile.delete()
            null
        }
    }

    /** Writes both the full cache (with current format version) and the queries sibling. */
    fun put(
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
        data: ManifestData,
    ) {
        val rawXml = (data.rawXml as? RawXmlResult.Success)?.xml ?: return
        val queries = (data.queries as? QueriesOutcome.Success)?.info

        try {
            val cached = CachedManifest(
                formatVersion = CURRENT_FORMAT_VERSION,
                versionCode = versionCode,
                lastUpdateTime = lastUpdateTime,
                rawXml = rawXml,
                queries = queries,
            )
            fullCacheFile(pkgName).writeText(json.encodeToString(CachedManifest.serializer(), cached))
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to write full cache for $pkgName: $e" }
        }

        if (queries != null) {
            writeQueriesSibling(pkgName, versionCode, lastUpdateTime, queries)
        }
    }

    /** Writes only the queries sibling — used by the scanner-only path. */
    fun putQueries(
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
        queries: QueriesInfo,
    ) {
        writeQueriesSibling(pkgName, versionCode, lastUpdateTime, queries)
    }

    private fun writeQueriesSibling(
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
        queries: QueriesInfo,
    ) {
        try {
            val payload = CachedQueries(versionCode, lastUpdateTime, queries)
            queriesCacheFile(pkgName).writeText(
                json.encodeToString(CachedQueries.serializer(), payload)
            )
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to write queries cache for $pkgName: $e" }
        }
    }

    private fun fullCacheFile(pkgName: Pkg.Name): File = File(cacheDir, "${pkgName.value}.json")

    private fun queriesCacheFile(pkgName: Pkg.Name): File = File(cacheDir, "${pkgName.value}.queries.json")

    /**
     * `formatVersion` is nullable so that pre-migration JSONs (which have no such field) do NOT
     * silently deserialize as "v2" via a non-null default. kotlinx.serialization fills a missing
     * field with the declared default, so the default must be `null` to be distinguishable from a
     * written v2 entry. On read we reject anything that isn't exactly [CURRENT_FORMAT_VERSION].
     */
    @Serializable
    internal data class CachedManifest(
        val formatVersion: Int? = null,
        val versionCode: Long,
        val lastUpdateTime: Long,
        val rawXml: String,
        val queries: QueriesInfo?,
    )

    @Serializable
    internal data class CachedQueries(
        val versionCode: Long,
        val lastUpdateTime: Long,
        val queries: QueriesInfo,
    )

    companion object {
        internal const val CURRENT_FORMAT_VERSION = 2
        private val TAG = logTag("Apps", "Manifest", "Cache")
    }
}
