package eu.darken.myperm.common.upgrade.core

import android.app.Activity
import android.os.SystemClock
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.common.upgrade.core.client.BillingException
import eu.darken.myperm.common.upgrade.core.data.BillingData
import eu.darken.myperm.common.upgrade.core.data.BillingDataRepo
import eu.darken.myperm.common.upgrade.core.data.PurchasedSku
import eu.darken.myperm.common.upgrade.core.data.Sku
import eu.darken.myperm.common.upgrade.core.data.SkuDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpgradeRepoGplay @Inject constructor(
    @AppScope private val scope: CoroutineScope,
    private val billingDataRepo: BillingDataRepo,
    private val billingCache: BillingCache,
) : UpgradeRepo {

    private var lastProStateAt: Long
        get() = billingCache.lastProStateAt.valueBlocking
        set(value) {
            billingCache.lastProStateAt.valueBlocking = value
        }

    private val lastProStateSku: String
        get() = billingCache.lastProStateSku.valueBlocking

    // Monotonic (elapsedRealtime) time of the last observed billing error, null when billing is
    // healthy. While an error is recent, an empty purchase result can't be trusted as "not owned"
    // either, so the longer error grace window applies to empty data too — this keeps the reactive
    // pipeline and explicit restore consistent regardless of which emission is processed last.
    // Retired when a refresh completes authoritatively.
    @Volatile private var lastBillingErrorAtElapsed: Long? = null

    // Outcomes of explicit restore calls; merged into upgradeInfo so the canonical state always
    // reflects what an explicit restore concluded (the reactive pipeline may have evaluated
    // different data, e.g. the error-window grace only applies after a billing error).
    // replay=1: an outcome must survive until the eagerly started stateIn collector attaches.
    private val manualInfo = MutableSharedFlow<Info>(replay = 1)

    override val upgradeInfo: StateFlow<UpgradeRepo.Info> = merge(
        billingDataRepo.billingData
            .map { data -> data.toUpgradeInfo() }
            .retryWhen { cause, _ ->
                if (cause !is BillingException && cause !is IOException) return@retryWhen false
                val now = System.currentTimeMillis()
                lastBillingErrorAtElapsed = SystemClock.elapsedRealtime()
                log(TAG) { "now=$now, lastProStateAt=$lastProStateAt, error=$cause" }
                if (proStateAge(now) < graceWindowErrorMs()) {
                    log(TAG, VERBOSE) { "Grace period active after billing error, retrying..." }
                    emit(Info(gracePeriod = true, billingData = null))
                }
                delay(RETRY_DELAY_MS)
                true
            },
        manualInfo,
    ).stateIn(scope, SharingStarted.Eagerly, cachedInfo())

    // True once this install has ever confirmed a known Pro purchase; drives the proactive restore
    // banner. Local signal only — a fresh install or a switched Google account starts false.
    val wasEverPro: Flow<Boolean> = billingCache.lastProStateAt.flow.map { it > 0 }

    // Asynchronous purchase-flow failures (already user-friendly-mapped).
    val purchaseFailures: Flow<Throwable> = billingDataRepo.purchaseFailures

    suspend fun querySkus(): Collection<SkuDetails> {
        return billingDataRepo.querySkus(*MyPermSku.PRO_SKUS.toTypedArray())
    }

    suspend fun launchBillingFlow(
        activity: Activity,
        skuDetails: SkuDetails,
        offer: Sku.Subscription.Offer? = null,
    ) {
        billingDataRepo.launchBillingFlow(activity, skuDetails, offer)
    }

    override suspend fun refresh() {
        log(TAG) { "refresh()" }
        try {
            // Same evaluate-and-publish path as an explicit restore, so the authoritative result
            // always reaches upgradeInfo; only the error handling differs (swallow-and-log).
            restorePurchaseNow()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(TAG, WARN) { "Background refresh failed: ${e.asLog()}" }
        }
    }

    // Explicit "Restore purchase": query Play now and evaluate Pro from the returned data in the
    // same coroutine (real happens-before), so we never read a stale upgradeInfo value. Billing
    // errors propagate so the caller can distinguish "not owned" from "Play unavailable".
    suspend fun restorePurchaseNow(): Info {
        log(TAG) { "restorePurchaseNow()" }
        val outcome = try {
            val data = billingDataRepo.refresh()
            // Play answered authoritatively — retire the error-window override.
            lastBillingErrorAtElapsed = null
            data.toUpgradeInfo()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Mirror the reactive flow's retryWhen: a billing error while we were Pro recently
            // keeps us Pro via the grace period, otherwise the error surfaces to the caller.
            val now = System.currentTimeMillis()
            lastBillingErrorAtElapsed = SystemClock.elapsedRealtime()
            if (proStateAge(now) < graceWindowErrorMs()) {
                log(TAG, VERBOSE) { "Restore hit a billing error, but we were Pro recently -> grace" }
                Info(gracePeriod = true, billingData = null)
            } else {
                throw e
            }
        }
        manualInfo.emit(outcome)
        return outcome
    }

    // Shared Pro/grace mapping used by both the reactive upgradeInfo flow and restorePurchaseNow().
    // Only relinquishes Pro if we haven't had it for a while (grace period).
    private suspend fun BillingData.toUpgradeInfo(): Info {
        val now = System.currentTimeMillis()
        val proSkus = getProSkus()
        log(TAG) { "toUpgradeInfo(): now=$now, lastProStateAt=$lastProStateAt, data=$this" }
        return when {
            proSkus.isNotEmpty() -> {
                // Only a *known* Pro SKU refreshes the grace state. Prefer the permanent IAP so
                // its longer grace window applies when both product types are owned.
                billingCache.confirmPro(at = now, sku = preferredProSku(proSkus)?.sku?.id.orEmpty())
                Info(billingData = this)
            }

            proStateAge(now) < graceWindowNormalMs() -> {
                log(TAG, VERBOSE) { "We are not pro, but were recently, did GPlay try annoy us again?" }
                Info(gracePeriod = true, billingData = null)
            }

            else -> {
                Info(billingData = this)
            }
        }
    }

    // Age of the last confirmed Pro state. A timestamp in the future (clock moved backwards) is
    // reset to now, so wall-clock changes can't create an indefinite grace period.
    private fun proStateAge(now: Long): Long {
        val lastAt = lastProStateAt
        if (lastAt > now) {
            log(TAG, WARN) { "lastProStateAt=$lastAt is in the future (now=$now), resetting." }
            lastProStateAt = now
            return 0L
        }
        return now - lastAt
    }

    // Grace window depends on what was last owned: a permanent one-time purchase should almost
    // never be dropped on a Play hiccup, a subscription (or unknown/legacy SKU) legitimately lapses.
    // Empty data is evaluated with the error window while a billing error is recent — an empty
    // result during billing trouble isn't trustworthy evidence of "not owned".
    private fun graceWindowNormalMs(): Long = when {
        isLastProSkuIap() -> GRACE_PERIOD_IAP_MS
        hasRecentBillingError() -> GRACE_PERIOD_ERROR_MS
        else -> GRACE_PERIOD_NORMAL_MS
    }

    private fun hasRecentBillingError(): Boolean = lastBillingErrorAtElapsed
        ?.let { (SystemClock.elapsedRealtime() - it) < GRACE_PERIOD_ERROR_MS } == true

    private fun graceWindowErrorMs(): Long =
        if (isLastProSkuIap()) GRACE_PERIOD_IAP_MS else GRACE_PERIOD_ERROR_MS

    private fun isLastProSkuIap(): Boolean =
        MyPermSku.PRO_SKUS.singleOrNull { it.id == lastProStateSku }?.type == Sku.Type.IAP

    data class Info(
        private val gracePeriod: Boolean = false,
        private val billingData: BillingData?,
    ) : UpgradeRepo.Info {

        override val type: UpgradeRepo.Type
            get() = UpgradeRepo.Type.GPLAY

        override val isPro: Boolean
            get() = billingData?.getProSku() != null || gracePeriod

        override val upgradedAt: Instant?
            get() = billingData
                ?.getProSku()
                ?.purchase?.purchaseTime
                ?.let { Instant.ofEpochMilli(it) }
    }

    private fun cachedInfo(): Info {
        val now = System.currentTimeMillis()
        val inGracePeriod = lastProStateAt > 0 && proStateAge(now) < graceWindowErrorMs()
        log(TAG) { "cachedInfo(): now=$now, lastProStateAt=$lastProStateAt, inGracePeriod=$inGracePeriod" }
        return Info(gracePeriod = inGracePeriod, billingData = null)
    }

    companion object {
        internal const val GRACE_PERIOD_NORMAL_MS = 6 * 60 * 60 * 1000L   // 6h - GPlay transient unavailability
        internal const val GRACE_PERIOD_ERROR_MS = 24 * 60 * 60 * 1000L   // 24h - billing errors / cold start
        internal const val GRACE_PERIOD_IAP_MS = 30 * 24 * 60 * 60 * 1000L // 30d - permanent one-time purchase
        private const val RETRY_DELAY_MS = 60_000L                         // 1min before re-subscribing

        private fun BillingData.getProSku(): PurchasedSku? = purchasedSkus
            .firstOrNull { it.sku in MyPermSku.PRO_SKUS }

        private fun BillingData.getProSkus(): Collection<PurchasedSku> = purchasedSkus
            .filter { it.sku in MyPermSku.PRO_SKUS }

        // The SKU whose grace window applies when several are owned: the permanent one-time
        // purchase wins over a subscription. null when no known Pro SKU is owned.
        internal fun preferredProSku(upgrades: Collection<PurchasedSku>): PurchasedSku? =
            upgrades.firstOrNull { it.sku.type == Sku.Type.IAP } ?: upgrades.firstOrNull()

        val TAG: String = logTag("Upgrade", "Gplay", "Control")
    }
}
