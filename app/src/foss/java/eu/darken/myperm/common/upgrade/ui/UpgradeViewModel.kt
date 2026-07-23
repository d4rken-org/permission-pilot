package eu.darken.myperm.common.upgrade.ui

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.core.FossUpgrade
import eu.darken.myperm.common.upgrade.core.UpgradeControlFoss
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

enum class FossUpgradeView { PITCH, STATUS_FREE, STATUS_UPGRADED }

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

    // null until the route resolves. Keeps the view unresolved (Loading in the UI) until we know
    // whether this is the sponsor pitch or the status screen.
    private val manageMode = MutableStateFlow<Boolean?>(null)

    data class State(
        val view: FossUpgradeView? = null,
        val upgradedAt: Instant? = null,
        val reason: FossUpgrade.Reason? = null,
    )

    val state: StateFlow<State> = combine(
        manageMode.filterNotNull(),
        upgradeControlFoss.upgradeInfo,
    ) { manage, info ->
        val gplayInfo = info as? UpgradeControlFoss.Info
        val view = when {
            // Upgraded status always wins, even if a stale "show pitch" route was requested.
            info.isPro -> FossUpgradeView.STATUS_UPGRADED
            manage -> FossUpgradeView.STATUS_FREE
            else -> FossUpgradeView.PITCH
        }
        State(view = view, upgradedAt = info.upgradedAt, reason = gplayInfo?.upgradeReason)
    }.stateIn(vmScope, SharingStarted.WhileSubscribed(5_000), State())

    fun init(manage: Boolean) {
        manageMode.compareAndSet(null, manage)
    }

    // Arming sponsor flow (pitch / free-status): after returning from the page (>= 5s) we unlock Pro
    // locally on resume.
    fun sponsor() {
        savedStateHandle[KEY_SPONSOR_OPENED_AT] = System.currentTimeMillis()
        webpageTool.open(SPONSOR_URL)
    }

    // Non-arming: from the upgraded-status view (recurring-donation nudge). Must NOT re-trigger the
    // resume unlock heuristic — the user is already a supporter.
    fun openSponsorPage() {
        webpageTool.open(SPONSOR_URL)
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
        private const val SPONSOR_URL = "https://github.com/sponsors/d4rken"
        private val TAG = logTag("Upgrade", "Foss", "VM")
    }
}
