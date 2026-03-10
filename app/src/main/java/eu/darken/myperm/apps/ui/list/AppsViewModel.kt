package eu.darken.myperm.apps.ui.list

import android.annotation.SuppressLint
import android.content.Context
import android.os.Process
import android.os.UserHandle
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.Installed
import eu.darken.myperm.apps.core.features.InstallerInfo
import eu.darken.myperm.apps.core.features.ReadableApk
import eu.darken.myperm.apps.core.features.isGranted
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import androidx.compose.ui.graphics.vector.ImageVector
import eu.darken.myperm.common.compose.toIcon
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.common.upgrade.UpgradeRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AppsViewModel @Inject constructor(
    @Suppress("unused") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val appRepo: AppRepo,
    private val generalSettings: GeneralSettings,
    private val permissionRepo: PermissionRepo,
    upgradeRepo: UpgradeRepo,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    val isPro: StateFlow<Boolean> = upgradeRepo.upgradeInfo
        .map { it.isPro }
        .stateIn(vmScope, SharingStarted.Eagerly, upgradeRepo.upgradeInfo.value.isPro)

    private val searchTerm = MutableStateFlow<String?>(null)
    private val filterOptions = generalSettings.appsFilterOptions.flow
    private val sortOptions = generalSettings.appsSortOptions.flow

    data class AppItem(
        val id: Pkg.Id,
        val pkg: Pkg,
        val label: String?,
        val packageName: String,
        val isSystemApp: Boolean,
        val permissionCount: Int,
        val grantedCount: Int,
        val totalCount: Int,
        val declaredCount: Int,
        val tagIcons: List<ImageVector>,
        val installerInfo: InstallerInfo?,
        val userHandle: UserHandle,
    )

    sealed class State {
        data object Loading : State()
        data class Ready(
            val items: List<AppItem> = emptyList(),
            val itemCount: Int = 0,
            val filterOptions: AppsFilterOptions = AppsFilterOptions(),
            val sortOptions: AppsSortOptions = AppsSortOptions(),
        ) : State()
    }

    val state = combine(
        appRepo.state,
        permissionRepo.state,
        searchTerm,
        filterOptions,
        sortOptions
    ) { appRepoState, permissionRepoState, searchTerm, filterOptions, sortOptions ->
        val apps = (appRepoState as? AppRepo.State.Ready)?.pkgs
        val permissions = (permissionRepoState as? PermissionRepo.State.Ready)?.permissions
        if (apps == null || permissions == null) return@combine State.Loading
        if (appRepoState.id != permissionRepoState.basedOnAppState) return@combine State.Loading

        val filtered = apps
            .filter { app -> filterOptions.keys.all { it.matches(app) } }
            .filter {
                val prunedTerm = searchTerm?.lowercase() ?: return@filter true
                if (it.id.toString().lowercase().contains(prunedTerm)) return@filter true
                if (it.getLabel(context)?.lowercase()?.contains(prunedTerm) == true) return@filter true
                false
            }
            .sortedWith(sortOptions.mainSort.getComparator(context))

        val listItems = filtered.map { app ->
            val readableApk = app as? ReadableApk
            val requestedPerms = readableApk?.requestedPermissions ?: emptyList()
            val tagIcons = requestedPerms
                .mapNotNull { it.id.toIcon() }
                .distinct()
                .take(5)

            AppItem(
                id = app.id,
                pkg = app,
                label = app.getLabel(context),
                packageName = app.packageName,
                isSystemApp = app.isSystemApp,
                permissionCount = requestedPerms.size,
                grantedCount = requestedPerms.count { it.isGranted },
                totalCount = requestedPerms.size,
                declaredCount = readableApk?.declaredPermissions?.size ?: 0,
                tagIcons = tagIcons,
                installerInfo = (app as? Installed)?.installerInfo,
                userHandle = app.userHandle,
            )
        }
        State.Ready(items = listItems, itemCount = listItems.size, filterOptions = filterOptions, sortOptions = sortOptions)
    }.asStateFlow()

    fun onSearchInputChanged(term: String?) {
        log(TAG) { "onSearchInputChanged(term=$term)" }
        searchTerm.value = term
    }

    fun onAppClicked(item: AppItem) {
        log(TAG) { "Navigating to ${item.id}" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = item.id.pkgName,
                userHandle = Process.myUserHandle().hashCode(), // UserHandle int representation
                appLabel = item.label,
            )
        )
    }

    fun updateFilterOptions(action: (AppsFilterOptions) -> AppsFilterOptions) = launch {
        generalSettings.appsFilterOptions.update { action(it) }
    }

    fun updateSortOptions(action: (AppsSortOptions) -> AppsSortOptions) = launch {
        generalSettings.appsSortOptions.update { action(it) }
    }

    fun onRefresh() {
        log(TAG) { "onRefresh" }
        appRepo.refresh()
    }

    fun goToSettings() {
        navTo(Nav.Settings.Index)
    }

    fun onUpgrade() {
        navTo(Nav.Main.Upgrade)
    }

    companion object {
        private val TAG = logTag("Apps", "VM")
    }
}
