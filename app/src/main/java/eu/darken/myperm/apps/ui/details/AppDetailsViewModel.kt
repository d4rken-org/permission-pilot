package eu.darken.myperm.apps.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Process
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.apps
import eu.darken.myperm.apps.core.getSettingsIntent
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.Installed
import eu.darken.myperm.apps.core.features.ReadableApk
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.features.isGranted
import eu.darken.myperm.common.AndroidVersionCodes
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.Instant
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    appRepo: AppRepo,
    private val permissionRepo: PermissionRepo,
    private val generalSettings: GeneralSettings,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    // Set from the screen host when the route is known
    var pkgId: Pkg.Id = Pkg.Id("", Process.myUserHandle())
        private set
    var initialLabel: String? = null
        private set

    fun init(route: Nav.Details.AppDetails) {
        pkgId = Pkg.Id(route.pkgName, Process.myUserHandle()) // TODO: reconstruct proper UserHandle from route.userHandle
        initialLabel = route.appLabel
    }

    data class PermItem(
        val permId: Permission.Id,
        val permLabel: String?,
        val usesPermission: UsesPermission,
        val status: UsesPermission.Status,
        val type: String,
        val isRuntime: Boolean,
        val isSpecialAccess: Boolean,
        val isDeclaredByApp: Boolean,
    )

    data class TwinItem(
        val pkgId: Pkg.Id,
        val label: String?,
    )

    data class SiblingItem(
        val pkgId: Pkg.Id,
        val label: String?,
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

    val state: Flow<State> by lazy {
        combine(
            appRepo.apps.map { apps -> apps.singleOrNull { it.id == pkgId } },
            generalSettings.appDetailsFilterOptions.flow,
        ) { app, filterOpts ->
            if (app == null) return@combine State(label = initialLabel ?: pkgId.pkgName, isLoading = true)

            val permissions = permissionRepo.permissions.first()
            val readableApk = app as? ReadableApk
            val installed = app as? Installed

            val allPerms = readableApk?.requestedPermissions
                ?.mapNotNull { usesPerm ->
                    val basePerm = permissions.singleOrNull { it.id == usesPerm.id } ?: return@mapNotNull null
                    usesPerm to basePerm
                }
                ?: emptyList()

            val filteredPerms = allPerms
                .filter { (usesPerm, basePerm) ->
                    usesPerm.status == UsesPermission.Status.UNKNOWN ||
                            filterOpts.keys.any { filter -> filter.matches(usesPerm, basePerm) }
                }
                .sortedWith(
                    compareByDescending<Pair<UsesPermission, BasePermission>> { (_, basePerm) ->
                        when (basePerm) {
                            is DeclaredPermission -> 2
                            is ExtraPermission -> 1
                            is UnknownPermission -> 0
                        }
                    }.thenBy { (usesPerm, _) -> usesPerm.status.ordinal }
                )
                .map { (usesPerm, basePerm) ->
                    val isDeclaredByApp = readableApk?.declaredPermissions
                        ?.any { it.name == basePerm.id.value } == true
                    PermItem(
                        permId = basePerm.id,
                        permLabel = basePerm.getLabel(context),
                        usesPermission = usesPerm,
                        status = usesPerm.status,
                        type = when (basePerm) {
                            is DeclaredPermission -> "declared"
                            is ExtraPermission -> "extra"
                            is UnknownPermission -> "unknown"
                        },
                        isRuntime = basePerm.tags.contains(RuntimeGrant),
                        isSpecialAccess = basePerm.tags.contains(SpecialAccess),
                        isDeclaredByApp = isDeclaredByApp,
                    )
                }

            val twins = app.twins.map { twin ->
                TwinItem(pkgId = twin.id, label = twin.getLabel(context))
            }

            val siblings = app.siblings.map { sibling ->
                SiblingItem(pkgId = sibling.id, label = sibling.getLabel(context))
            }

            fun formatApiLevel(level: Int?): String? {
                if (level == null) return null
                val versionCode = AndroidVersionCodes.values().singleOrNull { it.apiLevel == level }
                return versionCode?.longFormat ?: "? (?) [$level]"
            }

            State(
                label = app.getLabel(context) ?: app.packageName,
                packageName = app.packageName,
                pkg = app,
                isSystemApp = app.isSystemApp,
                versionName = readableApk?.versionName,
                versionCode = readableApk?.versionCode,
                grantedCount = allPerms.count { it.first.isGranted },
                totalPermCount = allPerms.size,
                installedAt = installed?.installedAt,
                updatedAt = installed?.updatedAt,
                apiTargetDesc = readableApk?.apiTargetLevel?.let { context.getString(R.string.api_target_level_x, formatApiLevel(it)) },
                apiMinimumDesc = readableApk?.apiMinimumLevel?.let { context.getString(R.string.api_minimum_level_x, formatApiLevel(it)) },
                apiCompileDesc = readableApk?.apiCompileLevel?.let { context.getString(R.string.api_build_level_x, formatApiLevel(it)) },
                installerLabel = installed?.installerInfo?.getLabel(context),
                installerSourceLabel = context.getString(R.string.apps_details_installer_label),
                canOpen = context.packageManager.getLaunchIntentForPackage(app.packageName) != null,
                installerPkgNames = installed?.installerInfo?.allInstallers?.map { it.packageName } ?: emptyList(),
                installerAppName = installed?.installerInfo?.allInstallers?.mapNotNull { it.getLabel(context) }?.firstOrNull(),
                permissions = filteredPerms,
                twins = twins,
                siblings = siblings,
                filterOptions = filterOpts.keys,
                isLoading = false,
            )
        }
            .onStart { emit(State(label = initialLabel ?: pkgId.pkgName)) }
    }

    fun onPermissionClicked(item: PermItem) {
        log(TAG) { "Navigating to permission ${item.permId}" }
        navTo(Nav.Details.PermissionDetails(permissionId = item.permId.value, permLabel = item.permLabel))
    }

    fun onTwinClicked(item: TwinItem) {
        log(TAG) { "Navigating to twin ${item.pkgId}" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = item.pkgId.pkgName,
                userHandle = Process.myUserHandle().hashCode(),
                appLabel = item.label,
            )
        )
    }

    fun onSiblingClicked(item: SiblingItem) {
        log(TAG) { "Navigating to sibling ${item.pkgId}" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = item.pkgId.pkgName,
                userHandle = Process.myUserHandle().hashCode(),
                appLabel = item.label,
            )
        )
    }

    fun updateFilterOptions(action: (AppDetailsFilterOptions) -> AppDetailsFilterOptions) {
        generalSettings.appDetailsFilterOptions.update { action(it) }
    }

    fun onGoSettings() {
        log(TAG) { "onGoSettings for $pkgId" }
        val pkg = Pkg.Container(pkgId)
        val intent = pkg.getSettingsIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun onOpenApp() {
        log(TAG) { "onOpenApp for $pkgId" }
        val intent = context.packageManager.getLaunchIntentForPackage(pkgId.pkgName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun onInstallerClicked(pkgName: String) {
        log(TAG) { "onInstallerClicked: $pkgName" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = pkgName,
                userHandle = Process.myUserHandle().hashCode(),
                appLabel = null,
            )
        )
    }

    companion object {
        private val TAG = logTag("AppDetails", "VM")
    }
}
