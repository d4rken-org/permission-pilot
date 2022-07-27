package eu.darken.myperm.main.ui.overview

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class OverviewFragmentVM @Inject constructor(
    @Suppress("unused") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    packageRepo: AppRepo,
    permissionRepo: PermissionRepo,
    private val generalSettings: GeneralSettings,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {


    data class State(
        val items: List<OverviewAdapter.Item> = emptyList(),
        val isLoading: Boolean = true,
    )

    val listData: LiveData<State> = combine(
        packageRepo.apps,
        permissionRepo.permissions,
    ) { apps, permissions ->

        State(
            items = emptyList(),
            isLoading = false
        )
    }
        .onStart { emit(State()) }
        .asLiveData2()

}