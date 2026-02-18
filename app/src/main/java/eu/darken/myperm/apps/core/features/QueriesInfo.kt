package eu.darken.myperm.apps.core.features

data class QueriesInfo(
    val packageQueries: List<String> = emptyList(),
    val intentQueries: List<IntentQuery> = emptyList(),
    val providerQueries: List<String> = emptyList(),
) {
    val totalCount: Int
        get() = packageQueries.size + intentQueries.size + providerQueries.size

    val isEmpty: Boolean
        get() = totalCount == 0

    data class IntentQuery(
        val actions: List<String> = emptyList(),
        val dataSpecs: List<String> = emptyList(),
        val categories: List<String> = emptyList(),
    )
}
