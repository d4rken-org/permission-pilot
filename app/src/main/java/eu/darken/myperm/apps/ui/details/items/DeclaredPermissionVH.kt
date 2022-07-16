package eu.darken.myperm.apps.ui.details.items

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.UsesPermission
import eu.darken.myperm.apps.core.UsesPermission.PermissionStatus
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsPermissionDeclaredItemBinding
import eu.darken.myperm.permissions.core.types.DeclaredPermission

class DeclaredPermissionVH(parent: ViewGroup) :
    AppDetailsAdapter.BaseVH<DeclaredPermissionVH.Item, AppsDetailsPermissionDeclaredItemBinding>(
        R.layout.apps_details_permission_declared_item,
        parent
    ), BindableVH<DeclaredPermissionVH.Item, AppsDetailsPermissionDeclaredItemBinding> {

    override val viewBinding = lazy { AppsDetailsPermissionDeclaredItemBinding.bind(itemView) }

    override val onBindData: AppsDetailsPermissionDeclaredItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val permission = item.permission

        identifier.text = permission.id.value
        label.apply {
            text = permission.label
            isGone = permission.label.isNullOrEmpty()
        }

        statusIcon.apply {
            val (iconRes, tintRes) = when (item.appPermission.status) {
                PermissionStatus.GRANTED -> R.drawable.ic_baseline_check_circle_24 to R.color.status_positive_1
                PermissionStatus.GRANTED_IN_USE -> R.drawable.ic_baseline_check_circle_24 to R.color.status_positive_1
                PermissionStatus.DENIED -> R.drawable.ic_baseline_remove_circle_24 to R.color.status_negative_1
            }
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, tintRes))
        }

        root.setOnClickListener { item.onItemClicked(item) }
    }

    data class Item(
        val appPermission: UsesPermission,
        val permission: DeclaredPermission,
        val onItemClicked: (Item) -> Unit,
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = permission.id.hashCode().toLong()
    }
}