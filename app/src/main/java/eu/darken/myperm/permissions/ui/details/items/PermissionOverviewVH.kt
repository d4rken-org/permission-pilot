package eu.darken.myperm.permissions.ui.details.items

import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsDetailsOverviewItemBinding
import eu.darken.myperm.permissions.core.types.BasePermission
import eu.darken.myperm.permissions.ui.details.PermissionDetailsAdapter

class PermissionOverviewVH(parent: ViewGroup) :
    PermissionDetailsAdapter.BaseVH<PermissionOverviewVH.Item, PermissionsDetailsOverviewItemBinding>(
        R.layout.permissions_details_overview_item,
        parent
    ), BindableVH<PermissionOverviewVH.Item, PermissionsDetailsOverviewItemBinding> {

    override val viewBinding = lazy { PermissionsDetailsOverviewItemBinding.bind(itemView) }

    override val onBindData: PermissionsDetailsOverviewItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val permission = item.permission

        identifier.text = permission.id.value
        label.apply {
            text = permission.label
            isGone = permission.label.isNullOrEmpty()
        }

        description.apply {
            text = permission.description
            isGone = permission.description.isNullOrEmpty()
        }

        tagAosp.isInvisible = !permission.isAospPermission
        tagContainer.isGone = tagContainer.children.all { !it.isVisible }
    }

    data class Item(
        override val permission: BasePermission
    ) : PermissionDetailsAdapter.Item {
        override val stableId: Long
            get() = permission.id.hashCode().toLong()
    }
}