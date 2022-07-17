package eu.darken.myperm.permissions.ui.details.items

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.UsesPermission
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsDetailsAppDeclaringItemBinding
import eu.darken.myperm.permissions.core.types.BasePermission
import eu.darken.myperm.permissions.ui.details.PermissionDetailsAdapter

class AppRequestingPermissionVH(parent: ViewGroup) :
    PermissionDetailsAdapter.BaseVH<AppRequestingPermissionVH.Item, PermissionsDetailsAppDeclaringItemBinding>(
        R.layout.permissions_details_app_requesting_item,
        parent
    ), BindableVH<AppRequestingPermissionVH.Item, PermissionsDetailsAppDeclaringItemBinding> {

    override val viewBinding = lazy { PermissionsDetailsAppDeclaringItemBinding.bind(itemView) }

    override val onBindData: PermissionsDetailsAppDeclaringItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        root.setOnClickListener { item.onItemClicked(item) }

        identifier.text = item.app.id.toString()

        label.apply {
            text = (item.app as? NormalApp)?.label
            isGone = text.isNullOrEmpty()
        }

        icon.load(item.app)

        statusIcon.apply {
            val status = item.app.getPermission(item.permission.id)?.status
            if (status != null) {
                val (iconRes, tintRes) = when (status) {
                    UsesPermission.PermissionStatus.GRANTED -> R.drawable.ic_baseline_check_circle_24 to R.color.status_positive_1
                    UsesPermission.PermissionStatus.GRANTED_IN_USE -> R.drawable.ic_baseline_check_circle_24 to R.color.status_positive_1
                    UsesPermission.PermissionStatus.DENIED -> R.drawable.ic_baseline_remove_circle_24 to R.color.status_negative_1
                }
                setImageResource(iconRes)
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, tintRes))
            }
            isGone = status == null
        }

        description.isGone = true
    }

    data class Item(
        override val permission: BasePermission,
        val app: BaseApp,
        val onItemClicked: (Item) -> Unit,
    ) : PermissionDetailsAdapter.Item {
        override val stableId: Long
            get() = app.id.hashCode().toLong()
    }
}