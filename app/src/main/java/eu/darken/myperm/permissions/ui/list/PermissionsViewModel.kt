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
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.container.DeclaredPermission
import eu.darken.myperm.permissions.core.container.ExtraPermission
import eu.darken.myperm.permissions.core.container.UnknownPermission
import eu.darken.myperm.permissions.core.getGroupIds
import eu.darken.myperm.permissions.core.known.APermGrp
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
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
) : ViewModel4(dispatcherProvider = dispatcherProvider) {

    private val searchTerm = MutableStateFlow<String?>(null)
    private val filterOptions = generalSettings.permissionsFilterOptions.flow
    private val sortOptions = generalSettings.permissionsSortOptions.flow
    private val expandedGroups = MutableStateFlow(mapOf<PermissionGroup.Id, Boolean>())

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
        ) : State()
    }

    val state: Flow<State> = combine(
        permissionRepo.state,
        expandedGroups,
        searchTerm,
        filterOptions,
        sortOptions
    ) { permissionRepoState, expGroups, searchTerm, filterOptions, sortOptions ->
        val permissions = (permissionRepoState as? PermissionRepo.State.Ready)?.permissions
            ?: return@combine State.Loading

        val filtered = permissions
            .filter { perm -> filterOptions.keys.all { it.matches(perm) } }
            .filter {
                val prunedTerm = searchTerm?.lowercase() ?: return@filter true
                if (it.id.toString().lowercase().contains(prunedTerm)) return@filter true
                if (it.getLabel(context)?.lowercase()?.contains(prunedTerm) == true) return@filter true
                false
            }
            .sortedWith(sortOptions.mainSort.getComparator(context))

        val permItems = filtered.map { perm ->
            PermItem(
                id = perm.id,
                label = perm.getLabel(context),
                type = when (perm) {
                    is DeclaredPermission -> "declared"
                    is ExtraPermission -> "extra"
                    is UnknownPermission -> "unknown"
                },
                requestingCount = perm.requestingPkgs.size,
                grantedCount = perm.grantingPkgs.size,
                permission = perm,
            )
        }.toMutableList()

        var permissionCount = 0
        var groupCount = 0
        val listItems = mutableListOf<ListItem>()

        APermGrp.values
            .filter { it != APermGrp.Other }
            .sortedBy { context.getString(it.labelRes) }
            .forEach { grp ->
                val permsInGrp = permItems
                    .filter { perm -> filtered.any { it.id == perm.id && it.getGroupIds().contains(grp.id) } }

                permItems -= permsInGrp.toSet()
                val isExpanded = expGroups[grp.id] == true

                if (permsInGrp.isNotEmpty()) {
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

        // Other group
        if (permItems.isNotEmpty()) {
            val isExpanded = expGroups[APermGrp.Other.id] == true
            listItems.add(
                ListItem.Group(
                    GroupItem(group = APermGrp.Other, permCount = permItems.size, isExpanded = isExpanded)
                )
            )
            permissionCount += permItems.size
            if (isExpanded) listItems.addAll(permItems.map { ListItem.Perm(it) })
        }

        State.Ready(
            listData = listItems,
            countGroups = groupCount,
            countPermissions = permissionCount,
            filterOptions = filterOptions,
            sortOptions = sortOptions,
        )
    }.onStart { emit(State.Loading) }

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
        log(TAG) { "Navigating to ${item.id}" }
        navTo(Nav.Details.PermissionDetails(permissionId = item.id.value, permLabel = item.label))
    }

    fun onRefresh() {
        appRepo.refresh()
    }

    fun updateFilterOptions(action: (PermsFilterOptions) -> PermsFilterOptions) {
        generalSettings.permissionsFilterOptions.update { action(it) }
    }

    fun updateSortOptions(action: (PermsSortOptions) -> PermsSortOptions) {
        generalSettings.permissionsSortOptions.update { action(it) }
    }

    fun goToSettings() {
        navTo(Nav.Settings.Index)
    }

    companion object {
        private val TAG = logTag("Permissions", "VM")
    }
}
