package eu.darken.myperm.common.upgrade.core

import android.app.Activity
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.common.upgrade.core.client.BillingException
import eu.darken.myperm.common.upgrade.core.data.BillingData
import eu.darken.myperm.common.upgrade.core.data.BillingDataRepo
import eu.darken.myperm.common.upgrade.core.data.PurchasedSku
import eu.darken.myperm.common.upgrade.core.data.Sku
import eu.darken.myperm.common.upgrade.core.data.SkuDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    override val upgradeInfo: StateFlow<UpgradeRepo.Info> = billingDataRepo.billingData
        .map { data ->
            val now = System.currentTimeMillis()
            val proSku = data.getProSku()
            log(TAG) { "now=$now, lastProStateAt=$lastProStateAt, data=${data}" }
            when {
                proSku != null -> {
                    lastProStateAt = now
                    Info(billingData = data)
                }

                (now - lastProStateAt) < GRACE_PERIOD_NORMAL_MS -> {
                    log(TAG, VERBOSE) { "We are not pro, but were recently, did GPlay try annoy us again?" }
                    Info(gracePeriod = true, billingData = null)
                }

                else -> {
                    Info(billingData = data)
                }
            }
        }
        .retryWhen { cause, _ ->
            if (cause !is BillingException && cause !is IOException) return@retryWhen false
            val now = System.currentTimeMillis()
            log(TAG) { "now=$now, lastProStateAt=$lastProStateAt, error=$cause" }
            if ((now - lastProStateAt) < GRACE_PERIOD_ERROR_MS) {
                log(TAG, VERBOSE) { "Grace period active after billing error, retrying..." }
                emit(Info(gracePeriod = true, billingData = null))
            }
            delay(RETRY_DELAY_MS)
            true
        }
        .stateIn(scope, SharingStarted.Eagerly, cachedInfo())

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
        billingDataRepo.refresh()
    }

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
        val inGracePeriod = lastProStateAt > 0 && (now - lastProStateAt) < GRACE_PERIOD_ERROR_MS
        log(TAG) { "cachedInfo(): now=$now, lastProStateAt=$lastProStateAt, inGracePeriod=$inGracePeriod" }
        return Info(gracePeriod = inGracePeriod, billingData = null)
    }

    companion object {
        private const val GRACE_PERIOD_NORMAL_MS = 6 * 60 * 60 * 1000L   // 6h - GPlay transient unavailability
        private const val GRACE_PERIOD_ERROR_MS = 24 * 60 * 60 * 1000L    // 24h - billing errors / cold start
        private const val RETRY_DELAY_MS = 60_000L                         // 1min before re-subscribing

        private fun BillingData.getProSku(): PurchasedSku? = purchasedSkus
            .firstOrNull { it.sku in MyPermSku.PRO_SKUS }

        val TAG: String = logTag("Upgrade", "Gplay", "Control")
    }
}
