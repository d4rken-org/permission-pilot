package eu.darken.myperm.settings.ui.general

import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.theming.ThemeColor
import eu.darken.myperm.common.theming.ThemeMode
import eu.darken.myperm.common.theming.ThemeStyle
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.common.upgrade.isProForUi
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
    /**
     * True only when billing settled without an error and reports no entitlement — the presentation
     * mirror of what [isProForUi] would deny. While billing is still connecting (GPlay cold-start
     * seed) a paying user keeps the real controls instead of being shown the upgrade branch; the
     * setters re-check via [isProForUi] before writing.
     */
    val isUpgradeLocked: StateFlow<Boolean> = upgradeRepo.upgradeInfo
        .map { it.error == null && it.isSettled && !it.isPro }
        .stateIn(vmScope, SharingStarted.Eagerly, false)

    fun setThemeMode(mode: ThemeMode) = launch {
        if (!upgradeRepo.isProForUi()) {
            navTo(Nav.Main.Upgrade())
            return@launch
        }
        generalSettings.themeMode.value(mode)
    }

    fun setThemeStyle(style: ThemeStyle) = launch {
        if (!upgradeRepo.isProForUi()) {
            navTo(Nav.Main.Upgrade())
            return@launch
        }
        generalSettings.themeStyle.value(style)
    }

    fun setThemeColor(color: ThemeColor) = launch {
        if (!upgradeRepo.isProForUi()) {
            navTo(Nav.Main.Upgrade())
            return@launch
        }
        generalSettings.themeColor.value(color)
    }

    val ipcParallelisation: Flow<Int> = generalSettings.ipcParallelisation.flow

    fun setIpcParallelisation(value: Int) = launch {
        if (!upgradeRepo.isProForUi()) {
            navTo(Nav.Main.Upgrade())
            return@launch
        }
        generalSettings.ipcParallelisation.value(value)
    }

    fun onUpgrade() {
        navTo(Nav.Main.Upgrade())
    }

    companion object {
        private val TAG = logTag("Settings", "General", "VM")
    }
}
