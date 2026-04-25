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
 * - `<pkg>.json` — per-section pretty XML, used by the manifest viewer.
 * - `<pkg>.queries.json` — queries projection only, used by the hint scanner.
 *
 * The full cache carries a [CachedManifest.formatVersion] to guard against format drift.
 * v2 entries (which stored the rendered raw XML before the streaming section visitor
 * landed) fail decode against v3's required `sections` field and are deleted by the
 * corruption catch on read.
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
     * Full cache read. Returns null on miss, version mismatch, or corruption (v2 entries
     * fall through here because the schema gained a required `sections` field).
     * `isFlagged` is left `false` on read; the caller is expected to recompute flags from
     * the live queries projection.
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
            val sections = cached.sections.map { it.toUiModel(isFlagged = false) }
            ManifestData(
                sections = SectionsResult.Success(sections),
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
     * Queries-only cache read. Never deserializes the section payload; never backfills from
     * the full cache (a backfill would deserialize the entire section list just to extract
     * queries, which defeats the memory win the sibling file exists for).
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
        val sections = (data.sections as? SectionsResult.Success)?.sections ?: return
        val queries = (data.queries as? QueriesOutcome.Success)?.info

        try {
            val cached = CachedManifest(
                formatVersion = CURRENT_FORMAT_VERSION,
                versionCode = versionCode,
                lastUpdateTime = lastUpdateTime,
                sections = sections.map { it.toCacheModel() },
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
     * silently deserialize as "v3" via a non-null default. kotlinx.serialization fills a missing
     * field with the declared default, so the default must be `null` to be distinguishable from a
     * written v3 entry. On read we reject anything that isn't exactly [CURRENT_FORMAT_VERSION].
     *
     * v2 entries (`rawXml: String` instead of `sections: List<CachedManifestSection>`) fail decode
     * here because `sections` has no default; the corruption catch in [get] then deletes them.
     */
    @Serializable
    internal data class CachedManifest(
        val formatVersion: Int? = null,
        val versionCode: Long,
        val lastUpdateTime: Long,
        val sections: List<CachedManifestSection>,
        val queries: QueriesInfo?,
    )

    @Serializable
    internal data class CachedQueries(
        val versionCode: Long,
        val lastUpdateTime: Long,
        val queries: QueriesInfo,
    )

    companion object {
        internal const val CURRENT_FORMAT_VERSION = 3
        private val TAG = logTag("Apps", "Manifest", "Cache")
    }
}
