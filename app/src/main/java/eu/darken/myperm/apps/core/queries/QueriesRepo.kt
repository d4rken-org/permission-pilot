package eu.darken.myperm.apps.core.queries

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.features.QueriesInfo
import eu.darken.myperm.apps.core.features.ReadableApk
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueriesRepo @Inject constructor(
    private val manifestParser: ManifestParser,
    private val dispatcherProvider: DispatcherProvider,
) {

    private val ioLimiter = Semaphore(3)
    private val cacheMutex = Mutex()
    private val cache = mutableMapOf<CacheKey, ManifestParser.QueriesResult>()

    private data class CacheKey(
        val pkgId: Pkg.Id,
        val versionCode: Long,
        val lastUpdateTime: Long,
    )

    suspend fun getQueries(pkg: BasePkg): ManifestParser.QueriesResult {
        val readableApk = pkg as? ReadableApk
            ?: return ManifestParser.QueriesResult.Unavailable("Not a readable APK")

        val key = CacheKey(
            pkgId = pkg.id,
            versionCode = readableApk.versionCode,
            lastUpdateTime = readableApk.packageInfo.lastUpdateTime,
        )

        cacheMutex.withLock { cache[key] }?.let { return it }

        val result = ioLimiter.withPermit {
            withContext(dispatcherProvider.IO) {
                parseFromApk(readableApk)
            }
        }

        cacheMutex.withLock {
            // Double-check: another coroutine may have cached this key while we were parsing
            cache.getOrPut(key) { result }
        }

        return result
    }

    private fun parseFromApk(pkg: ReadableApk): ManifestParser.QueriesResult {
        val sourceDir = pkg.applicationInfo?.sourceDir
            ?: return ManifestParser.QueriesResult.Unavailable("No sourceDir for ${pkg.packageName}")

        log(TAG, VERBOSE) { "Parsing queries for ${pkg.packageName} from $sourceDir" }

        val result = manifestParser.parseQueries(sourceDir)

        val finalResult = if (result is ManifestParser.QueriesResult.Success) {
            val splitDirs = pkg.applicationInfo?.splitSourceDirs
            if (!splitDirs.isNullOrEmpty()) {
                log(TAG, VERBOSE) { "${pkg.packageName} has ${splitDirs.size} split APKs, checking for queries" }
                var mergedInfo = result.queriesInfo
                for (splitDir in splitDirs) {
                    val splitResult = manifestParser.parseQueries(splitDir)
                    if (splitResult is ManifestParser.QueriesResult.Success && !splitResult.queriesInfo.isEmpty) {
                        mergedInfo = QueriesInfo(
                            packageQueries = mergedInfo.packageQueries + splitResult.queriesInfo.packageQueries,
                            intentQueries = mergedInfo.intentQueries + splitResult.queriesInfo.intentQueries,
                            providerQueries = mergedInfo.providerQueries + splitResult.queriesInfo.providerQueries,
                        )
                    }
                }
                ManifestParser.QueriesResult.Success(mergedInfo)
            } else {
                result
            }
        } else {
            result
        }

        when (finalResult) {
            is ManifestParser.QueriesResult.Success -> {
                log(TAG) { "${pkg.packageName}: ${finalResult.queriesInfo.totalCount} queries found" }
            }
            is ManifestParser.QueriesResult.Unavailable -> {
                log(TAG) { "${pkg.packageName}: queries unavailable - ${finalResult.reason}" }
            }
            is ManifestParser.QueriesResult.ParseError -> {
                log(TAG, ERROR) { "${pkg.packageName}: parse error - ${finalResult.error}" }
            }
        }

        return finalResult
    }

    suspend fun clearCache() {
        cacheMutex.withLock { cache.clear() }
    }

    companion object {
        private val TAG = logTag("Apps", "Queries", "Repo")
    }
}
