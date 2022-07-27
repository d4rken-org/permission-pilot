package eu.darken.myperm.main.ui.overview.items

import android.view.ViewGroup
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.OverviewItemSummaryBinding
import eu.darken.myperm.main.ui.overview.OverviewAdapter

class SummaryVH(parent: ViewGroup) : OverviewAdapter.BaseVH<SummaryVH.Item, OverviewItemSummaryBinding>(
    R.layout.overview_item_summary,
    parent
), BindableVH<SummaryVH.Item, OverviewItemSummaryBinding> {

    override val viewBinding = lazy { OverviewItemSummaryBinding.bind(itemView) }

    override val onBindData: OverviewItemSummaryBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->

    }

    class Item : OverviewAdapter.Item {
        override val stableId: Long
            get() = Item::class.java.hashCode().toLong()
    }
}