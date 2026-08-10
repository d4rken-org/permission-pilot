package eu.darken.myperm.apps.core.manifest

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManifestHintScanner @Inject constructor() {

    data class Flags(
        val hasActionMainQuery: Boolean,
        val packageQueryCount: Int,
        val intentQueryCount: Int,
        val providerQueryCount: Int,
    )

    fun evaluate(queriesInfo: QueriesInfo): Flags = Flags(
        hasActionMainQuery = queriesInfo.intentQueries.any { it.isBroadMainQuery() },
        packageQueryCount = queriesInfo.packageQueries.size,
        intentQueryCount = queriesInfo.intentQueries.size,
        providerQueryCount = queriesInfo.providerQueries.size,
    )

    /**
     * AppsFilter matches a `<queries><intent>` against target filters with standard intent
     * resolution: every category on the query must be declared by the target filter, and a query
     * carrying data can't match the data-less launcher filters. So MAIN plus a data constraint,
     * or MAIN plus any category other than LAUNCHER (launcher filters declare only MAIN+LAUNCHER),
     * reaches a narrow app set and is not broad discovery. A wildcard action ("*") subsumes MAIN.
     *
     * Only scheme and mimeType actually constrain: the platform builds the query intent's data
     * URI only when a scheme or mimeType is present, so a host without a scheme is dropped and
     * the query stays broad.
     */
    private fun QueriesInfo.IntentQuery.isBroadMainQuery(): Boolean {
        if (dataSpecs.any { it.contains("scheme=") || it.contains("mimeType=") }) return false
        if (actions.none { it == ACTION_MAIN || it == ACTION_WILDCARD }) return false
        return categories.isEmpty() || categories.toSet() == setOf(CATEGORY_LAUNCHER)
    }

    companion object {
        private const val ACTION_MAIN = "android.intent.action.MAIN"
        private const val ACTION_WILDCARD = "*"
        private const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"
        const val EXCESSIVE_THRESHOLD = 10

        // Bump when evaluate() changes semantics so cached hints from older logic get re-scanned.
        const val SCANNER_VERSION = 1

        fun hasExcessiveQueries(entity: ManifestHintEntity): Boolean =
            entity.packageQueryCount > EXCESSIVE_THRESHOLD

        fun hasFlaggedIssues(entity: ManifestHintEntity): Boolean =
            entity.hasActionMainQuery || hasExcessiveQueries(entity)
    }
}
