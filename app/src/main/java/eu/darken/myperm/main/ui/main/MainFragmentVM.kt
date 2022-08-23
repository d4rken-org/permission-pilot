package eu.darken.myperm.main.ui.main

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.apps
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.livedata.SingleLiveEvent
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.permissions
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class MainFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val appRepo: AppRepo,
    private val permissionRepo: PermissionRepo,
    private val upgradeRepo: UpgradeRepo,
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

    val launchUpgradeFlow = SingleLiveEvent<(Activity) -> Unit>()
    val upgradeInfo = upgradeRepo.upgradeInfo.asLiveData2()

    fun onUpgrade() = launch {
        val call: (Activity) -> Unit = {
            upgradeRepo.launchBillingFlow(it)
        }
        launchUpgradeFlow.postValue(call)
    }

    fun onRefresh() = launch {
        log { "refresh()" }
        appRepo.refresh()
    }
}