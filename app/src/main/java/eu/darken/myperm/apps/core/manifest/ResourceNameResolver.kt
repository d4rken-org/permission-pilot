package eu.darken.myperm.apps.core.manifest

import android.content.Context
import android.content.res.Resources
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves framework and app resource IDs to symbolic names (`@string/foo`, `@android:drawable/bar`,
 * `@com.other:string/baz`) using the public `PackageManager.getResourcesForApplication` API.
 *
 * Zero Java heap footprint for resolution — the framework holds resource metadata in native mmap'd
 * memory and only the short name string per lookup crosses into the JVM.
 */
@Singleton
class ResourceNameResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun forPackage(pkgName: Pkg.Name): ResolverScope {
        // PackageManager binders can throw a wider set than NameNotFoundException —
        // DeadObjectException after a system_server hiccup, IllegalArgumentException for apps
        // on currently-unmounted secondary storage, etc. Treat all of them as "no app
        // resources" so the viewer falls back to raw 0xNNNNNNNN refs instead of failing
        // the whole load.
        val appResources: Resources? = try {
            context.packageManager.getResourcesForApplication(pkgName.value)
        } catch (e: Exception) {
            log(TAG, WARN) { "getResourcesForApplication failed for ${pkgName.value}: $e" }
            null
        }
        return ResolverScope(appResources, context.resources, pkgName)
    }

    class ResolverScope internal constructor(
        private val appResources: Resources?,
        private val frameworkResources: Resources,
        private val ownerPkgName: Pkg.Name,
    ) : ResourceRefResolver {
        private val cache = HashMap<Int, String?>()

        override fun resolve(resourceId: Int): String? {
            if (resourceId == 0 || resourceId == NULL_REF) return null
            if (cache.containsKey(resourceId)) return cache[resourceId]
            val result = tryResolve(resourceId)
            cache[resourceId] = result
            return result
        }

        private fun tryResolve(resourceId: Int): String? {
            val pkgId = resourceId ushr 24
            val target = when (pkgId) {
                FRAMEWORK_PKG_ID -> frameworkResources
                else -> appResources ?: return null
            }
            return try {
                val type = target.getResourceTypeName(resourceId)
                val entry = target.getResourceEntryName(resourceId)
                val pkg = target.getResourcePackageName(resourceId)
                when {
                    pkgId == FRAMEWORK_PKG_ID || pkg == FRAMEWORK_PKG_NAME -> "@android:$type/$entry"
                    pkg == ownerPkgName.value -> "@$type/$entry"
                    else -> "@$pkg:$type/$entry"
                }
            } catch (_: Resources.NotFoundException) {
                null
            } catch (_: RuntimeException) {
                // Mirror of the broadened catch in forPackage: a per-id lookup can fail with
                // RuntimeExceptions other than NotFoundException (system_server transient
                // failures). Degrade to an unresolved reference rather than aborting the parse.
                null
            }
        }
    }

    companion object {
        private const val FRAMEWORK_PKG_ID = 0x01
        private const val FRAMEWORK_PKG_NAME = "android"
        private const val NULL_REF = -1 // 0xFFFFFFFF as signed Int
        private val TAG = logTag("Apps", "Manifest", "ResourceResolver")
    }
}
