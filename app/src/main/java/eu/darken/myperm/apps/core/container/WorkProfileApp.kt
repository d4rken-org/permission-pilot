package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.*
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.ApkPkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.pkgId
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.Logging.Priority.INFO
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.permissions.core.Permission

data class WorkProfileApp(
    override val id: Pkg.Id,
    override val packageInfo: PackageInfo,
    val launcherAppInfo: ApplicationInfo,
    val userHandle: UserHandle,
) : ApkPkg {

    override fun getLabel(context: Context): String {
        val pm = context.packageManager
        return launcherAppInfo.loadLabel(pm).toString()
    }

    override fun getIcon(context: Context): Drawable {
        val pm = context.packageManager
        return launcherAppInfo.loadIcon(pm)
    }

    override val sharedUserId: String?
        get() = throw NotImplementedError()

    override fun getPermission(id: Permission.Id): UsesPermission? {
        throw NotImplementedError()
    }

    override val requestedPermissions: Collection<UsesPermission>
        get() = throw NotImplementedError()

    override fun requestsPermission(id: Permission.Id): Boolean {
        throw NotImplementedError()
    }

    override val declaredPermissions: Collection<PermissionInfo>
        get() = throw NotImplementedError()

    override fun declaresPermission(id: Permission.Id): Boolean {
        throw NotImplementedError()
    }

}

fun Context.getTwinApps(): Map<UserHandle, List<WorkProfileApp>> {
    val launcherApps = getSystemService(LauncherApps::class.java)

    val profiles = launcherApps.profiles
    if (profiles.size < 2) return emptyMap()

    log(AppRepo.TAG, INFO) { "Found multiple user profiles: $profiles" }
    val extraProfiles = profiles - Process.myUserHandle()

    return extraProfiles.map outerMap@{ userHandle ->
        val launcherInfos = launcherApps.getActivityList(null, userHandle)

        val workProfileApps = launcherInfos.mapNotNull { lai ->
            val appInfo = lai.applicationInfo

            val apkPkgInfo = packageManager.getPackageArchiveInfo(appInfo.sourceDir, PackageManager.GET_PERMISSIONS)
            if (apkPkgInfo == null) {
                log(ERROR) { "Failed to read APK: ${appInfo.sourceDir}" }
                return@mapNotNull null
            }

            val app = WorkProfileApp(
                id = apkPkgInfo.pkgId,
                packageInfo = apkPkgInfo,
                launcherAppInfo = appInfo,
                userHandle = userHandle,
            )
            log(AppRepo.TAG) { "Processed PKG: $app" }
            app
        }

        userHandle to workProfileApps
    }.toMap()
}