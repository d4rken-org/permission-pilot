package eu.darken.myperm.apps.core.manifest

data class ManifestData(
    val rawXml: RawXmlResult,
    val queries: QueriesOutcome,
)

sealed class RawXmlResult {
    data class Success(val xml: String) : RawXmlResult()
    data class Unavailable(val reason: UnavailableReason) : RawXmlResult()
    data class Error(val error: Throwable) : RawXmlResult()
}

/**
 * Outcome of resolving the `<queries>` projection — used at every layer (reader, cache,
 * repo, scanner). Failure is split into [Unavailable] (the APK or its manifest could not be
 * read at all) and [Failure] (read fine but parsing/extraction failed).
 *
 * Cacheability decisions key off [UnavailableReason.isTransient] so policy lives on the
 * data, not in the consumers.
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
    MALFORMED_APK;

    /**
     * True when the underlying condition is expected to clear without app changes
     * (currently: only [LOW_MEMORY]). Callers decide their own policy: [ManifestRepo]
     * skips the memory cache; [ManifestHintRepo] preserves stale hints rather than
     * deleting them.
     */
    val isTransient: Boolean get() = this == LOW_MEMORY
}
