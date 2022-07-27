package eu.darken.myperm.apps.ui.details.items

import android.view.ViewGroup
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.container.BasePkg
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
        twinCount.text = app.twins.size.toString()
        app.twins.forEach { twin ->
            AppsDetailsTwinsItemTwinBinding.inflate(layoutInflater).apply {
                twin.getIcon(context)?.let { icon.setImageDrawable(it) }

                label.text = twin.getLabel(context)
                identifier.text = twin.id.toString()
                this.root.setOnClickListener { item.onTwinClicked(twin) }

                twinsContainer.addView(this.root)
            }
        }
    }

    data class Item(
        val app: BasePkg,
        val onTwinClicked: (Pkg) -> Unit,
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = Item::class.hashCode().toLong()
    }

}