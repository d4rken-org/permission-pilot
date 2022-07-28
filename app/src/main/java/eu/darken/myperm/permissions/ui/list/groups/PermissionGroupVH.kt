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
//        icon.load(
//            perm.declaringPkgs.singleOrNull()?.takeIf { it.id != AKnownPkg.AndroidSystem.id } ?: perm
//        )
//
        label.apply {
            text = group.getLabel(context)
        }
//
//        val granted = perm.grantingPkgs.size
//        val total = perm.requestingPkgs.size
//
//        quickStats.text = "Granted to $granted out of $total apps."
//
//        shortDescription.apply {
//            text = perm.getLabel(context)?.capitalizeFirstLetter()
//            isGone = perm.id.value.lowercase() == text?.toString()?.lowercase() || text.isEmpty()
//        }

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