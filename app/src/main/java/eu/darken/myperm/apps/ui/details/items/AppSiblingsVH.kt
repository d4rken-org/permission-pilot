package eu.darken.myperm.apps.ui.details.items

import android.view.ViewGroup
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsSiblingsItemBinding

class AppSiblingsVH(parent: ViewGroup) : AppDetailsAdapter.BaseVH<AppSiblingsVH.Item, AppsDetailsSiblingsItemBinding>(
    R.layout.apps_details_siblings_item,
    parent
), BindableVH<AppSiblingsVH.Item, AppsDetailsSiblingsItemBinding> {

    override val viewBinding = lazy { AppsDetailsSiblingsItemBinding.bind(itemView) }

    override val onBindData: AppsDetailsSiblingsItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val app = item.app

        val idLabel = app.packageInfo.applicationInfo?.let { appInfo ->
            context.packageManager.getText(
                app.packageName,
                app.packageInfo.sharedUserLabel,
                appInfo
            )
        }?.toString()

        shareduseridInfo.text = if (idLabel != null) {
            "$idLabel (${app.sharedUserId})"
        } else {
            app.sharedUserId
        }

        siblingsInfo.text = app.siblings.joinToString("\n") {
            "${it.label ?: "?"} (${it.id})"
        }
    }

    data class Item(
        val app: NormalApp
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = Item::class.hashCode().toLong()
    }

}