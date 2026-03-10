package eu.darken.myperm.common.upgrade

import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

interface UpgradeRepo {
    val upgradeInfo: StateFlow<Info>

    suspend fun refresh()

    interface Info {
        val type: Type

        val isPro: Boolean

        val upgradedAt: Instant?
    }

    enum class Type {
        GPLAY,
        FOSS
    }
}
