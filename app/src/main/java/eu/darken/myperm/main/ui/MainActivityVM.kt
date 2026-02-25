package eu.darken.myperm.main.ui

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.uix.ViewModel2
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class MainActivityVM @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    private val upgradeRepo: UpgradeRepo,
    private val generalSettings: GeneralSettings,
) : ViewModel2(dispatcherProvider = dispatcherProvider) {

    private val _readyState = MutableStateFlow(false)
    val readyState = _readyState.asStateFlow()

    val upgradeNag = SingleEventFlow<(Activity) -> Unit>()

    init {
        upgradeRepo.upgradeInfo
            .take(1)
            .onEach {
                _readyState.value = true

                if (it.isPro) return@onEach

                val launchCount = generalSettings.launchCount.value
                val skipNag = launchCount == 0 || launchCount % 8 != 0
                log { "LaunchCount: $launchCount (skipNag=$skipNag)" }
                if (skipNag) return@onEach

                upgradeNag.emit { activity ->
                    upgradeRepo.launchBillingFlow(activity)
                }
            }
            .launchInViewModel()
    }

    fun increaseLaunchCount() {
        generalSettings.launchCount.update {
            log { "LaunchCount was $it" }
            it + 1
        }
    }
}
