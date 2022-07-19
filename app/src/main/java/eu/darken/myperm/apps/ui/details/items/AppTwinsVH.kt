package eu.darken.myperm.apps.ui.details.items

import android.view.ViewGroup
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.container.NormalApp
import eu.darken.myperm.apps.core.features.ApkPkg
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.DividerItemDecorator2
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsTwinsItemBinding
import eu.darken.myperm.databinding.AppsDetailsTwinsItemTwinBinding

class AppTwinsVH(parent: ViewGroup) : AppDetailsAdapter.BaseVH<AppTwinsVH.Item, AppsDetailsTwinsItemBinding>(
    R.layout.apps_details_twins_item,
    parent
), BindableVH<AppTwinsVH.Item, AppsDetailsTwinsItemBinding>, DividerItemDecorator2.SkipDivider {

    override val viewBinding = lazy { AppsDetailsTwinsItemBinding.bind(itemView) }

    override val onBindData: AppsDetailsTwinsItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val app = item.app

        twinsContainer.removeAllViews()

        val pm = context.packageManager

        app.twins.forEach { twin ->
            AppsDetailsTwinsItemTwinBinding.inflate(layoutInflater).apply {
                icon.setImageDrawable(pm.getUserBadgedIcon(twin.getIcon(context), twin.userHandle))

                label.text = pm.getUserBadgedLabel(twin.getLabel(context), twin.userHandle)
                identifier.text = twin.id.toString()

                twinsContainer.addView(this.root)
            }
        }
    }

    data class Item(
        val app: NormalApp,
        val onTwinClicked: (ApkPkg) -> Unit,
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = Item::class.hashCode().toLong()
    }

}