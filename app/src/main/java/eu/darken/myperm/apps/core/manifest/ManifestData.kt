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

/**
 * Narrow outcome for callers that only need the `<queries>` projection and must not retain
 * the raw manifest XML. Keeps the memory-cache footprint bounded and lets us distinguish
 * transient failures (LOW_MEMORY, generic Failure) from stable ones (APK_NOT_FOUND,
 * MALFORMED_APK, PKG_NOT_FOUND) that can be cached.
 */
sealed interface QueriesOutcome {
    data class Success(val info: QueriesInfo) : QueriesOutcome
    data class Unavailable(val reason: UnavailableReason) : QueriesOutcome
    data class Failure(val error: Throwable) : QueriesOutcome
}

enum class UnavailableReason {
    APK_NOT_FOUND,
    APK_NOT_READABLE,
    PKG_NOT_FOUND,
    LOW_MEMORY,
    MALFORMED_APK,
}
