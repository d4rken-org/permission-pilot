package eu.darken.myperm.main.ui.overview.items

import android.view.ViewGroup
import androidx.core.view.isGone
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
    ) -> Unit = binder@{ item, _ ->
        loadingContainer.isGone = item !is Item.Loading
        if (item !is Item.Content) return@binder

        pkgsInProfile.text = getString(
            R.string.overview_summary_apps_active_profile,
            item.pkgCountActiveProfileUser + item.pkgCountActiveProfileSystem,
            item.pkgCountActiveProfileUser, item.pkgCountActiveProfileSystem
        )
        pkgsInOtherProfiles.text = getString(
            R.string.overview_summary_apps_other_profile,
            item.pkgCountOtherProfileUser + item.pkgCountOtherProfileSystem,
            item.pkgCountOtherProfileUser, item.pkgCountOtherProfileSystem
        )
        pkgsSideloaded.text = getString(
            R.string.overview_summary_apps_sideloaded, item.pkgCountSideloaded
        )
    }

    sealed class Item : OverviewAdapter.Item {
        override val stableId: Long
            get() = Item::class.java.hashCode().toLong()

        object Loading : Item()

//        No. of apps with 'Install from Unknown Sources' enabled*: Z (x/y)
//        No. of apps with 'Accessibility' permissions*: Z (x/y)
//        No. of apps that can 'Appear on Top'*: Z (x/y)
//        No. of apps with 'Device Admin' permissions*: Z (x/y)
//        No. of apps that have 'No Internet' permissions**: Z (x/y)
//        No. of apps with clones in other profiles: Z (x/y)
//        No. of apps with SharedUserID: Z (x/y)

        data class Content(
            val pkgCountActiveProfileUser: Int,
            val pkgCountActiveProfileSystem: Int,
            val pkgCountOtherProfileUser: Int,
            val pkgCountOtherProfileSystem: Int,
            val pkgCountSideloaded: Int,
        ) : Item()
    }
}