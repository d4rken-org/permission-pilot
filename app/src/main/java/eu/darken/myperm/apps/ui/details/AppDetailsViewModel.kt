package eu.darken.myperm.apps.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.getSettingsIntent
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.AndroidVersionCodes
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.PermissionUse
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.container.DeclaredPermission
import eu.darken.myperm.permissions.core.container.ExtraPermission
import eu.darken.myperm.permissions.core.container.UnknownPermission
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import eu.darken.myperm.permissions.core.permissions
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val appRepo: AppRepo,
    private val permissionRepo: PermissionRepo,
    private val generalSettings: GeneralSettings,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    var pkgName: String = ""
        private set
    var userHandleId: Int = 0
        private set
    var initialLabel: String? = null
        private set

    fun init(route: Nav.Details.AppDetails) {
        pkgName = route.pkgName
        userHandleId = route.userHandle
        initialLabel = route.appLabel
    }

    data class PermItem(
        val permId: Permission.Id,
        val permLabel: String?,
        val status: UsesPermission.Status,
        val type: String,
        val isRuntime: Boolean,
        val isSpecialAccess: Boolean,
        val isDeclaredByApp: Boolean,
    )

    data class TwinItem(
        val pkgName: String,
        val userHandleId: Int,
        val label: String,
    )

    data class SiblingItem(
        val pkgName: String,
        val userHandleId: Int,
        val label: String,
    )

    data class State(
        val label: String = "",
        val packageName: String = "",
        val pkg: Pkg? = null,
        val isSystemApp: Boolean = false,
        val versionName: String? = null,
        val versionCode: Long? = null,
        val grantedCount: Int = 0,
        val totalPermCount: Int = 0,
        val installedAt: Instant? = null,
        val updatedAt: Instant? = null,
        val apiTargetDesc: String? = null,
        val apiMinimumDesc: String? = null,
        val apiCompileDesc: String? = null,
        val installerLabel: String? = null,
        val installerSourceLabel: String? = null,
        val canOpen: Boolean = false,
        val installerPkgNames: List<String> = emptyList(),
        val installerAppName: String? = null,
        val permissions: List<PermItem> = emptyList(),
        val twins: List<TwinItem> = emptyList(),
        val siblings: List<SiblingItem> = emptyList(),
        val filterOptions: Set<AppDetailsFilterOptions.Filter> = AppDetailsFilterOptions().keys,
        val isLoading: Boolean = true,
    )

    val state by lazy {
        combine(
            appRepo.appData.map { state ->
                (state as? AppRepo.AppDataState.Ready)?.apps
            },
            permissionRepo.permissions,
            generalSettings.appDetailsFilterOptions.flow,
        ) { allApps, permissions, filterOpts ->
            if (allApps == null) return@combine State(label = initialLabel ?: pkgName, isLoading = true)

            val appInfo = allApps.singleOrNull { it.pkgName == pkgName && it.userHandleId == userHandleId }
                ?: return@combine State(label = initialLabel ?: pkgName, isLoading = true)

            val allPerms = appInfo.requestedPermissions
                .mapNotNull { cachedPerm ->
                    val basePerm = permissions.singleOrNull { it.id == Permission.Id(cachedPerm.permissionId) }
                        ?: return@mapNotNull null
                    cachedPerm to basePerm
                }

            val filteredPerms = allPerms
                .filter { (cachedPerm, basePerm) ->
                    cachedPerm.status == UsesPermission.Status.UNKNOWN ||
                            filterOpts.keys.any { filter -> filter.matches(cachedPerm, basePerm) }
                }
                .sortedWith(
                    compareByDescending<Pair<PermissionUse, BasePermission>> { (_, basePerm) ->
                        when (basePerm) {
                            is DeclaredPermission -> 2
                            is ExtraPermission -> 1
                            is UnknownPermission -> 0
                        }
                    }.thenBy { (cachedPerm, _) -> cachedPerm.status.ordinal }
                )
                .map { (cachedPerm, basePerm) ->
                    PermItem(
                        permId = basePerm.id,
                        permLabel = basePerm.getLabel(context),
                        status = cachedPerm.status,
                        type = when (basePerm) {
                            is DeclaredPermission -> "declared"
                            is ExtraPermission -> "extra"
                            is UnknownPermission -> "unknown"
                        },
                        isRuntime = basePerm.tags.contains(RuntimeGrant),
                        isSpecialAccess = basePerm.tags.contains(SpecialAccess),
                        isDeclaredByApp = appInfo.declaredPermissionCount > 0
                                && basePerm.declaringApps.any { it.pkgName == appInfo.pkgName && it.userHandleId == appInfo.userHandleId },
                    )
                }

            // Compute twins: same pkgName, different userHandleId
            val twins = allApps
                .filter { it.pkgName == appInfo.pkgName && it.userHandleId != appInfo.userHandleId }
                .map { TwinItem(pkgName = it.pkgName, userHandleId = it.userHandleId, label = it.label) }

            // Compute siblings: same non-null sharedUserId, same userHandleId, different pkgName
            val siblings = if (appInfo.sharedUserId != null) {
                allApps
                    .filter {
                        it.sharedUserId == appInfo.sharedUserId
                                && it.userHandleId == appInfo.userHandleId
                                && it.pkgName != appInfo.pkgName
                    }
                    .map { SiblingItem(pkgName = it.pkgName, userHandleId = it.userHandleId, label = it.label) }
            } else {
                emptyList()
            }

            fun formatApiLevel(level: Int?): String? {
                if (level == null) return null
                val versionCode = AndroidVersionCodes.values().singleOrNull { it.apiLevel == level }
                return versionCode?.longFormat ?: "? (?) [$level]"
            }

            // Resolve installer label from PackageManager
            val installerAppName = appInfo.allInstallerPkgNames.firstNotNullOfOrNull { installerPkg ->
                try {
                    val ai = context.packageManager.getApplicationInfo(installerPkg, 0)
                    context.packageManager.getApplicationLabel(ai)?.toString()
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }

            State(
                label = appInfo.label,
                packageName = appInfo.pkgName,
                pkg = Pkg.Container(Pkg.Id(appInfo.pkgName)),
                isSystemApp = appInfo.isSystemApp,
                versionName = appInfo.versionName,
                versionCode = appInfo.versionCode,
                grantedCount = allPerms.count { it.first.status.isGranted },
                totalPermCount = allPerms.size,
                installedAt = appInfo.installedAt,
                updatedAt = appInfo.updatedAt,
                apiTargetDesc = appInfo.apiTargetLevel?.let { context.getString(R.string.api_target_level_x, formatApiLevel(it)) },
                apiMinimumDesc = appInfo.apiMinimumLevel?.let { context.getString(R.string.api_minimum_level_x, formatApiLevel(it)) },
                apiCompileDesc = appInfo.apiCompileLevel?.let { context.getString(R.string.api_build_level_x, formatApiLevel(it)) },
                installerLabel = appInfo.installerPkgName,
                installerSourceLabel = context.getString(R.string.apps_details_installer_label),
                canOpen = context.packageManager.getLaunchIntentForPackage(appInfo.pkgName) != null,
                installerPkgNames = appInfo.allInstallerPkgNames,
                installerAppName = installerAppName,
                permissions = filteredPerms,
                twins = twins,
                siblings = siblings,
                filterOptions = filterOpts.keys,
                isLoading = false,
            )
        }.asStateFlow()
    }

    fun onPermissionClicked(item: PermItem) {
        log(TAG) { "Navigating to permission ${item.permId}" }
        navTo(Nav.Details.PermissionDetails(permissionId = item.permId.value, permLabel = item.permLabel))
    }

    fun onTwinClicked(item: TwinItem) {
        log(TAG) { "Navigating to twin ${item.pkgName}" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = item.pkgName,
                userHandle = item.userHandleId,
                appLabel = item.label,
            )
        )
    }

    fun onSiblingClicked(item: SiblingItem) {
        log(TAG) { "Navigating to sibling ${item.pkgName}" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = item.pkgName,
                userHandle = item.userHandleId,
                appLabel = item.label,
            )
        )
    }

    fun updateFilterOptions(action: (AppDetailsFilterOptions) -> AppDetailsFilterOptions) = launch {
        generalSettings.appDetailsFilterOptions.update { action(it) }
    }

    fun onGoSettings() {
        log(TAG) { "onGoSettings for $pkgName" }
        val pkg = Pkg.Container(Pkg.Id(pkgName))
        val intent = pkg.getSettingsIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun onOpenApp() {
        log(TAG) { "onOpenApp for $pkgName" }
        val intent = context.packageManager.getLaunchIntentForPackage(pkgName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun onInstallerClicked(pkgName: String) {
        log(TAG) { "onInstallerClicked: $pkgName" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = pkgName,
                userHandle = userHandleId,
                appLabel = null,
            )
        )
    }

    companion object {
        private val TAG = logTag("AppDetails", "VM")
    }
}
