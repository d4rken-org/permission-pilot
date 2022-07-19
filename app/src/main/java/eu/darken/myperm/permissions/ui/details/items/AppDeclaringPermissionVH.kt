package eu.darken.myperm.permissions.ui.details.items

import android.view.ViewGroup
import androidx.core.view.isGone
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsDetailsAppDeclaringItemBinding
import eu.darken.myperm.permissions.core.types.BasePermission
import eu.darken.myperm.permissions.ui.details.PermissionDetailsAdapter

class AppDeclaringPermissionVH(parent: ViewGroup) :
    PermissionDetailsAdapter.BaseVH<AppDeclaringPermissionVH.Item, PermissionsDetailsAppDeclaringItemBinding>(
        R.layout.permissions_details_app_declaring_item,
        parent
    ), BindableVH<AppDeclaringPermissionVH.Item, PermissionsDetailsAppDeclaringItemBinding> {

    override val viewBinding = lazy { PermissionsDetailsAppDeclaringItemBinding.bind(itemView) }

    override val onBindData: PermissionsDetailsAppDeclaringItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        identifier.text = item.app.id.toString()

        label.apply {
            text = item.app.getLabel(context)
            isGone = text.isNullOrEmpty()
        }

        icon.load(item.app)

        root.setOnClickListener { item.onItemClicked(item) }
    }

    data class Item(
        override val permission: BasePermission,
        val app: Pkg,
        val onItemClicked: (Item) -> Unit,
    ) : PermissionDetailsAdapter.Item {
        override val stableId: Long
            get() = app.id.hashCode().toLong()
    }
}