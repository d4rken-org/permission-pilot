package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.*
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.GET_UNINSTALLED_PACKAGES_COMPAT
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.*
import eu.darken.myperm.common.debug.logging.Logging.Priority.*
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.permissions.core.Permission

data class SecondaryProfilePkg(
    override val packageInfo: PackageInfo,
    override val userHandle: UserHandle,
    override val installerInfo: InstallerInfo,
    val launcherAppInfo: ApplicationInfo,
) : BasePkg(), SecondaryPkg {

    override val id: Pkg.Id = Pkg.Id(packageInfo.packageName, userHandle)


    private var _label: String? = null
    override fun getLabel(context: Context): String {
        _label?.let { return it }
        val pm = context.packageManager
        val newLabel = pm.getUserBadgedLabel(launcherAppInfo.loadLabel(pm).toString(), userHandle).toString()
        _label = newLabel
        return newLabel
    }

    override fun getIcon(context: Context): Drawable {
        val pm = context.packageManager
        return pm.getUserBadgedIcon(launcherAppInfo.loadIcon(pm), userHandle)
    }

    override var siblings: Collection<Pkg> = emptyList()
    override var twins: Collection<Installed> = emptyList()

    override val requestedPermissions: Collection<UsedPermissionStateful> by lazy {
        packageInfo.requestedPermissions?.mapIndexed { _, permissionId ->
            UsedPermissionStateful(
                id = Permission.Id(permissionId),
                flags = null,  // We don't know for secondary profiles
            )
        } ?: emptyList()
    }

    override val declaredPermissions: Collection<PermissionInfo> by lazy {
        packageInfo.permissions?.toSet() ?: emptyList()
    }

    override val internetAccess: InternetAccess = InternetAccess.UNKNOWN

    override val isSystemApp: Boolean
        get() = super.isSystemApp || twins.any { it.isSystemApp }
}

fun Context.getSecondaryProfilePkgs(): Collection<BasePkg> {
    val launcherApps = ContextCompat.getSystemService(this, LauncherApps::class.java)!!
    val userManager = ContextCompat.getSystemService(this, UserManager::class.java)!!

    val profiles = userManager.userProfiles

    if (profiles.size < 2) return emptySet()

    log(AppRepo.TAG, INFO) { "Found multiple user profiles: $profiles" }
    val extraProfiles = profiles - Process.myUserHandle()

    return extraProfiles.map outerMap@{ userHandle ->
        val launcherInfos = try {
            launcherApps.getActivityList(null, userHandle)
        } catch (e: SecurityException) {
            log(AppRepo.TAG, ERROR) { "Failed to retrieve activity list for $userHandle" }
            emptyList()
        }

        launcherInfos.mapNotNull { lai ->
            val appInfo = lai.applicationInfo

            var pkgInfo = packageManager.getPackageArchiveInfo(
                appInfo.packageName,
                packageManager.GET_UNINSTALLED_PACKAGES_COMPAT
            )

            if (pkgInfo == null) {
                log(AppRepo.TAG, VERBOSE) { "Failed to get info from packagemanager for $appInfo" }
                pkgInfo = packageManager.getPackageArchiveInfo(appInfo.sourceDir, PackageManager.GET_PERMISSIONS)
            }

            if (pkgInfo == null) {
                log(AppRepo.TAG, ERROR) { "Failed to read APK: ${appInfo.sourceDir}" }
                return@mapNotNull null
            }

            val app = SecondaryProfilePkg(
                packageInfo = pkgInfo,
                installerInfo = pkgInfo.getInstallerInfo(packageManager),
                launcherAppInfo = appInfo,
                userHandle = userHandle,
            )
            log(AppRepo.TAG) { "PKG[profile=${userHandle}}: $app" }
            app
        }
    }.flatten()
}

