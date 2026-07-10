package eu.darken.myperm.common.upgrade.ui

import android.app.Activity
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.INFO
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.core.MyPermSku
import eu.darken.myperm.common.upgrade.core.UpgradeRepoGplay
import eu.darken.myperm.common.upgrade.core.client.ItemAlreadyOwnedBillingException
import eu.darken.myperm.common.upgrade.core.client.UserCanceledBillingException
import eu.darken.myperm.common.upgrade.core.data.Sku
import eu.darken.myperm.common.upgrade.core.data.SkuDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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

    data class State(
        val pricing: Pricing? = null,
        val wasPreviouslyPro: Boolean = false,
        val restoreInProgress: Boolean = false,
    )

    val events = SingleEventFlow<UpgradeEvent>()
    val billingEvents = SingleEventFlow<BillingEvent>()

    private val restoring = MutableStateFlow(false)

    private val pricing: Flow<Pricing?> = flow {
        emit(null)

        val skuDetails = try {
            withTimeoutOrNull(5_000L) {
                upgradeRepo.querySkus()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to query SKUs: ${e.asLog()}" }
            null
        }

        val iapDetails = skuDetails?.firstOrNull { it.sku.type == Sku.Type.IAP }
        val subDetails = skuDetails?.firstOrNull { it.sku.type == Sku.Type.SUBSCRIPTION }

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
    }

    val state: StateFlow<State> = combine(
        pricing,
        upgradeRepo.upgradeInfo,
        upgradeRepo.wasEverPro,
        restoring,
    ) { pricing, current, wasEverPro, isRestoring ->
        State(
            pricing = pricing,
            // Hidden while a purchase or the grace period still keeps the user Pro.
            wasPreviouslyPro = wasEverPro && !current.isPro,
            restoreInProgress = isRestoring,
        )
    }.stateIn(vmScope, SharingStarted.WhileSubscribed(5_000), State())

    init {
        // Dedupe on isPro: consecutive Pro emissions with different backing data (reactive cache
        // union vs a fresh restore result) must not enqueue multiple navUp events.
        upgradeRepo.upgradeInfo
            .map { it.isPro }
            .distinctUntilChanged()
            .onEach { isPro ->
                if (isPro) {
                    log(TAG) { "User is now pro, navigating back" }
                    navUp()
                }
            }
            .launchIn(vmScope)

        // Purchase-flow failures that arrive asynchronously via onPurchasesUpdated after the Play
        // sheet opened — handled like the synchronous launch failures.
        upgradeRepo.purchaseFailures
            .onEach { failure ->
                when (failure) {
                    is UserCanceledBillingException -> log(TAG) { "User canceled the billing flow" }

                    is ItemAlreadyOwnedBillingException -> {
                        log(TAG, INFO) { "Purchase update says already owned, restoring purchase instead" }
                        restoreAfterAlreadyOwned(failure)
                    }

                    else -> {
                        log(TAG, WARN) { "Purchase flow failed: ${failure.asLog()}" }
                        errorEvents.emitBlocking(failure)
                    }
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

    fun launchBillingIap(activity: Activity) =
        launchBillingFlow(activity, state.value.pricing?.iap, null)

    fun launchBillingSubscription(activity: Activity) =
        launchBillingFlow(activity, state.value.pricing?.sub, MyPermSku.Sub.BASE_OFFER)

    fun launchBillingSubscriptionTrial(activity: Activity) =
        launchBillingFlow(activity, state.value.pricing?.sub, MyPermSku.Sub.TRIAL_OFFER)

    private fun launchBillingFlow(
        activity: Activity,
        skuDetails: SkuDetails?,
        offer: Sku.Subscription.Offer?,
    ) = launch {
        skuDetails ?: return@launch
        try {
            withContext(dispatcherProvider.Main) {
                upgradeRepo.launchBillingFlow(activity, skuDetails, offer)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UserCanceledBillingException) {
            log(TAG) { "User canceled the billing flow" }
        } catch (e: ItemAlreadyOwnedBillingException) {
            // Stale local state: Play says they already own it, so tapping "buy" really means
            // "unlock what I own" — restore instead of showing an error.
            log(TAG, INFO) { "Buy attempt says already owned, restoring purchase instead" }
            restoreAfterAlreadyOwned(e)
        } catch (e: Exception) {
            log(TAG, WARN) { "launchBillingFlow failed: ${e.asLog()}" }
            errorEvents.emitBlocking(e)
        }
    }

    private suspend fun restoreAfterAlreadyOwned(original: ItemAlreadyOwnedBillingException) {
        if (!restoring.compareAndSet(expect = false, update = true)) {
            log(TAG) { "Restore already in progress, letting it resolve the entitlement" }
            return
        }
        try {
            val restored = try {
                withTimeoutOrNull(RESTORE_TIMEOUT_MS) { upgradeRepo.restorePurchaseNow() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log(TAG, WARN) { "Restore after already-owned failed: ${e.asLog()}" }
                null
            }
            if (restored?.isPro == true) {
                log(TAG, INFO) { "Restored purchase after already-owned buy attempt" }
            } else {
                // Couldn't reconcile the entitlement (pending purchase, account mismatch, Play
                // quirk) — fall back to the already-owned dialog with restore tips.
                errorEvents.emitBlocking(original)
            }
        } finally {
            restoring.value = false
        }
    }

    fun restorePurchase() = launch {
        // Single-flight: repeated taps while a restore is running (worst case bounded by
        // RESTORE_TIMEOUT_MS) must not stack concurrent restores and duplicate result dialogs.
        if (!restoring.compareAndSet(expect = false, update = true)) {
            log(TAG) { "restorePurchase() ignored, already in progress" }
            return@launch
        }
        log(TAG) { "restorePurchase()" }
        try {
            val restored = withTimeoutOrNull(RESTORE_TIMEOUT_MS) { upgradeRepo.restorePurchaseNow() }
            when {
                restored == null -> {
                    log(TAG, WARN) { "Restore purchase timed out" }
                    events.tryEmit(UpgradeEvent.RestoreFailed)
                }

                restored.isPro -> log(TAG, INFO) { "Restored purchase :)" }

                else -> {
                    log(TAG, WARN) { "No pro purchase found" }
                    events.tryEmit(UpgradeEvent.RestoreFailed)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Play/billing error (e.g. service unavailable): surface the proper error dialog
            // instead of the generic "no purchases found" message.
            log(TAG, WARN) { "Restore purchase errored: ${e.asLog()}" }
            errorEvents.emitBlocking(e)
        } finally {
            // Reset only after result handling, so the single-flight guard covers the whole action.
            restoring.value = false
        }
    }

    companion object {
        internal const val RESTORE_TIMEOUT_MS = 15_000L
        private val TAG = logTag("Upgrade", "Gplay", "VM")
    }
}
