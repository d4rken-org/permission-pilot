package eu.darken.myperm.main.ui

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.theming.ThemeState
import eu.darken.myperm.settings.core.themeState
import eu.darken.myperm.settings.core.themeStateBlocking
import eu.darken.myperm.common.uix.ViewModel2
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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
    val readyState: StateFlow<Boolean> = _readyState

    val themeState: StateFlow<ThemeState> = generalSettings.themeState.stateIn(
        vmScope,
        SharingStarted.Eagerly,
        generalSettings.themeStateBlocking,
    )

    val isOnboardingFinished: Boolean
        get() = generalSettings.isOnboardingFinished.valueBlocking

    init {
        upgradeRepo.upgradeInfo
            .take(1)
            .onEach { _readyState.value = true }
            .launchInViewModel()
    }

    fun increaseLaunchCount() = launch {
        generalSettings.launchCount.update {
            log { "LaunchCount was $it" }
            it + 1
        }
    }
}
