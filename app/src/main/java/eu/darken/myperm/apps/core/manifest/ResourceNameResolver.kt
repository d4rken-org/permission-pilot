package eu.darken.myperm.apps.core.manifest

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.Pkg
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
        val appResources: Resources? = try {
            context.packageManager.getResourcesForApplication(pkgName.value)
        } catch (_: PackageManager.NameNotFoundException) {
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
            }
        }
    }

    companion object {
        private const val FRAMEWORK_PKG_ID = 0x01
        private const val FRAMEWORK_PKG_NAME = "android"
        private const val NULL_REF = -1 // 0xFFFFFFFF as signed Int
    }
}
