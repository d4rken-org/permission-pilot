package eu.darken.myperm.apps.core.manifest

data class ManifestData(
    val rawXml: RawXmlResult,
    val queries: QueriesResult,
)

sealed class RawXmlResult {
    data class Success(val xml: String) : RawXmlResult()
    data class Unavailable(val reason: UnavailableReason) : RawXmlResult()
    data class Error(val error: Throwable) : RawXmlResult()
}

sealed class QueriesResult {
    data class Success(val info: QueriesInfo) : QueriesResult()
    data class Error(val error: Throwable) : QueriesResult()
}

enum class UnavailableReason { APK_NOT_FOUND, APK_NOT_READABLE, PKG_NOT_FOUND }
