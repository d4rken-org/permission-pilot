package eu.darken.myperm.permissions.ui.details

import android.annotation.SuppressLint
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.UsesPermission
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
import eu.darken.myperm.permissions.core.container.PermissionAppRef
import eu.darken.myperm.permissions.core.features.Highlighted
import eu.darken.myperm.permissions.core.features.InstallTimeGrant
import eu.darken.myperm.permissions.core.features.ManifestDoc
import eu.darken.myperm.permissions.core.features.NotNormalPerm
import eu.darken.myperm.permissions.core.features.PermissionTag
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import eu.darken.myperm.permissions.core.permissions
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
        val label: String,
        val isSystemApp: Boolean,
        val userHandle: Int,
    )

    data class RequestingAppItem(
        val pkgName: String,
        val pkg: Pkg,
        val label: String,
        val isSystemApp: Boolean,
        val status: UsesPermission.Status,
        val userHandle: Int,
    )

    data class State(
        val label: String = "",
        val permissionId: String = "",
        val permission: BasePermission? = null,
        val description: String? = null,
        val fullDescription: String? = null,
        val protectionType: ProtectionType? = null,
        val protectionFlags: List<ProtectionFlag> = emptyList(),
        val protectionFlagOverflow: Int = 0,
        val tags: List<PermissionTag> = emptyList(),
        val grantedUserCount: Int = 0,
        val totalUserCount: Int = 0,
        val grantedSystemCount: Int = 0,
        val totalSystemCount: Int = 0,
        val declaringApps: List<DeclaringAppItem> = emptyList(),
        val requestingApps: List<RequestingAppItem> = emptyList(),
        val isLoading: Boolean = true,
        val filterOptions: PermissionDetailsFilterOptions = PermissionDetailsFilterOptions(),
    )

    val state by lazy {
        combine(
            permissionsRepo.permissions.map { perms -> perms.singleOrNull { it.id == permId } },
            generalSettings.permissionDetailsFilterOptions.flow,
        ) { perm, filterOpts ->
            if (perm == null) return@combine State(label = initialLabel ?: permId.value, isLoading = true)

            val declaring = perm.declaringApps.map { ref ->
                DeclaringAppItem(
                    pkgName = ref.pkgName,
                    pkg = Pkg.Container(Pkg.Id(ref.pkgName)),
                    label = ref.label,
                    isSystemApp = ref.isSystemApp,
                    userHandle = ref.userHandleId,
                )
            }

            val filteredRefs = perm.requestingApps
                .filter { ref -> filterOpts.filters.any { filter -> filter.matches(ref) } }

            val statusRank = mapOf(
                UsesPermission.Status.GRANTED to 0,
                UsesPermission.Status.GRANTED_IN_USE to 1,
                UsesPermission.Status.DENIED to 2,
                UsesPermission.Status.UNKNOWN to 3,
            )

            val requesting = filteredRefs
                .map { ref ->
                    RequestingAppItem(
                        pkgName = ref.pkgName,
                        pkg = Pkg.Container(Pkg.Id(ref.pkgName)),
                        label = ref.label,
                        isSystemApp = ref.isSystemApp,
                        status = ref.status,
                        userHandle = ref.userHandleId,
                    )
                }
                .sortedWith(
                    compareBy<RequestingAppItem> { statusRank[it.status] ?: 99 }
                        .thenBy { it.isSystemApp }
                        .thenBy { it.label }
                )

            val allProtectionFlags = (perm as? DeclaredPermission)?.protectionFlags
                ?.sortedBy { it.ordinal } ?: emptyList()

            State(
                label = perm.id.value.split(".").lastOrNull() ?: perm.id.value,
                permissionId = perm.id.value,
                permission = perm,
                description = perm.getLabel(context),
                fullDescription = perm.getDescription(context),
                protectionType = (perm as? DeclaredPermission)?.protectionType,
                protectionFlags = allProtectionFlags.take(3),
                protectionFlagOverflow = (allProtectionFlags.size - 3).coerceAtLeast(0),
                tags = perm.tags.sortedBy { tag ->
                    when (tag) {
                        is RuntimeGrant -> 0
                        is SpecialAccess -> 1
                        is InstallTimeGrant -> 2
                        is ManifestDoc -> 3
                        is Highlighted -> 4
                        is NotNormalPerm -> 5
                    }
                },
                grantedUserCount = filteredRefs.count { !it.isSystemApp && it.status.isGranted },
                totalUserCount = filteredRefs.count { !it.isSystemApp },
                grantedSystemCount = filteredRefs.count { it.isSystemApp && it.status.isGranted },
                totalSystemCount = filteredRefs.count { it.isSystemApp },
                declaringApps = declaring,
                requestingApps = requesting,
                isLoading = false,
                filterOptions = filterOpts,
            )
        }.asStateFlow()
    }

    fun updateFilterOptions(action: (PermissionDetailsFilterOptions) -> PermissionDetailsFilterOptions) = launch {
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
