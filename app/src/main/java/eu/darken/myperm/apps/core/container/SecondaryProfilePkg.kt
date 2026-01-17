package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.GET_UNINSTALLED_PACKAGES_COMPAT
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.*
import eu.darken.myperm.apps.core.isSystemApp
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.debug.logging.Logging.Priority.*
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.APerm
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SecondaryProfilePkg(
    override val packageInfo: PackageInfo,
    override val userHandle: UserHandle,
    override val installerInfo: InstallerInfo,
    val launcherAppInfo: ApplicationInfo,
    val extraPermissions: Collection<UsesPermission>,
) : BasePkg(), SecondaryPkg {

    override val id: Pkg.Id = Pkg.Id(packageInfo.packageName, userHandle)


    private var _label: String? = null
    override fun getLabel(context: Context): String {
        _label?.let { return it }
        val pm = context.packageManager
        val newLabel = try {
            val loadedLabel = launcherAppInfo.loadLabel(pm).toString()
            pm.getUserBadgedLabel(loadedLabel, userHandle).toString()
        } catch (_: Exception) {
            null
        }
            ?: twins.firstNotNullOfOrNull { it.getLabel(context) }
            ?: super.getLabel(context)
            ?: id.pkgName
        _label = newLabel
        return newLabel
    }

    override fun getIcon(context: Context): Drawable {
        val pm = context.packageManager
        return try {
            val loadedIcon = launcherAppInfo.loadIcon(pm)
            if (loadedIcon != null) {
                pm.getUserBadgedIcon(loadedIcon, userHandle)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
            ?: twins.firstNotNullOfOrNull { it.getIcon(context) }
            ?: super.getIcon(context)
            ?: context.getDrawable(R.drawable.ic_default_app_icon_24)!!
    }

    override var siblings: Collection<Pkg> = emptyList()
    override var twins: Collection<Installed> = emptyList()

    override val requestedPermissions: Collection<UsesPermission> by lazy {
        val base = packageInfo.requestedPermissions?.mapIndexed { _, permissionId ->
            UsesPermission.Unknown(id = Permission.Id(permissionId))
        } ?: emptyList()
        val acsPermissions = accessibilityServices.map {
            UsesPermission.WithState(
                id = APerm.BIND_ACCESSIBILITY_SERVICE.id,
                flags = null
            )
        }
        base + acsPermissions
    }

    override val declaredPermissions: Collection<PermissionInfo> by lazy {
        packageInfo.permissions?.toSet() ?: emptyList()
    }

    override val internetAccess: InternetAccess = InternetAccess.UNKNOWN

    override val isSystemApp: Boolean = (applicationInfo?.isSystemApp ?: true) || twins.any { it.isSystemApp }

    override fun toString(): String = "SecondaryProfilePkg(packageName=$packageName, userHandle=$userHandle)"
}

suspend fun getSecondaryProfilePkgs(ipcFunnel: IPCFunnel): Collection<BasePkg> = coroutineScope {

    val profiles = ipcFunnel.userManager.userProfiles()

    if (profiles.size < 2) return@coroutineScope emptySet()

    log(AppRepo.TAG, INFO) { "Found multiple user profiles: $profiles" }
    val extraProfiles = profiles - Process.myUserHandle()

    suspend fun determineForHandle(userHandle: UserHandle): Collection<BasePkg> {
        val launcherInfos = try {
            ipcFunnel.launcherApps.getActivityList(null, userHandle)
        } catch (e: SecurityException) {
            log(AppRepo.TAG, ERROR) { "Failed to retrieve activity list for $userHandle" }
            emptyList()
        }

        return launcherInfos.mapNotNull { lai ->
            val appInfo = lai.applicationInfo

            var pkgInfo = ipcFunnel.packageManager.getPackageArchiveInfo(
                appInfo.packageName,
                GET_UNINSTALLED_PACKAGES_COMPAT
            )

            if (pkgInfo == null) {
                log(AppRepo.TAG, VERBOSE) { "Failed to get info from packagemanager for $appInfo" }
                pkgInfo =
                    ipcFunnel.packageManager.getPackageArchiveInfo(appInfo.sourceDir, PackageManager.GET_PERMISSIONS)
            }

            if (pkgInfo == null) {
                log(AppRepo.TAG, ERROR) { "Failed to read APK: ${appInfo.sourceDir}" }
                return@mapNotNull null
            }

            SecondaryProfilePkg(
                packageInfo = pkgInfo,
                installerInfo = pkgInfo.getInstallerInfo(ipcFunnel),
                launcherAppInfo = appInfo,
                userHandle = userHandle,
                extraPermissions = pkgInfo.determineSpecialPermissions(ipcFunnel),
            ).also { log(AppRepo.TAG) { "PKG[profile=${userHandle}}: $it" } }
        }
    }

    extraProfiles
        .map { userHandle ->
            async { determineForHandle(userHandle) }
        }
        .awaitAll()
        .flatten()
}
