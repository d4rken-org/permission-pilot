package eu.darken.myperm.permissions.ui.list.groups

import android.view.ViewGroup
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsListGroupItemBinding
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.ui.list.PermissionsAdapter
import eu.darken.myperm.permissions.ui.list.permissions.PermissionItem

class PermissionGroupVH(parent: ViewGroup) :
    PermissionsAdapter.BaseVH<PermissionGroupVH.Item, PermissionsListGroupItemBinding>(
        R.layout.permissions_list_group_item,
        parent
    ), BindableVH<PermissionGroupVH.Item, PermissionsListGroupItemBinding> {

    override val viewBinding = lazy { PermissionsListGroupItemBinding.bind(itemView) }

    override val onBindData: PermissionsListGroupItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val group = item.group
        icon.setImageDrawable(group.getIcon(context))

        label.apply {
            text = group.getLabel(context) ?: group.id.value
        }

        quickStats.text = getQuantityString(R.plurals.generic_x_items_label, item.permissions.size)

        shortDescription.apply {
            text = group.getDescription(context)
        }

        collapseToggle.setIconResource(
            if (item.isExpanded) R.drawable.ic_baseline_expand_less_24
            else R.drawable.ic_baseline_expand_more_24
        )

        itemView.setOnClickListener { item.onClickAction(item) }
    }

    data class Item(
        val group: PermissionGroup,
        val permissions: List<PermissionItem>,
        val isExpanded: Boolean,
        val onClickAction: (Item) -> Unit,
    ) : PermissionGroupItem {
        override val groupId: PermissionGroup.Id
            get() = group.id
        override val stableId: Long
            get() = group.id.hashCode().toLong()
    }
}