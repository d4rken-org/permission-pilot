package eu.darken.myperm.apps.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.apps.ui.apps.NormalAppVH
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.uix.ViewModel3
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class AppsFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    packageRepo: AppRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    val listData: LiveData<List<AppsAdapter.Item>> = packageRepo.packages
        .map { pkgs ->
            pkgs
                .sortedBy { it.isSystemApp }
                .map { pkg ->
                    when (pkg) {
                        is NormalApp -> NormalAppVH.Item(
                            app = pkg,
                            onClickAction = {

                            }
                        )
                        else -> throw IllegalArgumentException()
                    }
                }
        }
        .asLiveData2()
}