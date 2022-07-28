package eu.darken.myperm.permissions.ui.list.permissions

import android.view.ViewGroup
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsListUnknownItemBinding
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.container.UnknownPermission
import eu.darken.myperm.permissions.ui.list.PermissionsAdapter

class UnknownPermissionVH(parent: ViewGroup) :
    PermissionsAdapter.BaseVH<UnknownPermissionVH.Item, PermissionsListUnknownItemBinding>(
        R.layout.permissions_list_unknown_item,
        parent
    ), BindableVH<UnknownPermissionVH.Item, PermissionsListUnknownItemBinding> {

    override val viewBinding = lazy { PermissionsListUnknownItemBinding.bind(itemView) }

    override val onBindData: PermissionsListUnknownItemBinding.(
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
        val perm: UnknownPermission,
        val onClickAction: (Item) -> Unit,
    ) : PermissionItem() {
        override val permissionId: Permission.Id
            get() = perm.id
        override val stableId: Long
            get() = perm.id.hashCode().toLong()
    }
}