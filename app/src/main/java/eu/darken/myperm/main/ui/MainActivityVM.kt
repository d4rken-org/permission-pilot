package eu.darken.myperm.main.ui

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Intent
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationDestination
import eu.darken.myperm.common.theming.ThemeState
import eu.darken.myperm.settings.core.themeState
import eu.darken.myperm.settings.core.themeStateBlocking
import eu.darken.myperm.common.uix.ViewModel2
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.settings.core.GeneralSettings
import eu.darken.myperm.watcher.core.WatcherNotifications
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

    val upgradeNag = SingleEventFlow<Unit>()
    val deepLinkNav = SingleEventFlow<NavigationDestination>()

    init {
        upgradeRepo.upgradeInfo
            .take(1)
            .onEach {
                _readyState.value = true

                if (it.isPro) return@onEach

                val launchCount = generalSettings.launchCount.valueBlocking
                val skipNag = launchCount == 0 || launchCount % 8 != 0
                log { "LaunchCount: $launchCount (skipNag=$skipNag)" }
                if (skipNag) return@onEach

                upgradeNag.emit(Unit)
            }
            .launchInViewModel()
    }

    fun increaseLaunchCount() = launch {
        generalSettings.launchCount.update {
            log { "LaunchCount was $it" }
            it + 1
        }
    }

    fun handleIntent(intent: Intent?) {
        val reportId = intent?.getLongExtra(WatcherNotifications.EXTRA_REPORT_ID, -1L) ?: -1L
        if (reportId > 0) {
            log { "Deep-link to watcher report: $reportId" }
            launch { deepLinkNav.emit(Nav.Watcher.ReportDetail(reportId)) }
        }
    }
}
