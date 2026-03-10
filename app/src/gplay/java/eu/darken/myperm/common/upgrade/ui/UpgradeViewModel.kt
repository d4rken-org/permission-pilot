package eu.darken.myperm.common.upgrade.ui

import android.app.Activity
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.core.MyPermSku
import eu.darken.myperm.common.upgrade.core.UpgradeRepoGplay
import eu.darken.myperm.common.upgrade.core.data.SkuDetails
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val upgradeRepo: UpgradeRepoGplay,
) : ViewModel4(dispatcherProvider) {

    sealed interface UpgradeEvent {
        data object RestoreFailed : UpgradeEvent
    }

    sealed interface BillingEvent {
        data object LaunchIap : BillingEvent
        data object LaunchSubscription : BillingEvent
        data object LaunchSubscriptionTrial : BillingEvent
    }

    data class Pricing(
        val iap: SkuDetails? = null,
        val sub: SkuDetails? = null,
        val subPrice: String? = null,
        val iapPrice: String? = null,
        val hasTrialOffer: Boolean = false,
    ) {
        val subAvailable: Boolean get() = sub != null || subPrice != null
        val iapAvailable: Boolean get() = iap != null || iapPrice != null
    }

    val events = SingleEventFlow<UpgradeEvent>()
    val billingEvents = SingleEventFlow<BillingEvent>()

    val state: StateFlow<Pricing?> = flow {
        val skuDetails = try {
            withTimeoutOrNull(5_000L) {
                upgradeRepo.querySkus()
            }
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to query SKUs: ${e.asLog()}" }
            null
        }

        val iapDetails = skuDetails?.firstOrNull { it.sku.type == eu.darken.myperm.common.upgrade.core.data.Sku.Type.IAP }
        val subDetails = skuDetails?.firstOrNull { it.sku.type == eu.darken.myperm.common.upgrade.core.data.Sku.Type.SUBSCRIPTION }

        val subOffers = subDetails?.details?.subscriptionOfferDetails
        val baseOffer = subOffers?.firstOrNull { MyPermSku.Sub.BASE_OFFER.matches(it) }
        val hasTrialOffer = subOffers?.any { MyPermSku.Sub.TRIAL_OFFER.matches(it) } == true

        emit(
            Pricing(
                iap = iapDetails,
                sub = subDetails,
                subPrice = baseOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice,
                iapPrice = iapDetails?.details?.oneTimePurchaseOfferDetails?.formattedPrice,
                hasTrialOffer = hasTrialOffer,
            )
        )
    }.stateIn(vmScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        upgradeRepo.upgradeInfo
            .onEach { info ->
                if (info.isPro) {
                    log(TAG) { "User is now pro, navigating back" }
                    navUp()
                }
            }
            .launchIn(vmScope)
    }

    fun onGoIap() {
        billingEvents.tryEmit(BillingEvent.LaunchIap)
    }

    fun onGoSubscription() {
        billingEvents.tryEmit(BillingEvent.LaunchSubscription)
    }

    fun onGoSubscriptionTrial() {
        billingEvents.tryEmit(BillingEvent.LaunchSubscriptionTrial)
    }

    fun launchBillingIap(activity: Activity) = launch {
        try {
            val iap = state.value?.iap ?: return@launch
            withContext(dispatcherProvider.Main) {
                upgradeRepo.launchBillingFlow(activity, iap)
            }
        } catch (e: Exception) {
            log(TAG) { "launchBillingIap failed: $e" }
            errorEvents.emitBlocking(e)
        }
    }

    fun launchBillingSubscription(activity: Activity) = launch {
        try {
            val sub = state.value?.sub ?: return@launch
            withContext(dispatcherProvider.Main) {
                upgradeRepo.launchBillingFlow(activity, sub, MyPermSku.Sub.BASE_OFFER)
            }
        } catch (e: Exception) {
            log(TAG) { "launchBillingSubscription failed: $e" }
            errorEvents.emitBlocking(e)
        }
    }

    fun launchBillingSubscriptionTrial(activity: Activity) = launch {
        try {
            val sub = state.value?.sub ?: return@launch
            withContext(dispatcherProvider.Main) {
                upgradeRepo.launchBillingFlow(activity, sub, MyPermSku.Sub.TRIAL_OFFER)
            }
        } catch (e: Exception) {
            log(TAG) { "launchBillingSubscriptionTrial failed: $e" }
            errorEvents.emitBlocking(e)
        }
    }

    fun restorePurchase() = launch {
        try {
            upgradeRepo.refresh()
            val info = upgradeRepo.upgradeInfo.first()
            if (info.isPro) {
                log(TAG) { "Pro purchase found" }
            } else {
                log(TAG) { "No pro purchase found" }
                events.tryEmit(UpgradeEvent.RestoreFailed)
            }
        } catch (e: Exception) {
            log(TAG) { "Restore failed: $e" }
            errorEvents.emitBlocking(e)
        }
    }

    companion object {
        private val TAG = logTag("Upgrade", "Gplay", "VM")
    }
}
