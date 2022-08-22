package eu.darken.myperm.permissions.ui.list.permissions

import android.view.ViewGroup
import androidx.core.view.isGone
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.common.capitalizeFirstLetter
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsListExtraItemBinding
import eu.darken.myperm.permissions.core.container.ExtraPermission
import eu.darken.myperm.permissions.ui.list.PermissionsAdapter

class ExtraPermissionVH(parent: ViewGroup) :
    PermissionsAdapter.BaseVH<ExtraPermissionVH.Item, PermissionsListExtraItemBinding>(
        R.layout.permissions_list_extra_item,
        parent
    ), BindableVH<ExtraPermissionVH.Item, PermissionsListExtraItemBinding> {

    override val viewBinding = lazy { PermissionsListExtraItemBinding.bind(itemView) }

    override val onBindData: PermissionsListExtraItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val perm = item.permission

        icon.apply {
            load(perm)
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
        override val permission: ExtraPermission,
        val onClickAction: (Item) -> Unit,
        val onIconClick: (Item) -> Unit,
    ) : PermissionItem() {
        override val stableId: Long
            get() = permission.id.hashCode().toLong()
    }
}