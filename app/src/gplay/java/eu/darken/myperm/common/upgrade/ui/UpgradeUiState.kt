package eu.darken.myperm.common.upgrade.ui

import eu.darken.myperm.common.upgrade.core.UpgradeRepoGplay
import eu.darken.myperm.common.upgrade.core.data.Sku
import eu.darken.myperm.common.upgrade.core.data.SkuDetails

// Owned-subscription facts the switch UI needs. Derived from replayed upgradeInfo — display only;
// the actual switch launch re-verifies with a fresh SUBS query and never trusts this.
data class SubscriptionOwnership(
    val isAutoRenewing: Boolean,
)

data class Ownership(
    val hasIap: Boolean,
    val subscription: SubscriptionOwnership?,
) {
    val hasSubscription: Boolean get() = subscription != null
}

data class GraceHint(
    // false = calm "waiting for Google Play to re-confirm"; true = escalate to diagnostics + restore.
    val showDiagnostics: Boolean,
)

enum class SubscriptionAction { TRIAL, STANDARD, UNAVAILABLE }

// The single money-path guard: restore, switch-verification, and launch are mutually exclusive.
enum class ActionState { IDLE, RESTORING, VERIFYING }

data class Pricing(
    val iap: SkuDetails? = null,
    val sub: SkuDetails? = null,
    val subPrice: String? = null,
    val iapPrice: String? = null,
    val hasTrialOffer: Boolean = false,
) {
    val subAvailable: Boolean get() = sub != null || subPrice != null
    val iapAvailable: Boolean get() = iap != null || iapPrice != null
    val subscriptionAction: SubscriptionAction
        get() = when {
            !subAvailable -> SubscriptionAction.UNAVAILABLE
            hasTrialOffer -> SubscriptionAction.TRIAL
            else -> SubscriptionAction.STANDARD
        }
}

sealed interface UpgradeUiState {
    data object Loading : UpgradeUiState

    data class Loaded(
        val manageMode: Boolean,
        val isPro: Boolean,
        val gracePeriod: Boolean,
        val ownership: Ownership,
        val graceHint: GraceHint?,
        val pricing: Pricing,
        // Whether billing has completed its first fresh reconciliation. Purchase/switch/restore
        // actions stay disabled until settled so a cold-start empty snapshot can't offer acquisition
        // to an existing owner (double-buy protection).
        val isSettled: Boolean,
        val action: ActionState,
        val wasPreviouslyPro: Boolean,
    ) : UpgradeUiState {
        val restoreInProgress: Boolean get() = action == ActionState.RESTORING
        val verificationInProgress: Boolean get() = action == ActionState.VERIFYING
        val actionBusy: Boolean get() = action != ActionState.IDLE

        // Show the ownership / status surface (congrats hero + switch) rather than the sales pitch.
        val showOwnership: Boolean get() = isPro

        // The one-time switch offer is unlocked only when a subscription is owned AND not renewing.
        val switchUnlocked: Boolean
            get() = ownership.subscription?.let { !it.isAutoRenewing } ?: false
    }
}

// Conservative: a subscription counts as renewing if ANY owned record for it is auto-renewing — this
// can only under-offer the switch, never wrongly enable it (which could permit double billing).
fun UpgradeRepoGplay.Info.toOwnership(): Ownership {
    val subs = proPurchases.filter { it.sku.type == Sku.Type.SUBSCRIPTION }
    return Ownership(
        hasIap = proPurchases.any { it.sku.type == Sku.Type.IAP },
        subscription = subs.takeIf { it.isNotEmpty() }?.let { list ->
            SubscriptionOwnership(isAutoRenewing = list.any { it.purchase.isAutoRenewing })
        },
    )
}

// Pure builder for the loaded state so the grace mapping and the 24h diagnostics boundary are
// unit-testable without the ViewModel. `lastProConfirmedAt`/`now` are wall-clock millis.
fun toLoadedState(
    manageMode: Boolean,
    info: UpgradeRepoGplay.Info,
    pricing: Pricing,
    lastProConfirmedAt: Long,
    now: Long,
    isSettled: Boolean,
    action: ActionState,
    wasEverPro: Boolean,
    graceDiagnosticsAfterMs: Long,
): UpgradeUiState.Loaded {
    val inGrace = info.gracePeriod && info.proPurchases.isEmpty()
    val graceHint = if (inGrace) {
        val agedOut = lastProConfirmedAt > 0 && (now - lastProConfirmedAt) >= graceDiagnosticsAfterMs
        GraceHint(showDiagnostics = agedOut)
    } else {
        null
    }
    return UpgradeUiState.Loaded(
        manageMode = manageMode,
        isPro = info.isPro,
        gracePeriod = inGrace,
        ownership = info.toOwnership(),
        graceHint = graceHint,
        pricing = pricing,
        isSettled = isSettled,
        action = action,
        wasPreviouslyPro = wasEverPro && !info.isPro,
    )
}
