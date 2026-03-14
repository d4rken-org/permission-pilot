package eu.darken.myperm.apps.ui.list

import android.annotation.SuppressLint
import android.os.Process
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.compose.toIcon
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.settings.core.GeneralSettings
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
    private val appRepo: AppRepo,
    private val generalSettings: GeneralSettings,
    upgradeRepo: UpgradeRepo,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    val isPro: StateFlow<Boolean> = upgradeRepo.upgradeInfo
        .map { it.isPro }
        .stateIn(vmScope, SharingStarted.Eagerly, upgradeRepo.upgradeInfo.value.isPro)

    private val searchTerm = MutableStateFlow<String?>(null)
    private val filterOptions = generalSettings.appsFilterOptions.flow
    private val sortOptions = generalSettings.appsSortOptions.flow

    data class AppItem(
        val pkgName: String,
        val userHandleId: Int,
        val iconModel: Pkg,
        val label: String,
        val isSystemApp: Boolean,
        val showPkgName: Boolean,
        val permissionCount: Int,
        val grantedCount: Int,
        val totalCount: Int,
        val declaredCount: Int,
        val tagIcons: List<ImageVector>,
        val installerPkgName: String?,
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
        appRepo.appData,
        searchTerm,
        filterOptions,
        sortOptions
    ) { appDataState, searchTerm, filterOptions, sortOptions ->
        val apps = (appDataState as? AppRepo.AppDataState.Ready)?.apps ?: return@combine State.Loading

        val filtered = apps
            .filter { app -> filterOptions.filters.all { it.matches(app) } }
            .filter {
                val prunedTerm = searchTerm?.lowercase() ?: return@filter true
                if (it.pkgName.lowercase().contains(prunedTerm)) return@filter true
                if (it.label.lowercase().contains(prunedTerm)) return@filter true
                false
            }
            .sortedWith(sortOptions.mainSort.getComparator())

        val duplicateLabels = filtered.groupingBy { it.label }.eachCount().filterValues { it > 1 }.keys

        val listItems = filtered.map { app ->
            val tagIcons = app.requestedPermissions
                .mapNotNull { Permission.Id(it.permissionId).toIcon() }
                .distinct()
                .take(5)

            AppItem(
                pkgName = app.pkgName,
                userHandleId = app.userHandleId,
                iconModel = Pkg.Container(Pkg.Id(app.pkgName)),
                label = app.label,
                isSystemApp = app.isSystemApp,
                showPkgName = app.label in duplicateLabels,
                permissionCount = app.requestedPermissions.size,
                grantedCount = app.requestedPermissions.count { it.status.isGranted },
                totalCount = app.requestedPermissions.size,
                declaredCount = app.declaredPermissionCount,
                tagIcons = tagIcons,
                installerPkgName = app.installerPkgName,
            )
        }
        State.Ready(items = listItems, itemCount = listItems.size, filterOptions = filterOptions, sortOptions = sortOptions)
    }.asStateFlow()

    fun onSearchInputChanged(term: String?) {
        log(TAG) { "onSearchInputChanged(term=$term)" }
        searchTerm.value = term
    }

    fun onAppClicked(item: AppItem) {
        log(TAG) { "Navigating to ${item.pkgName}" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = item.pkgName,
                userHandle = item.userHandleId,
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
