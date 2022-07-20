package eu.darken.myperm.apps.ui.list

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.container.BasicPkgContainer
import eu.darken.myperm.apps.ui.list.apps.NormalAppVH
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.livedata.SingleLiveEvent
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.main.ui.main.MainFragmentDirections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AppsFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    packageRepo: AppRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private var ssFilterOptions: FilterOptions?
        get() = handle[FilterOptions::class.simpleName!!]
        set(value) = handle.set(FilterOptions::class.simpleName!!, value)

    private var ssSortOptions: SortOptions?
        get() = handle[SortOptions::class.simpleName!!]
        set(value) = handle.set(SortOptions::class.simpleName!!, value)

    private val searchTerm = MutableStateFlow<String?>(null)
    private val filterOptions = MutableStateFlow(ssFilterOptions ?: FilterOptions())
    private val sortOptions = MutableStateFlow(ssSortOptions ?: SortOptions())

    val events = SingleLiveEvent<AppsEvents>()

    data class State(
        val items: List<AppsAdapter.Item> = emptyList(),
        val isLoading: Boolean = true
    )

    val listData: LiveData<State> = combine(
        packageRepo.apps,
        searchTerm,
        filterOptions,
        sortOptions
    ) { apps, searchTerm, filterOptions, sortOptions ->
        val filtered = apps
            .filter { app -> filterOptions.keys.all { it.matches(app) } }
            .filter {
                val prunedTerm = searchTerm?.lowercase() ?: return@filter true
                if (it.id.toString().lowercase().contains(prunedTerm)) return@filter true
                if (it.getLabel(context)?.lowercase()?.contains(prunedTerm) == true) {
                    return@filter true
                }

                return@filter false
            }
            .sortedWith(sortOptions.mainSort.getComparator(context))

        val listItems = filtered.map { app ->
            when (app) {
                is BasicPkgContainer -> NormalAppVH.Item(
                    app = app,
                    onIconClicked = { events.postValue(AppsEvents.ShowAppSystemDetails(it)) },
                    onRowClicked = {
                        log(TAG) { "Navigating to $app" }
                        MainFragmentDirections.actionMainFragmentToAppDetailsFragment(app.id, app.getLabel(context))
                            .navigate()
                    },
                    onTagClicked = { events.postValue(AppsEvents.ShowPermissionSnackbar(it)) },
                    onTagLongClicked = { events.postValue(AppsEvents.RunPermAction(it.getAction(context))) }
                )
                else -> throw IllegalArgumentException()
            }
        }
        State(
            items = listItems,
            isLoading = false
        )
    }
        .onStart { emit(State()) }
        .asLiveData2()

    fun onSearchInputChanged(term: String?) {
        log { "onSearchInputChanged(term=$term)" }
        searchTerm.value = term
    }

    fun updateFilterOptions(action: (FilterOptions) -> FilterOptions) {
        val old = filterOptions.value
        val new = action(old)
        log { "updateFilterOptions($old) -> $new" }
        ssFilterOptions = new
        filterOptions.value = new
    }

    fun updateSortOptions(action: (SortOptions) -> SortOptions) {
        val old = sortOptions.value
        val new = action(old)
        log { "updateFilterOptions($old) -> $new" }
        ssSortOptions = new
        sortOptions.value = new
    }

    fun showFilterDialog() {
        log { "showFilterDialog" }
        events.postValue(AppsEvents.ShowFilterDialog(filterOptions.value))
    }

    fun showSortDialog() {
        log { "showSortDialog" }
        events.postValue(AppsEvents.ShowSortDialog(sortOptions.value))
    }
}