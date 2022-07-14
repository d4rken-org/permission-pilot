package eu.darken.myperm.apps.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.navigation.navArgs
import eu.darken.myperm.common.uix.ViewModel3
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class AppDetailsFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    appRepo: AppRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val navArgs: AppDetailsFragmentArgs by handle.navArgs()

    data class Details(
        val app: BaseApp,
        val label: String,
    )

    val details: LiveData<Details> = appRepo.apps
        .map { apps -> apps.single { it.id == navArgs.appId } }
        .map { app ->
            app as NormalApp
            Details(
                app = app,
                label = app.label ?: app.packageName,
            )
        }
        .asLiveData2()
}