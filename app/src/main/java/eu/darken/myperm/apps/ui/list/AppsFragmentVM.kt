package eu.darken.myperm.apps.ui.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.apps.ui.list.apps.NormalAppVH
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.main.ui.main.MainFragmentDirections
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class AppsFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    packageRepo: AppRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    val listData: LiveData<List<AppsAdapter.Item>> = packageRepo.apps
        .map { pkgs ->
            pkgs
                .sortedBy { it.isSystemApp }
                .map { app ->
                    when (app) {
                        is NormalApp -> NormalAppVH.Item(
                            app = app,
                            onClickAction = {
                                log(TAG) { "Navigating to $app" }
                                MainFragmentDirections.actionMainFragmentToAppDetailsFragment(
                                    appId = app.id
                                ).navigate()
                            }
                        )
                        else -> throw IllegalArgumentException()
                    }
                }
        }
        .asLiveData2()
}