package eu.darken.myperm.apps.ui.list

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.AppStoreTool
import eu.darken.myperm.apps.core.container.BasicPkgContainer
import eu.darken.myperm.apps.ui.list.apps.NormalAppVH
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.livedata.SingleLiveEvent
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.main.ui.main.MainFragmentDirections
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AppsFragmentVM @Inject constructor(
    @Suppress("unused") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    packageRepo: AppRepo,
    private val generalSettings: GeneralSettings,
    private val webpageTool: WebpageTool,
    private val appStoreTool: AppStoreTool,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val searchTerm = MutableStateFlow<String?>(null)
    private val filterOptions = generalSettings.appsFilterOptions.flow
    private val sortOptions = generalSettings.appsSortOptions.flow

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
                    onTagLongClicked = { events.postValue(AppsEvents.RunPermAction(it.getAction(context))) },
                    onInstallerClicked = { installer -> appStoreTool.openAppStoreFor(app, installer) }
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

    fun updateFilterOptions(action: (AppsFilterOptions) -> AppsFilterOptions) {
        generalSettings.appsFilterOptions.update { action(it) }
    }

    fun updateSortOptions(action: (AppsSortOptions) -> AppsSortOptions) {
        generalSettings.appsSortOptions.update { action(it) }
    }

    fun showFilterDialog() {
        log { "showFilterDialog" }
        events.postValue(AppsEvents.ShowFilterDialog(generalSettings.appsFilterOptions.value))
    }

    fun showSortDialog() {
        log { "showSortDialog" }
        events.postValue(AppsEvents.ShowSortDialog(generalSettings.appsSortOptions.value))
    }
}