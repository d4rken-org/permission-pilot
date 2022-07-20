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
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.types.DeclaredPermission
import eu.darken.myperm.permissions.core.types.UnknownPermission
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

    val events = SingleLiveEvent<PermissionsEvents>()

    data class State(
        val listData: List<PermissionsAdapter.Item> = emptyList(),
        val isLoading: Boolean = true
    )

    val state: LiveData<State> = combine(
        permissionRepo.permissions,
        searchTerm,
        filterOptions,
        sortOptions
    ) { permissions, searchTerm, filterOptions, sortOptions ->
        val filtered = permissions
            .filter { perm -> filterOptions.keys.all { it.matches(perm) } }
            .filter {
                val prunedterm = searchTerm?.lowercase() ?: return@filter true
                if (it.id.toString().lowercase().contains(prunedterm)) return@filter true
                if (it.getLabel(context)?.lowercase()?.contains(prunedterm) == true) return@filter true

                return@filter false
            }
            .sortedWith(sortOptions.mainSort.getComparator(context))

        val items = filtered
            .sortedByDescending { it.grantingPkgs.size }
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
        State(
            listData = items,
            isLoading = false,
        )
    }
        .onStart { emit(State()) }
        .asLiveData2()


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