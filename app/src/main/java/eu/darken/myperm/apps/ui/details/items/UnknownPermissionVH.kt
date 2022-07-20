package eu.darken.myperm.apps.ui.details.items

import android.view.ViewGroup
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsPermissionUnknownItemBinding
import eu.darken.myperm.permissions.core.types.UnknownPermission

class UnknownPermissionVH(parent: ViewGroup) :
    AppDetailsAdapter.BaseVH<UnknownPermissionVH.Item, AppsDetailsPermissionUnknownItemBinding>(
        R.layout.apps_details_permission_unknown_item,
        parent
    ), BindableVH<UnknownPermissionVH.Item, AppsDetailsPermissionUnknownItemBinding> {

    override val viewBinding = lazy { AppsDetailsPermissionUnknownItemBinding.bind(itemView) }

    override val onBindData: AppsDetailsPermissionUnknownItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val permission = item.permission

        identifier.text = permission.id.value

        root.setOnClickListener { item.onItemClicked(item) }
    }

    data class Item(
        val appPermission: UsesPermission,
        val permission: UnknownPermission,
        val onItemClicked: (Item) -> Unit,
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = permission.id.hashCode().toLong()
    }
}