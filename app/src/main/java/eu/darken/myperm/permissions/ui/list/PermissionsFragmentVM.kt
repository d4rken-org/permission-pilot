package eu.darken.myperm.permissions.ui.list

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.livedata.SingleLiveEvent
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.main.ui.main.MainFragmentDirections
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.container.DeclaredPermission
import eu.darken.myperm.permissions.core.container.UnknownPermission
import eu.darken.myperm.permissions.core.known.APermGrp
import eu.darken.myperm.permissions.ui.list.groups.PermissionGroupVH
import eu.darken.myperm.permissions.ui.list.permissions.DeclaredPermissionVH
import eu.darken.myperm.permissions.ui.list.permissions.UnknownPermissionVH
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class PermissionsFragmentVM @Inject constructor(
    private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    permissionRepo: PermissionRepo,
    private val generalSettings: GeneralSettings,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val searchTerm = MutableStateFlow<String?>(null)
    private val filterOptions = generalSettings.permissionsFilterOptions.flow
    private val sortOptions = generalSettings.permissionsSortOptions.flow
    private val expandedGroups = MutableStateFlow(mapOf<PermissionGroup.Id, Boolean>())
    val events = SingleLiveEvent<PermissionsEvents>()

    data class State(
        val listData: List<PermissionsAdapter.Item> = emptyList(),
        val isLoading: Boolean = true
    )

    val state: LiveData<State> = combine(
        permissionRepo.permissions,
        expandedGroups,
        searchTerm,
        filterOptions,
        sortOptions
    ) { permissions, expGroups, searchTerm, filterOptions, sortOptions ->
        val filtered = permissions
            .filter { perm -> filterOptions.keys.all { it.matches(perm) } }
            .filter {
                val prunedterm = searchTerm?.lowercase() ?: return@filter true
                if (it.id.toString().lowercase().contains(prunedterm)) return@filter true
                if (it.getLabel(context)?.lowercase()?.contains(prunedterm) == true) return@filter true

                return@filter false
            }
            .sortedWith(sortOptions.mainSort.getComparator(context))

        val permItems = filtered
            .map { permission ->
                when (permission) {
                    is DeclaredPermission -> DeclaredPermissionVH.Item(
                        perm = permission,
                        onClickAction = {
                            log(TAG) { "Navigating to $permission" }
                            MainFragmentDirections.actionMainFragmentToPermissionDetailsFragment(
                                permissionId = permission.id,
                                permissionLabel = permission.getLabel(context),
                            ).navigate()
                        }
                    )
                    is UnknownPermission -> UnknownPermissionVH.Item(
                        perm = permission,
                        onClickAction = {

                        }
                    )
                }
            }
            .toMutableList()

        val listItems = mutableListOf<PermissionsAdapter.Item>()

        APermGrp.values.forEach { grp ->
            val permsInGrp = permItems
                .filter { grp.permissionIds.contains(it.permissionId) }
                .also { permItems -= it.toSet() }

            val isExpanded = expGroups[grp.id] == true

            PermissionGroupVH.Item(
                group = grp,
                permissions = permsInGrp,
                isExpanded = isExpanded,
                onClickAction = { expandedGroups.toggle(grp.id) },
            ).run { listItems.add(this) }

            if (isExpanded) {
                listItems.addAll(permsInGrp)
            }
        }

        APermGrp.Other.apply {
            val isExpanded = expGroups[this.id] == true

            PermissionGroupVH.Item(
                group = this,
                permissions = permItems,
                isExpanded = isExpanded,
                onClickAction = { expandedGroups.toggle(id) },
            )

            if (isExpanded) {
                listItems.addAll(permItems)
            }
        }

        State(
            listData = listItems,
            isLoading = false,
        )
    }
        .onStart { emit(State()) }
        .asLiveData2()

    private fun MutableStateFlow<Map<PermissionGroup.Id, Boolean>>.toggle(id: PermissionGroup.Id) {
        value = value.toMutableMap().apply { this[id] = !(this[id] ?: false) }
    }

    fun onSearchInputChanged(term: String?) {
        log { "onSearchInputChanged(term=$term)" }
        searchTerm.value = term
    }

    fun updateFilterOptions(action: (PermsFilterOptions) -> PermsFilterOptions) {
        generalSettings.permissionsFilterOptions.update { action(it) }
    }

    fun updateSortOptions(action: (PermsSortOptions) -> PermsSortOptions) {
        generalSettings.permissionsSortOptions.update { action(it) }
    }

    fun showFilterDialog() {
        log { "showFilterDialog" }
        events.postValue(PermissionsEvents.ShowFilterDialog(generalSettings.permissionsFilterOptions.value))
    }

    fun showSortDialog() {
        log { "showSortDialog" }
        events.postValue(PermissionsEvents.ShowSortDialog(generalSettings.permissionsSortOptions.value))
    }
}