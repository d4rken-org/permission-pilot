package eu.darken.myperm.apps.core.manifest

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

data class ManifestSection(
    val type: SectionType,
    val elementCount: Int,
    val prettyXml: String,
    val isFlagged: Boolean,
)
