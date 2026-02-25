package eu.darken.myperm.permissions.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.os.Process
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.Installed
import eu.darken.myperm.apps.core.features.getPermission
import eu.darken.myperm.apps.core.features.getPermissionUses
import eu.darken.myperm.apps.core.features.isGranted
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.ProtectionFlag
import eu.darken.myperm.permissions.core.ProtectionType
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.container.DeclaredPermission
import eu.darken.myperm.permissions.core.permissions
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class PermissionDetailsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val permissionsRepo: PermissionRepo,
    private val generalSettings: GeneralSettings,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    var permId: Permission.Id = Permission.Id("")
        private set
    var initialLabel: String? = null
        private set

    fun init(route: Nav.Details.PermissionDetails) {
        permId = Permission.Id(route.permissionId)
        initialLabel = route.permLabel
    }

    data class DeclaringAppItem(
        val pkgName: String,
        val pkg: Pkg,
        val label: String?,
        val isSystemApp: Boolean,
        val userHandle: Int,
    )

    data class RequestingAppItem(
        val pkgName: String,
        val pkg: Pkg,
        val label: String?,
        val isSystemApp: Boolean,
        val isGranted: Boolean,
        val statusLabel: String,
        val userHandle: Int,
    )

    data class State(
        val label: String = "",
        val permissionId: String = "",
        val permission: BasePermission? = null,
        val description: String? = null,
        val fullDescription: String? = null,
        val protectionLevel: String? = null,
        val grantedUserCount: Int = 0,
        val totalUserCount: Int = 0,
        val grantedSystemCount: Int = 0,
        val totalSystemCount: Int = 0,
        val declaringApps: List<DeclaringAppItem> = emptyList(),
        val requestingApps: List<RequestingAppItem> = emptyList(),
        val isLoading: Boolean = true,
        val filterOptions: PermissionDetailsFilterOptions = PermissionDetailsFilterOptions(),
    )

    val state: Flow<State> by lazy {
        combine(
            permissionsRepo.permissions.map { perms -> perms.singleOrNull { it.id == permId } },
            generalSettings.permissionDetailsFilterOptions.flow,
        ) { perm, filterOpts ->
            if (perm == null) return@combine State(label = initialLabel ?: permId.value, isLoading = true)

            val declaring = perm.declaringPkgs.map { app ->
                DeclaringAppItem(
                    pkgName = app.packageName,
                    pkg = app,
                    label = app.getLabel(context),
                    isSystemApp = app.isSystemApp,
                    userHandle = Process.myUserHandle().hashCode(),
                )
            }

            val requestingPkgs = perm.requestingPkgs
                .filter { app ->
                    app !is Installed || filterOpts.keys.any { filter -> filter.matches(app) }
                }

            val requesting = requestingPkgs
                .map { app ->
                    val status = app.getPermissionUses(perm.id).status
                    RequestingAppItem(
                        pkgName = app.packageName,
                        pkg = app,
                        label = app.getLabel(context),
                        isSystemApp = app.isSystemApp,
                        isGranted = app.getPermission(perm.id)?.isGranted == true,
                        statusLabel = status.name,
                        userHandle = Process.myUserHandle().hashCode(),
                    )
                }
                .sortedWith(compareBy<RequestingAppItem> { it.statusLabel }.thenBy { it.isSystemApp })

            val protectionLevelStr = (perm as? DeclaredPermission)?.let { declared ->
                val typeLabel = context.getString(declared.protectionType.labelRes)
                val flags = declared.protectionFlags
                if (flags.isNotEmpty()) {
                    "$typeLabel (${flags.joinToString(", ") { it.name }})"
                } else {
                    typeLabel
                }
            }

            State(
                label = perm.id.value.split(".").lastOrNull() ?: perm.id.value,
                permissionId = perm.id.value,
                permission = perm,
                description = perm.getLabel(context),
                fullDescription = perm.getDescription(context),
                protectionLevel = protectionLevelStr,
                grantedUserCount = requestingPkgs.count { !it.isSystemApp && it.getPermission(perm.id)?.isGranted == true },
                totalUserCount = requestingPkgs.count { !it.isSystemApp },
                grantedSystemCount = requestingPkgs.count { it.isSystemApp && it.getPermission(perm.id)?.isGranted == true },
                totalSystemCount = requestingPkgs.count { it.isSystemApp },
                declaringApps = declaring,
                requestingApps = requesting,
                isLoading = false,
                filterOptions = filterOpts,
            )
        }.onStart { emit(State(label = initialLabel ?: permId.value)) }
    }

    fun updateFilterOptions(action: (PermissionDetailsFilterOptions) -> PermissionDetailsFilterOptions) {
        generalSettings.permissionDetailsFilterOptions.update { action(it) }
    }

    fun onAppClicked(pkgName: String, userHandle: Int) {
        log(TAG) { "Navigating to app $pkgName" }
        navTo(Nav.Details.AppDetails(pkgName = pkgName, userHandle = userHandle))
    }

    companion object {
        private val TAG = logTag("PermissionDetails", "VM")
    }
}
