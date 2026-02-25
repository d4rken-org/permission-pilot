package eu.darken.myperm.main.ui.onboarding

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.PrivacyPolicy
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.settings.core.GeneralSettings
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val generalSettings: GeneralSettings,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider) {

    fun openPrivacyPolicy() {
        webpageTool.open(PrivacyPolicy.URL)
    }

    fun finishOnboarding() = launch {
        generalSettings.isOnboardingFinished.value = true
        navTo(Nav.Tab.Apps, popUpTo = Nav.Main.Onboarding, inclusive = true)
    }

    companion object {
        private val TAG = logTag("Onboarding", "VM")
    }
}
