package eu.darken.myperm.apps.ui.details.items

import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.core.view.isGone
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.features.UsesPermission.Status
import eu.darken.myperm.apps.ui.details.AppDetailsAdapter
import eu.darken.myperm.common.capitalizeFirstLetter
import eu.darken.myperm.common.getColorForAttr
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsDetailsPermissionUsesItemBinding
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.id

class UsesPermissionVH(parent: ViewGroup) :
    AppDetailsAdapter.BaseVH<UsesPermissionVH.Item, AppsDetailsPermissionUsesItemBinding>(
        R.layout.apps_details_permission_uses_item,
        parent
    ), BindableVH<UsesPermissionVH.Item, AppsDetailsPermissionUsesItemBinding> {

    override val viewBinding = lazy { AppsDetailsPermissionUsesItemBinding.bind(itemView) }

    override val onBindData: AppsDetailsPermissionUsesItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val permission = item.permission

        icon.apply {
            setImageDrawable(permission.getIcon(context))
        }

        identifier.text = permission.id.value

        label.apply {
            text = permission.getLabel(context)?.capitalizeFirstLetter() ?: permission.id.value.split(".").lastOrNull()
            isGone = text.isNullOrEmpty()
        }

        extraInfo.apply {
            val isDeclaredbyThisApp = item.pkg.declaredPermissions.any { it.id == item.appPermission.id }
            text = when {
                isDeclaredbyThisApp -> getString(R.string.permissions_app_type_declaring_description)
                else -> null
            }
            isGone = text.isNullOrEmpty()
        }

        actionButton.apply {
            setOnClickListener { item.onTogglePermission(item) }
            val (iconRes, tintRes) = when (item.appPermission.status) {
                Status.GRANTED -> R.drawable.ic_baseline_check_circle_24 to com.google.android.material.R.attr.colorPrimary
                Status.GRANTED_IN_USE -> R.drawable.ic_baseline_check_circle_24 to com.google.android.material.R.attr.colorPrimary
                Status.DENIED -> R.drawable.ic_baseline_remove_circle_24 to com.google.android.material.R.attr.colorOnBackground
                Status.UNKNOWN -> R.drawable.ic_baseline_question_mark_24 to com.google.android.material.R.attr.colorOnBackground
            }
            setIconResource(iconRes)
            backgroundTintList = ColorStateList.valueOf(context.getColorForAttr(tintRes))
        }

        root.setOnClickListener { item.onItemClicked(item) }
    }

    data class Item(
        val pkg: BasePkg,
        val appPermission: UsesPermission,
        val permission: BasePermission,
        val onItemClicked: (Item) -> Unit,
        val onTogglePermission: (Item) -> Unit,
    ) : AppDetailsAdapter.Item {
        override val stableId: Long
            get() = permission.id.hashCode().toLong()
    }
}