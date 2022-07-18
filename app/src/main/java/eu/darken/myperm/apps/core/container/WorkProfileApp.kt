package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.*
import android.os.Process
import android.os.UserHandle
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.ApkPkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.pkgId
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.permissions.core.Permission

class WorkProfileApp(
    override val id: Pkg.Id,
    override val packageInfo: PackageInfo,
    val launcherAppInfo: ApplicationInfo,
    val userHandle: UserHandle,
) : ApkPkg {

    override val sharedUserId: String?
        get() = TODO("Not yet implemented")

    override fun getPermission(id: Permission.Id): UsesPermission? {
        TODO("Not yet implemented")
    }

    override val requestedPermissions: Collection<UsesPermission>
        get() = TODO("Not yet implemented")

    override fun requestsPermission(id: Permission.Id): Boolean {
        TODO("Not yet implemented")
    }

    override val declaredPermissions: Collection<PermissionInfo>
        get() = TODO("Not yet implemented")

    override fun declaresPermission(id: Permission.Id): Boolean {
        TODO("Not yet implemented")
    }

}

fun Context.getTwinApps(): Map<UserHandle, List<WorkProfileApp>> {
    val launcherApps = getSystemService(LauncherApps::class.java)

    val profiles = launcherApps.profiles
    if (profiles.size < 2) return emptyMap()

    log { "Found multiple user profiles: $profiles" }
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

            WorkProfileApp(
                id = apkPkgInfo.pkgId,
                packageInfo = apkPkgInfo,
                launcherAppInfo = appInfo,
                userHandle = userHandle,
            )
        }

        userHandle to workProfileApps
    }.toMap()
}