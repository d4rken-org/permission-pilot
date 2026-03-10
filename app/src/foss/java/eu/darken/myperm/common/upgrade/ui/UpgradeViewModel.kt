package eu.darken.myperm.common.upgrade.ui

import java.lang.System
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.core.FossUpgrade
import eu.darken.myperm.common.upgrade.core.UpgradeControlFoss
import javax.inject.Inject

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val upgradeControlFoss: UpgradeControlFoss,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider) {

    sealed interface SponsorEvent {
        data object ReturnedTooEarly : SponsorEvent
    }

    val sponsorEvents = SingleEventFlow<SponsorEvent>()

    fun sponsor() {
        savedStateHandle[KEY_SPONSOR_OPENED_AT] = System.currentTimeMillis()
        webpageTool.open("https://github.com/sponsors/d4rken")
    }

    fun onResume() {
        val openedAt = savedStateHandle.get<Long>(KEY_SPONSOR_OPENED_AT) ?: return
        savedStateHandle.remove<Long>(KEY_SPONSOR_OPENED_AT)

        if (System.currentTimeMillis() - openedAt >= 5_000L) {
            upgradeControlFoss.upgrade(FossUpgrade.Reason.DONATED)
            navUp()
        } else {
            sponsorEvents.tryEmit(SponsorEvent.ReturnedTooEarly)
        }
    }

    companion object {
        private const val KEY_SPONSOR_OPENED_AT = "sponsor_opened_at"
        private val TAG = logTag("Upgrade", "Foss", "VM")
    }
}
