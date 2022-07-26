package eu.darken.myperm.permissions.ui.details.items

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.view.isGone
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.HasApkData
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.getColorForAttr
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsDetailsAppDeclaringItemBinding
import eu.darken.myperm.permissions.core.container.BasePermission
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
            text = item.app.getLabel(context)
            isGone = text.isNullOrEmpty()
        }

        icon.apply {
            load(item.app)
            setOnClickListener { item.onIconClicked(item) }
        }

        statusIcon.apply {
            val status = item.app.getPermission(item.permission.id)?.status
            if (status != null) {
                val (iconRes, tintRes) = when (status) {
                    UsesPermission.Status.GRANTED -> R.drawable.ic_baseline_check_circle_24 to R.attr.colorPrimary
                    UsesPermission.Status.GRANTED_IN_USE -> R.drawable.ic_baseline_check_circle_24 to R.attr.colorPrimary
                    UsesPermission.Status.DENIED -> R.drawable.ic_baseline_remove_circle_24 to R.attr.colorOnBackground
                    UsesPermission.Status.UNKNOWN -> R.drawable.ic_baseline_question_mark_24 to R.attr.colorOnBackground
                }
                setImageResource(iconRes)
                imageTintList = ColorStateList.valueOf(context.getColorForAttr(tintRes))
            }
            isGone = status == null
        }

        description.isGone = true
    }

    data class Item(
        override val permission: BasePermission,
        val app: HasApkData,
        val onItemClicked: (Item) -> Unit,
        val onIconClicked: (Item) -> Unit,
    ) : PermissionDetailsAdapter.Item {
        override val stableId: Long
            get() = app.id.hashCode().toLong()
    }
}