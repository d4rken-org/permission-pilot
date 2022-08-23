package eu.darken.myperm.permissions.ui.list.permissions

import android.view.ViewGroup
import androidx.core.view.isGone
import eu.darken.myperm.R
import eu.darken.myperm.common.capitalizeFirstLetter
import eu.darken.myperm.common.coil.loadPermissionIcon
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsListDeclaredItemBinding
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.ui.list.PermissionsAdapter

class DeclaredPermissionVH(parent: ViewGroup) :
    PermissionsAdapter.BaseVH<DeclaredPermissionVH.Item, PermissionsListDeclaredItemBinding>(
        R.layout.permissions_list_declared_item,
        parent
    ), BindableVH<DeclaredPermissionVH.Item, PermissionsListDeclaredItemBinding> {

    override val viewBinding = lazy { PermissionsListDeclaredItemBinding.bind(itemView) }

    override val onBindData: PermissionsListDeclaredItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val perm = item.permission

        icon.apply {
            loadPermissionIcon(perm)
            setOnClickListener { item.onIconClick(item) }
        }

        identifier.apply {
            text = perm.id.value
        }

        val granted = perm.grantingPkgs.size
        val total = perm.requestingPkgs.size

        usedBy.text = "Granted to $granted out of $total apps."

        shortDescription.apply {
            text = perm.getLabel(context)?.capitalizeFirstLetter()
            isGone = perm.id.value.lowercase() == text?.toString()?.lowercase() || text.isEmpty()
        }

        itemView.setOnClickListener { item.onClickAction(item) }
    }

    data class Item(
        override val permission: BasePermission,
        val onClickAction: (Item) -> Unit,
        val onIconClick: (Item) -> Unit,
    ) : PermissionItem() {
        override val stableId: Long
            get() = permission.id.hashCode().toLong()
    }
}