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
 * Corruption in one file does not evict the valid sibling. On a queries-only miss we
 * lazily backfill the sibling from the full cache if it is present and version-valid
 * (prevents a reparse storm after upgrade to a version that writes both files).
 */
@Singleton
class ManifestCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val cacheDir: File
        get() = File(context.cacheDir, "manifests").also { it.mkdirs() }

    /** Full cache read. Returns null on miss, version mismatch, or corruption. */
    fun get(
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
    ): ManifestData? {
        val file = fullCacheFile(pkgName)
        if (!file.exists()) return null

        return try {
            val cached = json.decodeFromString<CachedManifest>(file.readText())
            if (cached.versionCode != versionCode || cached.lastUpdateTime != lastUpdateTime) {
                log(TAG) { "Full cache stale for $pkgName: version/time mismatch" }
                file.delete()
                return null
            }
            ManifestData(
                rawXml = RawXmlResult.Success(cached.rawXml),
                queries = cached.queries?.let { QueriesResult.Success(it) }
                    ?: QueriesResult.Error(IllegalStateException("Queries not cached")),
            )
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to read full cache for $pkgName: $e" }
            file.delete()
            null
        }
    }

    /**
     * Queries-only cache read. Never deserializes raw XML. Falls back to backfilling from
     * the full cache if the sibling is missing but the full file is present and valid.
     */
    fun getQueries(
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
    ): QueriesInfo? {
        val siblingFile = queriesCacheFile(pkgName)
        if (siblingFile.exists()) {
            try {
                val cached = json.decodeFromString<CachedQueries>(siblingFile.readText())
                if (cached.versionCode != versionCode || cached.lastUpdateTime != lastUpdateTime) {
                    log(TAG) { "Queries cache stale for $pkgName: version/time mismatch" }
                    siblingFile.delete()
                } else {
                    return cached.queries
                }
            } catch (e: Exception) {
                log(TAG, WARN) { "Failed to read queries cache for $pkgName: $e" }
                siblingFile.delete()
                // fall through to backfill attempt
            }
        }

        // Lazy backfill from the full cache, if available and version-valid. Avoids a
        // reparse storm on upgrade when only the old full `.json` files exist.
        val fullFile = fullCacheFile(pkgName)
        if (!fullFile.exists()) return null
        return try {
            val cached = json.decodeFromString<CachedManifest>(fullFile.readText())
            if (cached.versionCode != versionCode || cached.lastUpdateTime != lastUpdateTime) {
                return null
            }
            val queries = cached.queries ?: return null
            // Write the sibling so future reads skip the rawXml deserialization.
            try {
                siblingFile.writeText(
                    json.encodeToString(
                        CachedQueries.serializer(),
                        CachedQueries(versionCode, lastUpdateTime, queries),
                    )
                )
            } catch (e: Exception) {
                log(TAG, WARN) { "Failed to backfill queries cache for $pkgName: $e" }
            }
            queries
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to backfill-read full cache for $pkgName: $e" }
            null
        }
    }

    fun put(
        pkgName: Pkg.Name,
        versionCode: Long,
        lastUpdateTime: Long,
        data: ManifestData,
    ) {
        val rawXml = (data.rawXml as? RawXmlResult.Success)?.xml ?: return
        val queries = (data.queries as? QueriesResult.Success)?.info

        try {
            val cached = CachedManifest(
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
            try {
                val siblingPayload = CachedQueries(versionCode, lastUpdateTime, queries)
                queriesCacheFile(pkgName).writeText(
                    json.encodeToString(CachedQueries.serializer(), siblingPayload)
                )
            } catch (e: Exception) {
                log(TAG, WARN) { "Failed to write queries cache for $pkgName: $e" }
            }
        }
    }

    private fun fullCacheFile(pkgName: Pkg.Name): File = File(cacheDir, "${pkgName.value}.json")

    private fun queriesCacheFile(pkgName: Pkg.Name): File = File(cacheDir, "${pkgName.value}.queries.json")

    @Serializable
    private data class CachedManifest(
        val versionCode: Long,
        val lastUpdateTime: Long,
        val rawXml: String,
        val queries: QueriesInfo?,
    )

    @Serializable
    private data class CachedQueries(
        val versionCode: Long,
        val lastUpdateTime: Long,
        val queries: QueriesInfo,
    )

    companion object {
        private val TAG = logTag("Apps", "Manifest", "Cache")
    }
}
