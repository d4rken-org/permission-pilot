package eu.darken.myperm.apps.ui.details.items

import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.QueriesInfo
import eu.darken.myperm.apps.core.queries.ManifestParser
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.DividerItemDecorator2
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsQueriesItemBinding

class AppQueriesVH(parent: ViewGroup) : AppDetailsAdapter.BaseVH<AppQueriesVH.Item, AppsDetailsQueriesItemBinding>(
    R.layout.apps_details_queries_item,
    parent
), BindableVH<AppQueriesVH.Item, AppsDetailsQueriesItemBinding>, DividerItemDecorator2.SkipDivider {

    override val viewBinding = lazy { AppsDetailsQueriesItemBinding.bind(itemView) }

    override val onBindData: AppsDetailsQueriesItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        when (val state = item.state) {
            is State.Loading -> {
                loadingIndicator.isVisible = true
                subtitle.text = getString(R.string.apps_details_queries_loading)
                details.isGone = true
            }

            is State.Data -> {
                loadingIndicator.isGone = true
                val info = state.queriesInfo
                if (info.isEmpty) {
                    subtitle.text = getString(R.string.apps_details_queries_none)
                    details.isGone = true
                } else {
                    subtitle.text = buildSummary(info)
                    details.apply {
                        text = buildDetails(info)
                        isGone = text.isNullOrEmpty()
                    }
                }
            }

            is State.Unsupported -> {
                loadingIndicator.isGone = true
                subtitle.text = getString(R.string.apps_details_queries_unavailable)
                details.isGone = true
            }

            is State.Error -> {
                loadingIndicator.isGone = true
                subtitle.text = getString(R.string.apps_details_queries_error)
                details.isGone = true
            }
        }
    }

    private fun AppsDetailsQueriesItemBinding.buildSummary(info: QueriesInfo): String {
        val parts = mutableListOf<String>()
        if (info.packageQueries.isNotEmpty()) {
            parts.add(getQuantityString(R.plurals.apps_details_queries_packages, info.packageQueries.size, info.packageQueries.size))
        }
        if (info.intentQueries.isNotEmpty()) {
            parts.add(getQuantityString(R.plurals.apps_details_queries_intents, info.intentQueries.size, info.intentQueries.size))
        }
        if (info.providerQueries.isNotEmpty()) {
            parts.add(getQuantityString(R.plurals.apps_details_queries_providers, info.providerQueries.size, info.providerQueries.size))
        }
        return parts.joinToString(", ")
    }

    private fun AppsDetailsQueriesItemBinding.buildDetails(info: QueriesInfo): String {
        val lines = mutableListOf<String>()
        info.packageQueries.forEach { lines.add(it) }
        info.intentQueries.forEach { query ->
            val parts = (query.actions + query.dataSpecs + query.categories).joinToString(" | ")
            lines.add(parts.ifEmpty { getString(R.string.apps_details_queries_intent_fallback) })
        }
        info.providerQueries.forEach { lines.add(getString(R.string.apps_details_queries_provider_prefix, it)) }
        return lines.joinToString("\n")
    }

    sealed class State {
        data object Loading : State()
        data class Data(val queriesInfo: QueriesInfo) : State()
        data class Unsupported(val reason: String) : State()
        data class Error(val error: Throwable) : State()

        companion object {
            fun from(result: ManifestParser.QueriesResult): State = when (result) {
                is ManifestParser.QueriesResult.Success -> Data(result.queriesInfo)
                is ManifestParser.QueriesResult.Unavailable -> Unsupported(result.reason)
                is ManifestParser.QueriesResult.ParseError -> Error(result.error)
            }
        }
    }

    data class Item(
        val state: State,
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = Item::class.hashCode().toLong()
    }
}
