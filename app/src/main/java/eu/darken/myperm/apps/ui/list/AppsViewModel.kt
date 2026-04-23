package eu.darken.myperm.apps.ui.list

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.export.core.ExportSelectionStore
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.features.Highlighted
import eu.darken.myperm.permissions.core.known.APerm
import eu.darken.myperm.permissions.core.known.APermGrp
import eu.darken.myperm.permissions.core.known.toKnownGroup
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AppsViewModel @Inject constructor(
    @Suppress("unused") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val appRepo: AppRepo,
    private val generalSettings: GeneralSettings,
    private val exportSelectionStore: ExportSelectionStore,
    upgradeRepo: UpgradeRepo,
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    val isPro: StateFlow<Boolean> = upgradeRepo.upgradeInfo
        .map { it.isPro }
        .stateIn(vmScope, SharingStarted.Eagerly, upgradeRepo.upgradeInfo.value.isPro)

    val isRefreshing: StateFlow<Boolean> = appRepo.isScanning

    private val searchTerm = MutableStateFlow<String?>(null)
    private val filterOptions = generalSettings.appsFilterOptions.flow
    private val sortOptions = generalSettings.appsSortOptions.flow

    data class SelectionKey(val pkgName: Pkg.Name, val userHandleId: Int)

    private val selectedItems = MutableStateFlow<Set<SelectionKey>>(emptySet())

    private val appDataWithLabels = appRepo.appData.map { state ->
        val apps = (state as? AppRepo.AppDataState.Ready)?.apps
        if (apps == null) {
            state to emptyMap()
        } else {
            state to buildInstallerLabelCache(apps)
        }
    }

    data class PermChip(@StringRes val labelRes: Int)

    data class AppItem(
        val pkgName: Pkg.Name,
        val userHandleId: Int,
        val iconModel: Pkg,
        val label: String,
        val isSystemApp: Boolean,
        val showPkgName: Boolean,
        val installerIconPkg: Pkg.Name?,
        val installerLabel: String,
        val updatedAtFormatted: String?,
        val permChips: List<PermChip>,
    )

    sealed class State {
        data object Loading : State()
        data class Ready(
            val items: List<AppItem> = emptyList(),
            val itemCount: Int = 0,
            val filterOptions: AppsFilterOptions = AppsFilterOptions(),
            val sortOptions: AppsSortOptions = AppsSortOptions(),
            val selection: Set<SelectionKey> = emptySet(),
            val refreshError: Throwable? = null,
        ) : State()

        data class Error(val error: Throwable) : State()
    }

    private val appDataWithLabelsAndError = combine(
        appDataWithLabels,
        appRepo.scanError,
    ) { appDataPair, scanError -> Triple(appDataPair.first, appDataPair.second, scanError) }

    val state = combine(
        appDataWithLabelsAndError,
        searchTerm,
        filterOptions,
        sortOptions,
        selectedItems,
    ) { appDataTriple, searchTerm, filterOptions, sortOptions, selection ->
        val (appDataState, installerLabels, scanError) = appDataTriple
        val apps = (appDataState as? AppRepo.AppDataState.Ready)?.apps
        if (apps == null) {
            return@combine if (scanError != null) State.Error(scanError) else State.Loading
        }

        val filtered = apps
            .filter { app -> filterOptions.matches(app) }
            .filter {
                val prunedTerm = searchTerm?.lowercase() ?: return@filter true
                if (it.pkgName.value.lowercase().contains(prunedTerm)) return@filter true
                if (it.label.lowercase().contains(prunedTerm)) return@filter true
                false
            }
            .sortedWith(sortOptions.mainSort.getComparator())

        val duplicateLabels = filtered.groupingBy { it.label }.eachCount().filterValues { it > 1 }.keys

        val dateFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())

        val listItems = filtered.map { app ->
            // Install source: known store → cached PM label → manually installed/pre-installed
            val knownStoreLabel = app.installerPkgName?.let { ipn ->
                AKnownPkg.values.firstOrNull { it.id.pkgName == ipn }?.labelRes
                    ?.let { context.getString(it) }
            }
            val installerLabel = knownStoreLabel
                ?: app.allInstallerPkgNames.firstNotNullOfOrNull { installerLabels[it] }
                ?: context.getString(
                    if (app.isSystemApp) R.string.apps_list_installer_preinstalled_label
                    else R.string.apps_list_installer_sideloaded_label
                )

            // Updated date
            val updatedAtFormatted = app.updatedAt?.let { instant ->
                val formatted = dateFormatter.format(instant.atZone(ZoneId.systemDefault()))
                context.getString(R.string.apps_list_updated_label, formatted)
            }

            // Permission chips: granted Highlighted permissions, deduped by group
            val permChips = app.requestedPermissions
                .filter { it.status.isGranted }
                .mapNotNull { use ->
                    val known = aPermById[Permission.Id(use.permissionId)] ?: return@mapNotNull null
                    if (Highlighted !in known.tags) return@mapNotNull null
                    val labelRes = known.groupIds
                        .filter { it != APermGrp.Other.id }
                        .firstNotNullOfOrNull { it.toKnownGroup()?.labelRes }
                        ?: known.labelRes
                        ?: return@mapNotNull null
                    PermChip(labelRes = labelRes)
                }
                .distinctBy { it.labelRes }
                .sortedBy { chip -> CHIP_PRIORITY.indexOf(chip.labelRes).takeIf { it >= 0 } ?: Int.MAX_VALUE }

            AppItem(
                pkgName = app.pkgName,
                userHandleId = app.userHandleId,
                iconModel = Pkg.Container(Pkg.Id(app.pkgName)),
                label = app.label,
                isSystemApp = app.isSystemApp,
                showPkgName = app.label in duplicateLabels,
                installerIconPkg = if (knownStoreLabel != null) app.installerPkgName else null,
                installerLabel = installerLabel,
                updatedAtFormatted = updatedAtFormatted,
                permChips = permChips,
            )
        }
        State.Ready(
            items = listItems,
            itemCount = listItems.size,
            filterOptions = filterOptions,
            sortOptions = sortOptions,
            selection = selection,
            refreshError = scanError,
        )
    }.asStateFlow()

    fun onSearchInputChanged(term: String?) {
        log(TAG) { "onSearchInputChanged(term=$term)" }
        searchTerm.value = term
    }

    fun onAppClicked(item: AppItem) {
        val key = SelectionKey(item.pkgName, item.userHandleId)
        if (selectedItems.value.isNotEmpty()) {
            toggleSelection(key)
            return
        }
        log(TAG) { "Navigating to ${item.pkgName}" }
        navTo(
            Nav.Details.AppDetails(
                pkgName = item.pkgName.value,
                userHandle = item.userHandleId,
                appLabel = item.label,
            )
        )
    }

    fun onAppLongPressed(item: AppItem) {
        log(TAG) { "Long pressed ${item.pkgName}" }
        toggleSelection(SelectionKey(item.pkgName, item.userHandleId))
    }

    private fun toggleSelection(key: SelectionKey) {
        selectedItems.value = selectedItems.value.let {
            if (key in it) it - key else it + key
        }
    }

    fun selectAll() {
        val readyState = state.value as? State.Ready ?: return
        val allKeys = readyState.items.map { SelectionKey(it.pkgName, it.userHandleId) }.toSet()
        selectedItems.value = selectedItems.value + allKeys
        log(TAG) { "Selected all: ${selectedItems.value.size} items" }
    }

    fun clearSelection() {
        selectedItems.value = emptySet()
    }

    fun onExportSelected() {
        val selection = selectedItems.value
        if (selection.isEmpty()) return
        val ids = selection.map { "${it.pkgName.value}:${it.userHandleId}" }
        val token = exportSelectionStore.save(ids)
        clearSelection()
        navTo(Nav.Export.Config(token = token, mode = "apps"))
    }

    fun updateFilterOptions(action: (AppsFilterOptions) -> AppsFilterOptions) = launch {
        generalSettings.appsFilterOptions.update { action(it) }
    }

    fun updateSortOptions(action: (AppsSortOptions) -> AppsSortOptions) = launch {
        generalSettings.appsSortOptions.update { action(it) }
    }

    fun updateOptions(action: (AppsFilterOptions, AppsSortOptions) -> Pair<AppsFilterOptions, AppsSortOptions>) = launch {
        val currentFilter = generalSettings.appsFilterOptions.value()
        val currentSort = generalSettings.appsSortOptions.value()
        val (newFilter, newSort) = action(currentFilter, currentSort)
        generalSettings.appsFilterOptions.update { newFilter }
        generalSettings.appsSortOptions.update { newSort }
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

    private fun buildInstallerLabelCache(apps: List<AppInfo>): Map<Pkg.Name, String> {
        val allInstallerPkgs = apps.flatMapTo(mutableSetOf()) { it.allInstallerPkgNames }
        return buildMap {
            for (pkg in allInstallerPkgs) {
                if (AKnownPkg.values.any { it.id.pkgName == pkg }) continue
                try {
                    val ai = context.packageManager.getApplicationInfo(pkg.value, 0)
                    context.packageManager.getApplicationLabel(ai)?.toString()?.let { put(pkg, it) }
                } catch (_: PackageManager.NameNotFoundException) {
                    // Installer package not installed
                }
            }
        }
    }

    companion object {
        private val TAG = logTag("Apps", "VM")
        private val aPermById by lazy { APerm.values.associateBy { it.id } }

        private val CHIP_PRIORITY = listOf(
            R.string.permission_group_camera_label,
            R.string.permission_group_location_label,
            R.string.permission_group_audio_label,
            R.string.permission_group_contacts_label,
            R.string.permission_group_calendar_label,
            R.string.permission_group_calls_label,
            R.string.permission_group_messaging_label,
            R.string.permission_group_files_label,
            R.string.permission_group_sensors_label,
            R.string.permission_group_apps_label,
            R.string.permission_group_connectivity_label,
        )
    }
}
