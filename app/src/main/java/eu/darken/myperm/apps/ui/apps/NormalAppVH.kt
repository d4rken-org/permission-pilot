package eu.darken.myperm.apps.ui.apps

import android.view.ViewGroup
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.apps.ui.AppsAdapter
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsNormalItemBinding

class NormalAppVH(parent: ViewGroup) : AppsAdapter.BaseVH<NormalAppVH.Item, AppsNormalItemBinding>(
    R.layout.apps_normal_item,
    parent
), BindableVH<NormalAppVH.Item, AppsNormalItemBinding> {

    override val viewBinding = lazy { AppsNormalItemBinding.bind(itemView) }

    override val onBindData: AppsNormalItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val app = item.app

        packageName.text = app.packageName

        label.apply {
            text = app.label
            isSelected = true
        }

        permissionInfo.apply {
            val countTotal = app.requestedPermissions.size
            val grantedCount = app.requestedPermissions.count { it.isGranted }
            text = "$grantedCount of $countTotal permissions granted."
            if (app.isSystemApp) append(" Is a system app.")
        }

        icon.load(app.packageInfo)

        itemView.setOnClickListener { item.onClickAction(item) }
    }

    data class Item(
        override val app: NormalApp,
        val onClickAction: (Item) -> Unit
    ) : AppsAdapter.Item
}