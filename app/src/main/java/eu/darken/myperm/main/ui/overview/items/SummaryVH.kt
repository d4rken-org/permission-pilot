package eu.darken.myperm.main.ui.overview.items

import android.view.ViewGroup
import androidx.core.view.isGone
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.OverviewItemSummaryBinding
import eu.darken.myperm.main.ui.overview.OverviewAdapter
import eu.darken.myperm.main.ui.overview.PkgCount

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
        contentContainer.isGone = item is Item.Loading
        if (item !is Item.Content) return@binder

        pkgsInProfile.text = item.pkgCountActiveProfile.getHR(context)
        pkgsInOtherProfiles.text = item.pkgCountOtherProfile.getHR(context)
        pkgsSideloaded.text = item.pkgCountSideloaded.getHR(context)
        pkgsInstallers.text = item.pkgCountInstallerApps.getHR(context)
        pkgsOverlayers.text = item.pkgCountSystemAlertWindow.getHR(context)
        pkgsOffline.text = item.pkgCountNoInternet.getHR(context)
        pkgsClones.text = item.pkgCountClones.getHR(context)
        pkgsSharedids.text = item.pkgCountSharedIds.getHR(context)
    }

    sealed class Item : OverviewAdapter.Item {
        override val stableId: Long = Item::class.java.hashCode().toLong()

        object Loading : Item()

        data class Content(
            val pkgCountActiveProfile: PkgCount,
            val pkgCountOtherProfile: PkgCount,
            val pkgCountSideloaded: PkgCount,
            val pkgCountInstallerApps: PkgCount,
            val pkgCountSystemAlertWindow: PkgCount,
            val pkgCountNoInternet: PkgCount,
            val pkgCountClones: PkgCount,
            val pkgCountSharedIds: PkgCount,
        ) : Item()
    }
}