package eu.darken.myperm.permissions.ui.details.items

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.view.isGone
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.features.UsesPermission.Status
import eu.darken.myperm.common.getColorForAttr
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsDetailsAppRequestingItemBinding
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.ui.details.PermissionDetailsAdapter

class AppRequestingPermissionVH(parent: ViewGroup) :
    PermissionDetailsAdapter.BaseVH<AppRequestingPermissionVH.Item, PermissionsDetailsAppRequestingItemBinding>(
        R.layout.permissions_details_app_requesting_item,
        parent
    ), BindableVH<AppRequestingPermissionVH.Item, PermissionsDetailsAppRequestingItemBinding> {

    override val viewBinding = lazy { PermissionsDetailsAppRequestingItemBinding.bind(itemView) }

    override val onBindData: PermissionsDetailsAppRequestingItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        root.setOnClickListener { item.onItemClicked(item) }

        identifier.text = item.app.id.toString()

        label.apply {
            text = item.app.getLabel(context)
            isGone = text.isNullOrEmpty()
        }

        icon.apply {
            load(item.app)
            setOnClickListener { item.onIconClicked(item) }
        }

        actionButton.apply {
            setOnClickListener { item.onIconClicked(item) }
            val (iconRes, tintRes) = when (item.status) {
                Status.GRANTED -> R.drawable.ic_baseline_check_circle_24 to R.attr.colorPrimary
                Status.GRANTED_IN_USE -> R.drawable.ic_baseline_check_circle_24 to R.attr.colorPrimary
                Status.DENIED -> R.drawable.ic_baseline_remove_circle_24 to R.attr.colorOnBackground
                Status.UNKNOWN -> R.drawable.ic_baseline_question_mark_24 to R.attr.colorOnBackground
            }
            setIconResource(iconRes)
            backgroundTintList = ColorStateList.valueOf(context.getColorForAttr(tintRes))
        }
    }

    data class Item(
        override val permission: BasePermission,
        val status: Status,
        val app: BasePkg,
        val onItemClicked: (Item) -> Unit,
        val onIconClicked: (Item) -> Unit,
    ) : PermissionDetailsAdapter.Item {
        override val stableId: Long
            get() = app.id.hashCode().toLong()
    }
}