package eu.darken.myperm.permissions.ui.list

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.export.core.ExportSelectionStore
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.container.DeclaredPermission
import eu.darken.myperm.permissions.core.container.ExtraPermission
import eu.darken.myperm.permissions.core.container.UnknownPermission
import eu.darken.myperm.permissions.core.known.APermGrp
import eu.darken.myperm.common.upgrade.UpgradeRepo
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
class PermissionsViewModel @Inject constructor(
    @Suppress("unused") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    permissionRepo: PermissionRepo,
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
    private val filterOptions = generalSettings.permissionsFilterOptions.flow
    private val sortOptions = generalSettings.permissionsSortOptions.flow
    private val expandedGroups = MutableStateFlow(mapOf<PermissionGroup.Id, Boolean>())
    private val selectedPermissions = MutableStateFlow<Set<Permission.Id>>(emptySet())

    private val permDataWithLabels = permissionRepo.state.map { state ->
        val perms = (state as? PermissionRepo.State.Ready)?.permissions
        if (perms == null) {
            state to emptyMap()
        } else {
            state to perms.associate { it.id to it.getLabel(context) }
        }
    }

    data class PermItem(
        val id: Permission.Id,
        val label: String?,
        val type: String, // "declared", "extra", "unknown"
        val requestingCount: Int,
        val grantedCount: Int,
        val permission: BasePermission,
    )

    data class GroupItem(
        val group: APermGrp,
        val permCount: Int,
        val isExpanded: Boolean,
    )

    sealed class ListItem {
        data class Group(val item: GroupItem) : ListItem()
        data class Perm(val item: PermItem) : ListItem()
    }

    sealed class State {
        data object Loading : State()
        data class Ready(
            val listData: List<ListItem> = emptyList(),
            val countPermissions: Int = 0,
            val countGroups: Int = 0,
            val filterOptions: PermsFilterOptions = PermsFilterOptions(),
            val sortOptions: PermsSortOptions = PermsSortOptions(),
            val selection: Set<Permission.Id> = emptySet(),
            val groupPerms: Map<PermissionGroup.Id, List<Permission.Id>> = emptyMap(),
        ) : State()
    }

    val state = combine(
        permDataWithLabels,
        expandedGroups,
        searchTerm,
        filterOptions,
        combine(sortOptions, selectedPermissions) { s, sel -> s to sel },
    ) { permDataPair, expGroups, searchTerm, filterOptions, sortAndSel ->
        val (sortOptions, selection) = sortAndSel
        val (permissionRepoState, permLabelMap) = permDataPair
        val permissions = (permissionRepoState as? PermissionRepo.State.Ready)?.permissions
            ?: return@combine State.Loading

        val filtered = permissions
            .filter { perm -> filterOptions.matches(perm) }
            .filter {
                val prunedTerm = searchTerm?.lowercase() ?: return@filter true
                if (it.id.toString().lowercase().contains(prunedTerm)) return@filter true
                if (permLabelMap[it.id]?.lowercase()?.contains(prunedTerm) == true) return@filter true
                false
            }
            .sortedWith(sortOptions.mainSort.getComparator(context))

        val permItems = filtered.map { perm ->
            PermItem(
                id = perm.id,
                label = permLabelMap[perm.id],
                type = when (perm) {
                    is DeclaredPermission -> "declared"
                    is ExtraPermission -> "extra"
                    is UnknownPermission -> "unknown"
                },
                requestingCount = perm.requestingApps.size,
                grantedCount = perm.grantingApps.size,
                permission = perm,
            )
        }

        var permissionCount = 0
        var groupCount = 0
        val listItems = mutableListOf<ListItem>()
        val groupPermsMap = mutableMapOf<PermissionGroup.Id, List<Permission.Id>>()

        // Pre-group permissions by their group IDs (single pass)
        val permsByGroup = mutableMapOf<PermissionGroup.Id, MutableList<PermItem>>()
        for (item in permItems) {
            for (grpId in item.permission.groupIds) {
                permsByGroup.getOrPut(grpId) { mutableListOf() }.add(item)
            }
        }

        val assigned = mutableSetOf<Permission.Id>()
        APermGrp.values
            .filter { it != APermGrp.Other }
            .sortedBy { context.getString(it.labelRes) }
            .forEach { grp ->
                val permsInGrp = permsByGroup[grp.id]
                    ?.filter { it.id !in assigned }
                    ?: emptyList()

                assigned.addAll(permsInGrp.map { it.id })
                val isExpanded = expGroups[grp.id] == true

                if (permsInGrp.isNotEmpty()) {
                    groupPermsMap[grp.id] = permsInGrp.map { it.id }
                    listItems.add(
                        ListItem.Group(
                            GroupItem(group = grp, permCount = permsInGrp.size, isExpanded = isExpanded)
                        )
                    )
                    groupCount++
                }

                permissionCount += permsInGrp.size
                if (isExpanded) listItems.addAll(permsInGrp.map { ListItem.Perm(it) })
            }

        // Other group: permissions not assigned to any named group
        val ungrouped = permItems.filter { it.id !in assigned }
        if (ungrouped.isNotEmpty()) {
            val isExpanded = expGroups[APermGrp.Other.id] == true
            groupPermsMap[APermGrp.Other.id] = ungrouped.map { it.id }
            listItems.add(
                ListItem.Group(
                    GroupItem(group = APermGrp.Other, permCount = ungrouped.size, isExpanded = isExpanded)
                )
            )
            groupCount++
            permissionCount += ungrouped.size
            if (isExpanded) listItems.addAll(ungrouped.map { ListItem.Perm(it) })
        }

        State.Ready(
            listData = listItems,
            countGroups = groupCount,
            countPermissions = permissionCount,
            filterOptions = filterOptions,
            sortOptions = sortOptions,
            selection = selection,
            groupPerms = groupPermsMap,
        )
    }.asStateFlow()

    fun toggleGroup(id: PermissionGroup.Id) {
        expandedGroups.value = expandedGroups.value.toMutableMap().apply {
            this[id] = !(this[id] ?: false)
        }
    }

    fun expandAll() {
        expandedGroups.value = APermGrp.values.map { it.id }.associateWith { true }
    }

    fun collapseAll() {
        expandedGroups.value = emptyMap()
    }

    fun onSearchInputChanged(term: String?) {
        searchTerm.value = term
    }

    fun onPermissionClicked(item: PermItem) {
        if (selectedPermissions.value.isNotEmpty()) {
            togglePermissionSelection(item.id)
            return
        }
        log(TAG) { "Navigating to ${item.id}" }
        navTo(Nav.Details.PermissionDetails(permissionId = item.id.value, permLabel = item.label))
    }

    fun onPermissionLongPressed(item: PermItem) {
        log(TAG) { "Long pressed ${item.id}" }
        togglePermissionSelection(item.id)
    }

    fun onGroupLongPressed(groupItem: GroupItem) {
        log(TAG) { "Group long pressed: ${groupItem.group.id}" }
        val readyState = state.value as? State.Ready ?: return
        val groupPermIds = readyState.groupPerms[groupItem.group.id] ?: return
        val current = selectedPermissions.value
        val allSelected = groupPermIds.all { it in current }
        selectedPermissions.value = if (allSelected) {
            current - groupPermIds.toSet()
        } else {
            current + groupPermIds.toSet()
        }
    }

    private fun togglePermissionSelection(id: Permission.Id) {
        selectedPermissions.value = selectedPermissions.value.let {
            if (id in it) it - id else it + id
        }
    }

    fun selectAllPermissions() {
        val readyState = state.value as? State.Ready ?: return
        val allIds = readyState.listData
            .filterIsInstance<ListItem.Perm>()
            .map { it.item.id }
            .toSet()
        // Also include permissions from collapsed groups
        val allGroupPermIds = readyState.groupPerms.values.flatten().toSet()
        selectedPermissions.value = selectedPermissions.value + allIds + allGroupPermIds
        log(TAG) { "Selected all: ${selectedPermissions.value.size} permissions" }
    }

    fun clearPermissionSelection() {
        selectedPermissions.value = emptySet()
    }

    fun onExportSelectedPermissions() {
        val selection = selectedPermissions.value
        if (selection.isEmpty()) return
        val ids = selection.map { it.value }
        val token = exportSelectionStore.save(ids)
        clearPermissionSelection()
        navTo(Nav.Export.Config(token = token, mode = "permissions"))
    }

    fun onRefresh() {
        log(TAG) { "onRefresh()" }
        appRepo.refresh()
    }

    fun updateFilterOptions(action: (PermsFilterOptions) -> PermsFilterOptions) = launch {
        generalSettings.permissionsFilterOptions.update { action(it) }
    }

    fun updateSortOptions(action: (PermsSortOptions) -> PermsSortOptions) = launch {
        generalSettings.permissionsSortOptions.update { action(it) }
    }

    fun updateOptions(action: (PermsFilterOptions, PermsSortOptions) -> Pair<PermsFilterOptions, PermsSortOptions>) = launch {
        val currentFilter = generalSettings.permissionsFilterOptions.value()
        val currentSort = generalSettings.permissionsSortOptions.value()
        val (newFilter, newSort) = action(currentFilter, currentSort)
        generalSettings.permissionsFilterOptions.update { newFilter }
        generalSettings.permissionsSortOptions.update { newSort }
    }

    fun goToSettings() {
        navTo(Nav.Settings.Index)
    }

    fun onUpgrade() {
        navTo(Nav.Main.Upgrade)
    }

    companion object {
        private val TAG = logTag("Permissions", "VM")
    }
}
