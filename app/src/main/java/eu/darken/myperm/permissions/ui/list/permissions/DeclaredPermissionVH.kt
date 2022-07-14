package eu.darken.myperm.permissions.ui.list.permissions

import android.view.ViewGroup
import androidx.core.view.isGone
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsDeclaredItemBinding
import eu.darken.myperm.permissions.core.types.DeclaredPermission
import eu.darken.myperm.permissions.ui.list.PermissionsAdapter

class DeclaredPermissionVH(parent: ViewGroup) :
    PermissionsAdapter.BaseVH<DeclaredPermissionVH.Item, PermissionsDeclaredItemBinding>(
        R.layout.permissions_declared_item,
        parent
    ), BindableVH<DeclaredPermissionVH.Item, PermissionsDeclaredItemBinding> {

    override val viewBinding = lazy { PermissionsDeclaredItemBinding.bind(itemView) }

    override val onBindData: PermissionsDeclaredItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val perm = item.perm

        identifier.apply {
            text = perm.id
        }

        val granted = perm.grantedApps.size
        val total = perm.requestingApps.size

        usedBy.text = "Granted to $granted out of $total apps."

        shortDescription.apply {
            text = perm.label
            isGone = perm.id.lowercase() == perm.label?.lowercase() || perm.label == null
        }

        itemView.setOnClickListener { item.onClickAction(item) }
    }

    data class Item(
        override val perm: DeclaredPermission,
        val onClickAction: (Item) -> Unit
    ) : PermissionsAdapter.Item
}