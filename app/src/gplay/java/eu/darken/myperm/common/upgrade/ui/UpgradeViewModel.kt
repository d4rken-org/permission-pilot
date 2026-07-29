package eu.darken.myperm.common.upgrade.ui

import android.app.Activity
import android.os.SystemClock
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.INFO
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.SingleEventFlow
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.uix.ViewModel4
import eu.darken.myperm.common.upgrade.core.MyPermSku
import eu.darken.myperm.common.upgrade.core.UpgradeRepoGplay
import eu.darken.myperm.common.upgrade.core.client.GplayServiceUnavailableException
import eu.darken.myperm.common.upgrade.core.client.ItemAlreadyOwnedBillingException
import eu.darken.myperm.common.upgrade.core.client.UserCanceledBillingException
import eu.darken.myperm.common.upgrade.core.data.Sku
import eu.darken.myperm.common.upgrade.core.data.SkuDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val upgradeRepo: UpgradeRepoGplay,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider) {

    sealed interface UpgradeEvent {
        data object RestoreFailed : UpgradeEvent
        data object RestoreSucceeded : UpgradeEvent
        data object SubscriptionStillRenewing : UpgradeEvent
        data object SubscriptionCheckFailed : UpgradeEvent
    }

    val events = SingleEventFlow<UpgradeEvent>()

    // null until the nav route resolves via init(manage). The state flow gates on this so we never
    // render (or auto-navigate) before we know whether this is the sales or the manage screen.
    private val manageMode = MutableStateFlow<Boolean?>(null)

    // Single money-path guard: restore, switch-verification and purchase launch are mutually exclusive.
    private val action = MutableStateFlow(ActionState.IDLE)

    // Bumped to re-run the offer query (manual retry, or an on-resume retry after a failure).
    private val retryTrigger = MutableStateFlow(0)

    // Query-status of the purchase offers. Kept as a VM-lifetime hot flow (Eagerly), NOT a cold flow
    // folded into the combined UI state: that way backgrounding the screen (which drops the state
    // subscription after 5s) can't discard a loaded catalog and re-query with a loading flash on
    // return. flatMapLatest cancels an in-flight query when a newer retry supersedes it.
    private val pricingState: StateFlow<PricingState> = retryTrigger
        .flatMapLatest {
            flow {
                emit(PricingState.Pending)
                emit(queryPricing())
            }
        }
        .stateIn(vmScope, SharingStarted.Eagerly, PricingState.Pending)

    // Re-evaluates the 24h grace-diagnostics boundary while the screen is open (the boundary is
    // derived from a wall-clock timestamp, so combined flows alone won't re-fire when it elapses).
    private val graceTick = flow {
        while (true) {
            emit(Unit)
            delay(GRACE_TICK_INTERVAL_MS)
        }
    }

    val state: StateFlow<UpgradeUiState> = combine(
        pricingState,
        upgradeRepo.upgradeInfo,
        upgradeRepo.wasEverPro,
        upgradeRepo.lastProConfirmedAt,
        combine(action, manageMode.filterNotNull(), graceTick) { a, m, _ -> a to m },
    ) { pricing, _, wasEverPro, lastProAt, (action, manage) ->
        // Resolve the freshest upgradeInfo rather than trusting this combine slot's value: `combine`
        // provides no ordering between its branches, so the action/manage branch can fire while the
        // info branch still holds the pre-reconciliation snapshot. Settledness rides the Info itself,
        // so reading .value pairs "settled" with the ownership data it certifies, never a stale
        // non-Pro one (which would briefly flash Unavailable, or enabled sales actions, to an owner
        // on cold start). The upgradeInfo branch is still present in combine() above solely to
        // trigger recomputation when the Info changes.
        val gplayInfo = upgradeRepo.upgradeInfo.value as? UpgradeRepoGplay.Info
            ?: UpgradeRepoGplay.Info(billingData = null)
        val isSettled = gplayInfo.isSettled
        // Owners AND grace users are Pro: their status/management surface renders regardless of the
        // offer catalog, so a pending or failed price query never blocks them (Loading) nor errors
        // them out (Unavailable). Only a genuine, settled, non-Pro buyer sees Unavailable.
        val priceIndependent = gplayInfo.isPro

        fun loaded(resolved: Pricing) = toLoadedState(
            manageMode = manage,
            info = gplayInfo,
            pricing = resolved,
            lastProConfirmedAt = lastProAt,
            now = System.currentTimeMillis(),
            isSettled = isSettled,
            action = action,
            wasEverPro = wasEverPro,
            graceDiagnosticsAfterMs = GRACE_DIAGNOSTICS_AFTER_MS,
        )

        when (pricing) {
            is PricingState.Ready -> loaded(pricing.pricing)
            PricingState.Pending -> if (isSettled && priceIndependent) loaded(Pricing()) else UpgradeUiState.Loading
            is PricingState.Failed -> when {
                priceIndependent -> loaded(Pricing())
                !isSettled -> UpgradeUiState.Loading
                else -> UpgradeUiState.Unavailable(pricing.error)
            }
        }
    }.stateIn(vmScope, SharingStarted.WhileSubscribed(5_000), UpgradeUiState.Loading)

    init {
        // Kick off the first billing reconciliation; the Info's isSettled flips once it publishes.
        // (The offer query is started separately by pricingState's Eagerly sharing.)
        launch {
            try {
                upgradeRepo.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log(TAG, WARN) { "Initial refresh failed: ${e.asLog()}" }
            }
        }
    }

    fun init(manage: Boolean) {
        if (!manageMode.compareAndSet(null, manage)) return
        log(TAG) { "init(manage=$manage)" }
        if (!manage) {
            // Sales mode only: close the screen the instant the user becomes Pro. Manage mode stays
            // open so a current subscriber can view status and switch plans.
            upgradeRepo.upgradeInfo
                .map { it.isPro }
                .distinctUntilChanged()
                .onEach { isPro -> if (isPro) navUp() }
                .launchIn(vmScope)
        }
    }

    // Re-query on return from Google Play (e.g. after the user cancels the subscription in Play), so
    // the locked one-time offer unlocks without needing a manual refresh action. Also retries the SKU
    // pricing if a previous query failed (a transient Play outage otherwise leaves the screen with no
    // purchase buttons even after billing recovers).
    fun onResume() = launch {
        try {
            upgradeRepo.refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(TAG, WARN) { "onResume refresh failed: ${e.asLog()}" }
        }
        // Retry the offer query only if the last attempt failed. Ready = usable offers already loaded;
        // Pending = a query is in flight, restarting it would only cause a needless loading flash.
        if (pricingState.value is PricingState.Failed) retrySkuQuery()
    }

    // Re-run the offer query (from the Unavailable card's retry button, or on-resume after a failure).
    fun retrySkuQuery() {
        log(TAG) { "retrySkuQuery()" }
        retryTrigger.update { it + 1 }
    }

    private suspend fun queryPricing(): PricingState {
        val skuDetails = try {
            withTimeoutOrNull(SKU_QUERY_TIMEOUT_MS) { upgradeRepo.querySkus() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to query SKUs: ${e.asLog()}" }
            return PricingState.Failed(GplayServiceUnavailableException(e))
        } ?: run {
            log(TAG, WARN) { "SKU query timed out" }
            return PricingState.Failed(GplayServiceUnavailableException(RuntimeException("SKU query timed out")))
        }

        val iapDetails = skuDetails.firstOrNull { it.sku.type == Sku.Type.IAP }
        val subDetails = skuDetails.firstOrNull { it.sku.type == Sku.Type.SUBSCRIPTION }
        val subOffers = subDetails?.details?.subscriptionOfferDetails
        val baseOffer = subOffers?.firstOrNull { MyPermSku.Sub.BASE_OFFER.matches(it) }
        val hasTrialOffer = subOffers?.any { MyPermSku.Sub.TRIAL_OFFER.matches(it) } == true

        val pricing = Pricing(
            iap = iapDetails,
            sub = subDetails,
            subPrice = baseOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice,
            iapPrice = iapDetails?.details?.oneTimePurchaseOfferDetails?.formattedPrice,
            hasTrialOffer = hasTrialOffer,
        )
        // Both configured products must come back. A response missing either the subscription or the
        // one-time offer is, to the buyer, a half-broken screen — treat it as a failure so we show a
        // retry instead of a partial catalog (Play normally returns both; one missing signals a glitch).
        return if (pricing.subAvailable && pricing.iapAvailable) {
            PricingState.Ready(pricing)
        } else {
            log(TAG, WARN) { "SKU query returned an incomplete offer set (sub=${pricing.subAvailable}, iap=${pricing.iapAvailable})" }
            PricingState.Failed(GplayServiceUnavailableException(RuntimeException("Incomplete purchase offer set")))
        }
    }

    fun onSubscribe(activity: Activity) {
        val current = pricingState.value.readyPricing ?: return
        val skuDetails = current.sub ?: return
        val offer = if (current.hasTrialOffer) MyPermSku.Sub.TRIAL_OFFER else MyPermSku.Sub.BASE_OFFER
        // Acquire the guard synchronously on the caller (main) thread BEFORE launching, so two rapid
        // taps can't both slip past the CAS from separate coroutines.
        if (!action.compareAndSet(ActionState.IDLE, ActionState.VERIFYING)) return
        launch {
            try {
                launchBillingFlow(activity, skuDetails, offer)
            } finally {
                action.value = ActionState.IDLE
            }
        }
    }

    // Both the sales "Buy" button and the ownership "switch" route through here. EVERY one-time (IAP)
    // acquisition is gated on a fresh SUBS verification and fails closed, so we never launch the
    // one-time purchase while a subscription may still be auto-renewing (double billing) — including
    // during a grace window where a stale entitlement could otherwise expose an unguarded buy button.
    fun onBuyIap(activity: Activity) = verifyThenLaunchIap(activity)

    fun onSwitchToIap(activity: Activity) = verifyThenLaunchIap(activity)

    private fun verifyThenLaunchIap(activity: Activity) {
        if (!action.compareAndSet(ActionState.IDLE, ActionState.VERIFYING)) {
            log(TAG) { "IAP acquisition ignored, an action is already in progress" }
            return
        }
        launch {
            try {
                val subscriptions = try {
                    withTimeoutOrNull(VERIFY_TIMEOUT_MS) { upgradeRepo.queryCurrentSubscriptions() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log(TAG, WARN) { "Subscription verify failed: ${e.asLog()}" }
                    errorEvents.emitBlocking(e)
                    return@launch
                }
                when {
                    subscriptions == null -> {
                        log(TAG, WARN) { "Subscription verify timed out -> failing closed" }
                        events.tryEmit(UpgradeEvent.SubscriptionCheckFailed)
                    }

                    subscriptions.any { it.isAutoRenewing } -> {
                        log(TAG, INFO) { "Subscription still renewing -> blocking one-time purchase" }
                        events.tryEmit(UpgradeEvent.SubscriptionStillRenewing)
                    }

                    else -> {
                        val iap = pricingState.value.readyPricing?.iap
                        if (iap == null) {
                            // The IAP catalog isn't loaded. The UI gates the buy/switch buttons on IAP
                            // availability (OffersCard / SwitchOfferCard), so this is only reachable if
                            // the catalog dropped between render and tap — abort quietly rather than
                            // launch a broken flow or pop a dialog.
                            log(TAG, WARN) { "No IAP SkuDetails available -> aborting purchase launch" }
                        } else {
                            launchBillingFlow(activity, iap, null)
                        }
                    }
                }
            } finally {
                action.value = ActionState.IDLE
            }
        }
    }

    fun onManageSubscription() {
        val url = "https://play.google.com/store/account/subscriptions" +
            "?sku=${MyPermSku.Sub.PRO_UPGRADE.id}&package=${BuildConfigWrap.APPLICATION_ID}"
        webpageTool.open(url)
    }

    fun onContactSupport() {
        navTo(Nav.Settings.ContactForm)
    }

    // Assumes the action guard is already held by the caller.
    private suspend fun launchBillingFlow(
        activity: Activity,
        skuDetails: SkuDetails,
        offer: Sku.Subscription.Offer?,
    ) {
        try {
            withContext(dispatcherProvider.Main) {
                upgradeRepo.launchBillingFlow(activity, skuDetails, offer)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UserCanceledBillingException) {
            log(TAG) { "User canceled the billing flow" }
        } catch (e: ItemAlreadyOwnedBillingException) {
            log(TAG, INFO) { "Buy attempt says already owned, restoring purchase instead" }
            restoreAfterAlreadyOwned(e, skuDetails)
        } catch (e: Exception) {
            log(TAG, WARN) { "launchBillingFlow failed: ${e.asLog()}" }
            errorEvents.emitBlocking(e)
        }
    }

    // Recovery for ITEM_ALREADY_OWNED: only treat it as resolved if the restore actually returns the
    // EXACT SKU the launch was for (not merely any Pro purchase / a grace-only state).
    private suspend fun restoreAfterAlreadyOwned(
        original: ItemAlreadyOwnedBillingException,
        skuDetails: SkuDetails,
    ) {
        val restored = try {
            withTimeoutOrNull(RESTORE_TIMEOUT_MS) { upgradeRepo.restorePurchaseNow() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(TAG, WARN) { "Restore after already-owned failed: ${e.asLog()}" }
            null
        }
        val ownsExact = (restored as? UpgradeRepoGplay.Info)
            ?.proPurchases
            ?.any { it.sku.id == skuDetails.sku.id } == true
        if (ownsExact) {
            log(TAG, INFO) { "Restored ${skuDetails.sku} after already-owned buy attempt" }
        } else {
            errorEvents.emitBlocking(original)
        }
    }

    fun onRestore() {
        // Acquire synchronously before launching (see onSubscribe).
        if (!action.compareAndSet(ActionState.IDLE, ActionState.RESTORING)) {
            log(TAG) { "onRestore ignored, an action is already in progress" }
            return
        }
        launch { runRestore() }
    }

    private suspend fun runRestore() {
        log(TAG) { "onRestore()" }
        try {
            // Keep the progress state visible for a minimum duration, so a warm-cache restore still
            // reads as "a live Play check happened" instead of flashing for a single frame. Padding
            // the remainder after the query yields the same max(queryTime, minVisible) floor.
            val startedAt = SystemClock.elapsedRealtime()
            val restored = withTimeoutOrNull(RESTORE_TIMEOUT_MS) { upgradeRepo.restorePurchaseNow() }
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            if (elapsed < RESTORE_MIN_VISIBLE_MS) delay(RESTORE_MIN_VISIBLE_MS - elapsed)
            val ownsSomething = (restored as? UpgradeRepoGplay.Info)?.proPurchases?.isNotEmpty() == true
            when {
                restored == null -> {
                    log(TAG, WARN) { "Restore purchase timed out" }
                    events.tryEmit(UpgradeEvent.RestoreFailed)
                }

                // Grace-only Pro (no returned purchase) is NOT a successful restore.
                ownsSomething -> {
                    log(TAG, INFO) { "Restored purchase :)" }
                    events.tryEmit(UpgradeEvent.RestoreSucceeded)
                }

                else -> {
                    log(TAG, WARN) { "No pro purchase found" }
                    events.tryEmit(UpgradeEvent.RestoreFailed)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(TAG, WARN) { "Restore purchase errored: ${e.asLog()}" }
            errorEvents.emitBlocking(e)
        } finally {
            action.value = ActionState.IDLE
        }
    }

    private sealed interface PricingState {
        // A query is in flight (initial load or a retry generation).
        data object Pending : PricingState

        // The query failed, timed out, or returned no usable offer. Non-Pro buyers see the Unavailable
        // retry card; owners/grace users ignore it.
        data class Failed(val error: Throwable) : PricingState

        // At least one usable offer (subscription or one-time) is available.
        data class Ready(val pricing: Pricing) : PricingState

        val readyPricing: Pricing? get() = (this as? Ready)?.pricing
    }

    companion object {
        internal const val VERIFY_TIMEOUT_MS = 10_000L
        internal const val SKU_QUERY_TIMEOUT_MS = 15_000L
        internal const val RESTORE_TIMEOUT_MS = 15_000L
        internal const val RESTORE_MIN_VISIBLE_MS = 1_500L
        internal const val GRACE_DIAGNOSTICS_AFTER_MS = 24 * 60 * 60 * 1000L
        private const val GRACE_TICK_INTERVAL_MS = 60_000L
        private val TAG = logTag("Upgrade", "Gplay", "VM")
    }
}
