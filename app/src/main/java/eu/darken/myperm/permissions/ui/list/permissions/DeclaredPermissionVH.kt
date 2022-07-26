package eu.darken.myperm.permissions.ui.list.permissions

import android.view.ViewGroup
import androidx.core.view.isGone
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.capitalizeFirstLetter
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsDeclaredItemBinding
import eu.darken.myperm.permissions.core.container.DeclaredPermission
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

        icon.load(
            perm.declaringPkgs.singleOrNull()?.takeIf { it.id != AKnownPkg.AndroidSystem.id } ?: perm
        )

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
        override val perm: DeclaredPermission,
        val onClickAction: (Item) -> Unit
    ) : PermissionsAdapter.Item
}