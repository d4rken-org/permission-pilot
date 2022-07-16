package eu.darken.myperm.apps.ui.details.items

import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsOverviewItemBinding

class AppOverviewVH(parent: ViewGroup) : AppDetailsAdapter.BaseVH<AppOverviewVH.Item, AppsDetailsOverviewItemBinding>(
    R.layout.apps_details_overview_item,
    parent
), BindableVH<AppOverviewVH.Item, AppsDetailsOverviewItemBinding> {

    override val viewBinding = lazy { AppsDetailsOverviewItemBinding.bind(itemView) }

    override val onBindData: AppsDetailsOverviewItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val app = item.app

        identifier.text = app.id.toString()
        label.apply {
            text = app.label
            isGone = app.label.isNullOrEmpty()
        }

        description.apply {
            val countTotal = app.requestedPermissions.size
            val grantedCount = app.requestedPermissions.count { it.isGranted }
            text = "$grantedCount of $countTotal permissions granted."
        }

        tagSystem.isInvisible = !app.isSystemApp
        tagContainer.isGone = tagContainer.children.all { !it.isVisible }
    }

    data class Item(
        val app: NormalApp
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = Item::class.hashCode().toLong()
    }

}