package eu.darken.myperm.permissions.ui.permissions

import android.view.ViewGroup
import androidx.core.view.isGone
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsNormalItemBinding
import eu.darken.myperm.permissions.core.types.NormalPermission
import eu.darken.myperm.permissions.ui.PermissionsAdapter
import java.util.*

class NormalPermissionVH(parent: ViewGroup) :
    PermissionsAdapter.BaseVH<NormalPermissionVH.Item, PermissionsNormalItemBinding>(
        R.layout.permissions_normal_item,
        parent
    ), BindableVH<NormalPermissionVH.Item, PermissionsNormalItemBinding> {

    override val viewBinding = lazy { PermissionsNormalItemBinding.bind(itemView) }

    override val onBindData: PermissionsNormalItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val perm = item.perm

        identifier.apply {
            text = perm.id
        }

        shortDescription.apply {
            text = perm.label?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            isGone = perm.id == perm.label || perm.label == null
        }

        val granted = perm.grantedApps.size
        val total = perm.requestingApps.size

        usedBy.text = "Granted to $granted out of $total apps."

        itemView.setOnClickListener { item.onClickAction(item) }
    }

    data class Item(
        override val perm: NormalPermission,
        val onClickAction: (Item) -> Unit
    ) : PermissionsAdapter.Item
}