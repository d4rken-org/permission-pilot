package eu.darken.myperm.apps.core.queries

import eu.darken.myperm.apps.core.features.QueriesInfo

interface ManifestParser {
    fun parseQueries(apkPath: String): QueriesResult

    sealed class QueriesResult {
        data class Success(val queriesInfo: QueriesInfo) : QueriesResult()
        data class Unavailable(val reason: String) : QueriesResult()
        data class ParseError(val error: Throwable) : QueriesResult()
    }
}
