package eu.darken.myperm.apps.core.manifest

import kotlinx.serialization.Serializable

@Serializable
enum class SectionType {
    USES_PERMISSION,
    PERMISSION,
    QUERIES,
    ACTIVITIES,
    SERVICES,
    RECEIVERS,
    PROVIDERS,
    META_DATA,
    OTHER,
}

/**
 * Cacheable per-section payload (no [ManifestSection.isFlagged] — flagging keys off live
 * [ManifestHintScanner] thresholds and is recomputed on read).
 */
@Serializable
internal data class CachedManifestSection(
    val type: SectionType,
    val elementCount: Int,
    val prettyXml: String,
)

data class ManifestSection(
    val type: SectionType,
    val elementCount: Int,
    val prettyXml: String,
    val isFlagged: Boolean,
)

internal fun CachedManifestSection.toUiModel(isFlagged: Boolean) = ManifestSection(
    type = type,
    elementCount = elementCount,
    prettyXml = prettyXml,
    isFlagged = isFlagged,
)

internal fun ManifestSection.toCacheModel() = CachedManifestSection(
    type = type,
    elementCount = elementCount,
    prettyXml = prettyXml,
)
