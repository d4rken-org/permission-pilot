package eu.darken.myperm.main.ui.main

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.permissions.core.PermissionRepo
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class MainFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val appRepo: AppRepo,
    private val permissionRepo: PermissionRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    data class State(
        val appCount: Int,
        val permissionCount: Int,
    )

    val state = combine(
        appRepo.apps,
        permissionRepo.permissions
    ) { apps, permissions ->
        State(
            appCount = apps.size,
            permissionCount = permissions.size
        )
    }.asLiveData2()
}