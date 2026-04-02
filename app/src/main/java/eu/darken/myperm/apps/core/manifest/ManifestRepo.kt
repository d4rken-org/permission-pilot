package eu.darken.myperm.apps.core.manifest

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManifestRepo @Inject constructor(
    private val apkManifestReader: ApkManifestReader,
    private val manifestCache: ManifestCache,
    private val dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
) {
    private val semaphore = Semaphore(2)
    private val memoryCache = object : LinkedHashMap<String, ManifestData>(30, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ManifestData>?) = size > 30
    }

    suspend fun getManifest(pkgName: Pkg.Name): ManifestData = withContext(dispatcherProvider.IO) {
        val cacheKey = pkgName.value

        synchronized(memoryCache) { memoryCache[cacheKey] }?.let {
            log(TAG) { "Memory cache hit for $pkgName" }
            return@withContext it
        }

        val appMeta = resolveAppMeta(pkgName)
        if (appMeta == null) {
            log(TAG) { "Package not found: $pkgName" }
            return@withContext ManifestData(
                rawXml = RawXmlResult.Unavailable(UnavailableReason.PKG_NOT_FOUND),
                queries = QueriesResult.Error(IllegalStateException("Package not found: $pkgName")),
            )
        }

        manifestCache.get(pkgName, appMeta.versionCode, appMeta.lastUpdateTime)?.let { cached ->
            log(TAG) { "Disk cache hit for $pkgName" }
            synchronized(memoryCache) { memoryCache[cacheKey] = cached }
            return@withContext cached
        }

        semaphore.withPermit {
            // Double-check memory cache after acquiring permit
            synchronized(memoryCache) { memoryCache[cacheKey] }?.let { return@withPermit it }

            log(TAG) { "Parsing APK for $pkgName at ${appMeta.sourceDir}" }
            val result = apkManifestReader.readManifest(appMeta.sourceDir)
            // Only cache successful parses — transient failures (LOW_MEMORY) should be retried
            if (result.rawXml is RawXmlResult.Success) {
                manifestCache.put(pkgName, appMeta.versionCode, appMeta.lastUpdateTime, result)
                synchronized(memoryCache) { memoryCache[cacheKey] = result }
            }
            result
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

    companion object {
        private val TAG = logTag("Apps", "Manifest", "Repo")
    }
}
