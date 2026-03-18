package eu.darken.myperm.apps.core.manifest

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManifestCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val cacheDir: File
        get() = File(context.cacheDir, "manifests").also { it.mkdirs() }

    fun get(
        pkgName: String,
        versionCode: Long,
        lastUpdateTime: Long,
    ): ManifestData? {
        val file = cacheFile(pkgName)
        if (!file.exists()) return null

        return try {
            val cached = json.decodeFromString<CachedManifest>(file.readText())
            if (cached.versionCode != versionCode || cached.lastUpdateTime != lastUpdateTime) {
                log(TAG) { "Cache stale for $pkgName: version/time mismatch" }
                file.delete()
                return null
            }
            ManifestData(
                rawXml = RawXmlResult.Success(cached.rawXml),
                queries = cached.queries?.let { QueriesResult.Success(it) }
                    ?: QueriesResult.Error(IllegalStateException("Queries not cached")),
            )
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to read cache for $pkgName: $e" }
            file.delete()
            null
        }
    }

    fun put(
        pkgName: String,
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
            cacheFile(pkgName).writeText(json.encodeToString(CachedManifest.serializer(), cached))
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to write cache for $pkgName: $e" }
        }
    }

    private fun cacheFile(pkgName: String): File {
        return File(cacheDir, "${pkgName}.json")
    }

    @Serializable
    private data class CachedManifest(
        val versionCode: Long,
        val lastUpdateTime: Long,
        val rawXml: String,
        val queries: QueriesInfo?,
    )

    companion object {
        private val TAG = logTag("Apps", "Manifest", "Cache")
    }
}
