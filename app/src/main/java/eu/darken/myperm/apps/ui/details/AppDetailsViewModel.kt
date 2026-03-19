package eu.darken.myperm.apps.ui.details

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.widget.Toast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.PermissionUse
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.manifest.ManifestHintRepo
import eu.darken.myperm.apps.core.manifest.ManifestHintScanner
import eu.darken.myperm.apps.core.tryCreateUserHandle
import eu.darken.myperm.common.AndroidVersionCodes
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val manifestHintRepo: ManifestHintRepo,
    private val upgradeRepo: UpgradeRepo,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    var pkgName: Pkg.Name = Pkg.Name("")
        private set
    var userHandleId: Int = 0
        private set
    var initialLabel: String? = null
        private set

    private val launcherApps: LauncherApps?
        get() = context.getSystemService(LauncherApps::class.java)

    fun init(route: Nav.Details.AppDetails) {
        pkgName = Pkg.Name(route.pkgName)
        userHandleId = route.userHandle
        initialLabel = route.appLabel
    }

    private fun getUserHandle(): UserHandle? {
        if (userHandleId == Process.myUserHandle().hashCode()) return Process.myUserHandle()
        val userManager = context.getSystemService(UserManager::class.java) ?: return null
        return userManager.tryCreateUserHandle(userHandleId)
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
        val pkgName: Pkg.Name,
        val userHandleId: Int,
        val label: String,
    )

    data class SiblingItem(
        val pkgName: Pkg.Name,
        val userHandleId: Int,
        val label: String,
    )

    data class State(
        val label: String = "",
        val packageName: Pkg.Name = Pkg.Name(""),
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
        val installerPkgNames: List<Pkg.Name> = emptyList(),
        val installerAppName: String? = null,
        val permissions: List<PermItem> = emptyList(),
        val twins: List<TwinItem> = emptyList(),
        val siblings: List<SiblingItem> = emptyList(),
        val filterOptions: Set<AppDetailsFilterOptions.Filter> = AppDetailsFilterOptions().filters,
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
            if (allApps == null) return@combine State(label = initialLabel ?: pkgName.value, isLoading = true)

            val appInfo = allApps.singleOrNull { it.pkgName == pkgName && it.userHandleId == userHandleId }
                ?: return@combine State(label = initialLabel ?: pkgName.value, isLoading = true)

            val allPerms = appInfo.requestedPermissions
                .mapNotNull { cachedPerm ->
                    val basePerm = permissions.singleOrNull { it.id == Permission.Id(cachedPerm.permissionId) }
                        ?: return@mapNotNull null
                    cachedPerm to basePerm
                }

            val filteredPerms = allPerms
                .filter { (cachedPerm, basePerm) ->
                    cachedPerm.status == UsesPermission.Status.UNKNOWN ||
                            filterOpts.filters.any { filter -> filter.matches(cachedPerm, basePerm) }
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
                        isDeclaredByApp = basePerm.declaringApps.any { it.pkgName == appInfo.pkgName && it.userHandleId == appInfo.userHandleId },
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
                    val ai = context.packageManager.getApplicationInfo(installerPkg.value, 0)
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
                installerLabel = appInfo.installerPkgName?.value,
                installerSourceLabel = context.getString(R.string.apps_details_installer_label),
                canOpen = run {
                    val userHandle = getUserHandle()
                    val la = launcherApps
                    if (la != null && userHandle != null) {
                        la.getActivityList(appInfo.pkgName.value, userHandle).isNotEmpty()
                    } else {
                        context.packageManager.getLaunchIntentForPackage(appInfo.pkgName.value) != null
                    }
                },
                installerPkgNames = appInfo.allInstallerPkgNames,
                installerAppName = installerAppName,
                permissions = filteredPerms,
                twins = twins,
                siblings = siblings,
                filterOptions = filterOpts.filters,
                isLoading = false,
            )
        }.asStateFlow()
    }

    sealed class ManifestCardState {
        data class Queued(
            val progress: ManifestHintRepo.ScanProgress? = null,
        ) : ManifestCardState()

        data class Analyzing(
            val progress: ManifestHintRepo.ScanProgress? = null,
        ) : ManifestCardState()

        data class Loaded(
            val hasActionMainQuery: Boolean,
            val hasExcessiveQueries: Boolean,
            val packageQueryCount: Int,
            val intentQueryCount: Int,
            val providerQueryCount: Int,
            val totalQueryCount: Int,
            val hasWarning: Boolean,
            val canViewManifest: Boolean,
        ) : ManifestCardState()
    }

    private val _isPrioritized = MutableStateFlow(false)

    val manifestCardState: StateFlow<ManifestCardState> by lazy {
        combine(
            manifestHintRepo.hints,
            manifestHintRepo.scanProgress,
            manifestHintRepo.currentlyScanning,
            _isPrioritized,
        ) { hints, progress, currentlyScanning, isPrioritized ->
            val hint = hints[pkgName]
            if (hint != null) {
                val hasExcessive = ManifestHintScanner.hasExcessiveQueries(hint)
                return@combine ManifestCardState.Loaded(
                    hasActionMainQuery = hint.hasActionMainQuery,
                    hasExcessiveQueries = hasExcessive,
                    packageQueryCount = hint.packageQueryCount,
                    intentQueryCount = hint.intentQueryCount,
                    providerQueryCount = hint.providerQueryCount,
                    totalQueryCount = hint.totalQueryCount,
                    hasWarning = hint.hasActionMainQuery || hasExcessive,
                    canViewManifest = true,
                )
            }

            if (currentlyScanning == pkgName || isPrioritized) {
                ManifestCardState.Analyzing(progress)
            } else {
                ManifestCardState.Queued(progress)
            }
        }
            .stateIn(vmScope, SharingStarted.WhileSubscribed(5000), ManifestCardState.Queued())
    }

    fun onManifestClicked() {
        when (manifestCardState.value) {
            is ManifestCardState.Queued -> {
                log(TAG) { "Manifest queued, prioritizing $pkgName" }
                manifestHintRepo.prioritize(pkgName)
                _isPrioritized.value = true
            }
            is ManifestCardState.Analyzing -> {
                log(TAG) { "Manifest already analyzing for $pkgName" }
            }
            is ManifestCardState.Loaded -> {
                val current = manifestCardState.value as? ManifestCardState.Loaded ?: return
                if (!current.canViewManifest) return
                if (!upgradeRepo.upgradeInfo.value.isPro) {
                    log(TAG) { "Not pro, navigating to upgrade instead of manifest viewer" }
                    navTo(Nav.Main.Upgrade)
                    return
                }
                navTo(Nav.Details.AppManifest(pkgName = pkgName.value, appLabel = initialLabel))
            }
        }
    }

    fun onPermissionClicked(item: PermItem) {
        log(TAG) { "Navigating to permission ${item.permId}" }
        navTo(Nav.Details.PermissionDetails(permissionId = item.permId.value, permLabel = item.permLabel))
    }

    fun onTwinClicked(item: TwinItem) {
        log(TAG) { "Navigating to twin ${item.pkgName}" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = item.pkgName.value,
                userHandle = item.userHandleId,
                appLabel = item.label,
            )
        )
    }

    fun onSiblingClicked(item: SiblingItem) {
        log(TAG) { "Navigating to sibling ${item.pkgName}" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = item.pkgName.value,
                userHandle = item.userHandleId,
                appLabel = item.label,
            )
        )
    }

    fun updateFilterOptions(action: (AppDetailsFilterOptions) -> AppDetailsFilterOptions) = launch {
        generalSettings.appDetailsFilterOptions.update { action(it) }
    }

    fun onGoSettings() {
        log(TAG) { "onGoSettings for $pkgName (userHandleId=$userHandleId)" }
        val la = launcherApps
        val userHandle = getUserHandle()
        if (la != null && userHandle != null) {
            try {
                la.startAppDetailsActivity(ComponentName(pkgName.value, ""), userHandle, null, null)
            } catch (e: Exception) {
                log(TAG) { "startAppDetailsActivity failed: $e" }
                Toast.makeText(context, R.string.apps_details_open_settings_error, Toast.LENGTH_SHORT).show()
            }
        } else {
            log(TAG) { "LauncherApps or UserHandle unavailable, falling back to plain intent" }
            Toast.makeText(context, R.string.apps_details_open_settings_error, Toast.LENGTH_SHORT).show()
        }
    }

    fun onOpenApp() {
        log(TAG) { "onOpenApp for $pkgName (userHandleId=$userHandleId)" }
        val la = launcherApps
        val userHandle = getUserHandle()
        if (la != null && userHandle != null) {
            val activities = la.getActivityList(pkgName.value, userHandle)
            val component = activities.firstOrNull()?.componentName
            if (component != null) {
                try {
                    la.startMainActivity(component, userHandle, null, null)
                } catch (e: Exception) {
                    log(TAG) { "startMainActivity failed: $e" }
                    Toast.makeText(context, R.string.apps_details_open_app_error, Toast.LENGTH_SHORT).show()
                }
            } else {
                log(TAG) { "No launchable activity found for $pkgName" }
                Toast.makeText(context, R.string.apps_details_open_app_error, Toast.LENGTH_SHORT).show()
            }
        } else {
            log(TAG) { "LauncherApps or UserHandle unavailable" }
            Toast.makeText(context, R.string.apps_details_open_app_error, Toast.LENGTH_SHORT).show()
        }
    }

    fun onInstallerClicked(pkgName: Pkg.Name) {
        log(TAG) { "onInstallerClicked: $pkgName" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = pkgName.value,
                userHandle = userHandleId,
                appLabel = null,
            )
        )
    }

    companion object {
        private val TAG = logTag("AppDetails", "VM")
    }
}
