package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.AccessibilityService
import eu.darken.myperm.apps.core.features.InstallerInfo
import eu.darken.myperm.apps.core.features.Installed
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.features.SecondaryPkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.features.determineSpecialPermissions
import eu.darken.myperm.apps.core.features.getInstallerInfo
import eu.darken.myperm.apps.core.features.getSpecialPermissionStatuses
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
    private val specialPermissionStatuses: Map<Permission.Id, UsesPermission.Status> = emptyMap(),
) : BasePkg(), SecondaryPkg {

    override val id: Pkg.Id = Pkg.Id(Pkg.Name(packageInfo.packageName), userHandle)


    @Volatile private var _label: String? = null
    @Volatile private var _resolvingLabel = false
    override fun getLabel(context: Context): String {
        _label?.let { return it }
        if (_resolvingLabel) return id.pkgName.value
        _resolvingLabel = true
        try {
            val pm = context.packageManager
            val newLabel = try {
                val loadedLabel = launcherAppInfo.loadLabel(pm).toString()
                pm.getUserBadgedLabel(loadedLabel, userHandle).toString()
            } catch (_: Exception) {
                null
            }
                ?: twins.firstNotNullOfOrNull { it.getLabel(context) }
                ?: super.getLabel(context)
                ?: id.pkgName.value
            _label = newLabel
            return newLabel
        } catch (e: Exception) {
            val fallback = id.pkgName.value
            _label = fallback
            return fallback
        } finally {
            _resolvingLabel = false
        }
    }

    private var _resolvingIcon = false
    override fun getIcon(context: Context): Drawable? {
        if (_resolvingIcon) return null
        _resolvingIcon = true
        return try {
            val pm = context.packageManager
            try {
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
        } finally {
            _resolvingIcon = false
        }
    }

    override var siblings: Collection<Pkg> = emptyList()
    override var twins: Collection<Installed> = emptyList()

    override val requestedPermissions: Collection<UsesPermission> by lazy {
        val base = packageInfo.requestedPermissions?.map { permissionId ->
            val permId = Permission.Id(permissionId)
            val overrideStatus = specialPermissionStatuses[permId]
            if (overrideStatus != null) {
                UsesPermission.WithState(id = permId, flags = null, overrideStatus = overrideStatus)
            } else {
                UsesPermission.Unknown(id = permId)
            }
        } ?: emptyList()
        val acsPermissions = accessibilityServices.map {
            UsesPermission.WithState(
                id = APerm.BIND_ACCESSIBILITY_SERVICE.id,
                flags = null
            )
        }
        val deviceAdminPermissions = deviceAdmins.map {
            UsesPermission.WithState(
                id = APerm.BIND_DEVICE_ADMIN.id,
                flags = null
            )
        }
        base + acsPermissions + deviceAdminPermissions
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

        // LauncherApps.getActivityList returns one entry per launcher activity, so apps
        // with multiple launcher activities yield multiple entries for the same package.
        // Group by package and build one SecondaryProfilePkg per group, falling through to
        // the next activity in the same group only if the current one's lookup fails.
        return launcherInfos
            .groupBy { it.applicationInfo.packageName }
            .values
            .mapNotNull { activitiesForPkg ->
                activitiesForPkg.firstNotNullOfOrNull { lai ->
                    val appInfo = lai.applicationInfo

                    val pkgInfo = ipcFunnel.packageManager.getPackageInfo(
                        appInfo.packageName,
                        PackageManager.GET_PERMISSIONS,
                    ) ?: ipcFunnel.packageManager.getPackageArchiveInfo(
                        appInfo.sourceDir,
                        PackageManager.GET_PERMISSIONS,
                    )

                    if (pkgInfo == null) {
                        log(AppRepo.TAG, ERROR) { "Failed to read APK: ${appInfo.sourceDir}" }
                        return@firstNotNullOfOrNull null
                    }

                    SecondaryProfilePkg(
                        packageInfo = pkgInfo,
                        installerInfo = pkgInfo.getInstallerInfo(ipcFunnel),
                        launcherAppInfo = appInfo,
                        userHandle = userHandle,
                        extraPermissions = pkgInfo.determineSpecialPermissions(ipcFunnel, uidOverride = appInfo.uid),
                        specialPermissionStatuses = pkgInfo.getSpecialPermissionStatuses(ipcFunnel, uidOverride = appInfo.uid),
                    ).also { log(AppRepo.TAG) { "PKG[profile=${userHandle}}: $it" } }
                }
            }
    }

    extraProfiles
        .map { userHandle ->
            async { determineForHandle(userHandle) }
        }
        .awaitAll()
        .flatten()
}
