package eu.darken.myperm.settings.ui.general

import android.app.Activity
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.theming.ThemeColor
import eu.darken.myperm.common.theming.ThemeMode
import eu.darken.myperm.common.theming.ThemeStyle
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val generalSettings: GeneralSettings,
    private val upgradeRepo: UpgradeRepo,
) : ViewModel4(dispatcherProvider) {

    val themeMode: Flow<ThemeMode> = generalSettings.themeMode.flow
    val themeStyle: Flow<ThemeStyle> = generalSettings.themeStyle.flow
    val themeColor: Flow<ThemeColor> = generalSettings.themeColor.flow
    val isPro: StateFlow<Boolean> = upgradeRepo.upgradeInfo
        .map { it.isPro }
        .stateIn(vmScope, SharingStarted.Eagerly, true)

    fun setThemeMode(mode: ThemeMode) = launch {
        generalSettings.themeMode.value(mode)
    }

    fun setThemeStyle(style: ThemeStyle) = launch {
        generalSettings.themeStyle.value(style)
    }

    fun setThemeColor(color: ThemeColor) = launch {
        generalSettings.themeColor.value(color)
    }

    fun onUpgrade(activity: Activity) {
        upgradeRepo.launchBillingFlow(activity)
    }

    companion object {
        private val TAG = logTag("Settings", "General", "VM")
    }
}
