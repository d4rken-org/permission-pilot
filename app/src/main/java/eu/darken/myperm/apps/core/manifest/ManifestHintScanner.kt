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

    fun evaluate(queriesInfo: QueriesInfo): Flags {
        val hasActionMain = queriesInfo.intentQueries.any { intent ->
            intent.actions.any { it == ACTION_MAIN }
        }
        return Flags(
            hasActionMainQuery = hasActionMain,
            packageQueryCount = queriesInfo.packageQueries.size,
            intentQueryCount = queriesInfo.intentQueries.size,
            providerQueryCount = queriesInfo.providerQueries.size,
        )
    }

    companion object {
        private const val ACTION_MAIN = "android.intent.action.MAIN"
        const val EXCESSIVE_THRESHOLD = 10

        fun hasExcessiveQueries(entity: ManifestHintEntity): Boolean =
            entity.packageQueryCount > EXCESSIVE_THRESHOLD

        fun hasFlaggedIssues(entity: ManifestHintEntity): Boolean =
            entity.hasActionMainQuery || hasExcessiveQueries(entity)
    }
}
