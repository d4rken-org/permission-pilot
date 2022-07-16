package eu.darken.myperm.permissions.ui.list.permissions

import android.view.ViewGroup
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsUnknownItemBinding
import eu.darken.myperm.permissions.core.types.UnknownPermission
import eu.darken.myperm.permissions.ui.list.PermissionsAdapter

class UnknownPermissionVH(parent: ViewGroup) :
    PermissionsAdapter.BaseVH<UnknownPermissionVH.Item, PermissionsUnknownItemBinding>(
        R.layout.permissions_unknown_item,
        parent
    ), BindableVH<UnknownPermissionVH.Item, PermissionsUnknownItemBinding> {

    override val viewBinding = lazy { PermissionsUnknownItemBinding.bind(itemView) }

    override val onBindData: PermissionsUnknownItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val perm = item.perm

        identifier.apply {
            text = perm.id.value
        }

        val total = perm.requestingPkgs.size

        usedBy.text = "Requested by $total apps."

        itemView.setOnClickListener { item.onClickAction(item) }
    }

    data class Item(
        override val perm: UnknownPermission,
        val onClickAction: (Item) -> Unit
    ) : PermissionsAdapter.Item
}