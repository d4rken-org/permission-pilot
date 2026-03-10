package eu.darken.myperm.common.upgrade.core

import eu.darken.myperm.common.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class FossUpgrade(
    @Serializable(with = InstantSerializer::class) val upgradedAt: Instant,
    val reason: Reason
) {
    @Serializable
    enum class Reason {
        @SerialName("foss.upgrade.reason.donated") DONATED,
        @SerialName("foss.upgrade.reason.alreadydonated") ALREADY_DONATED,
        @SerialName("foss.upgrade.reason.nomoney") NO_MONEY;
    }
}
