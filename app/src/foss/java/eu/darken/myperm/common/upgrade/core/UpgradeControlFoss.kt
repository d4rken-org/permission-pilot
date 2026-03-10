package eu.darken.myperm.common.upgrade.core

import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.upgrade.UpgradeRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpgradeControlFoss @Inject constructor(
    @AppScope private val scope: CoroutineScope,
    private val fossCache: FossCache,
) : UpgradeRepo {

    override val upgradeInfo: StateFlow<UpgradeRepo.Info> = fossCache.upgrade.flow.map { data ->
        if (data == null) {
            Info()
        } else {
            Info(
                isPro = true,
                upgradedAt = data.upgradedAt,
                upgradeReason = data.reason
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, cachedInfo())

    private fun cachedInfo(): Info {
        val data = fossCache.upgrade.valueBlocking
        return if (data != null) Info(isPro = true, upgradedAt = data.upgradedAt, upgradeReason = data.reason) else Info()
    }

    fun upgrade(reason: FossUpgrade.Reason) {
        fossCache.upgrade.valueBlocking = FossUpgrade(
            upgradedAt = Instant.now(),
            reason = reason,
        )
    }

    override suspend fun refresh() {
        // No-op for FOSS - upgrade state is local
    }

    data class Info(
        override val isPro: Boolean = false,
        override val upgradedAt: Instant? = null,
        val upgradeReason: FossUpgrade.Reason? = null,
    ) : UpgradeRepo.Info {
        override val type: UpgradeRepo.Type = UpgradeRepo.Type.FOSS
    }
}
