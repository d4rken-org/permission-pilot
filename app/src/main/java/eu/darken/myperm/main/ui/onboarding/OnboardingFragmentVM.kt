package eu.darken.myperm.main.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.uix.ViewModel3
import eu.darken.myperm.settings.core.GeneralSettings
import javax.inject.Inject

@HiltViewModel
class OnboardingFragmentVM @Inject constructor(
    @Suppress("UNUSED_PARAMETER") handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val generalSettings: GeneralSettings,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    fun finishOnboarding() {
        generalSettings.isOnboardingFinished.value = true
        OnboardingFragmentDirections.actionOnboardingFragmentToMainFragment().navigate()
    }

}