package eu.darken.myperm.permissions.ui.details.items

import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.DividerItemDecorator2
import eu.darken.myperm.common.capitalizeFirstLetter
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.PermissionsDetailsOverviewItemBinding
import eu.darken.myperm.permissions.core.types.BasePermission
import eu.darken.myperm.permissions.core.types.DeclaredPermission
import eu.darken.myperm.permissions.ui.details.PermissionDetailsAdapter

class PermissionOverviewVH(parent: ViewGroup) :
    PermissionDetailsAdapter.BaseVH<PermissionOverviewVH.Item, PermissionsDetailsOverviewItemBinding>(
        R.layout.permissions_details_overview_item,
        parent
    ), BindableVH<PermissionOverviewVH.Item, PermissionsDetailsOverviewItemBinding>, DividerItemDecorator2.SkipDivider {

    override val viewBinding = lazy { PermissionsDetailsOverviewItemBinding.bind(itemView) }

    override val onBindData: PermissionsDetailsOverviewItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        val perm = item.permission

        icon.load(
            perm.declaringPkgs.singleOrNull()?.takeIf { it.id != AKnownPkg.AndroidSystem.id } ?: perm
        )

        identifier.text = perm.id.value
        label.apply {
            text = perm.getLabel(context)?.capitalizeFirstLetter()
            isGone = text.isNullOrEmpty()
        }

        description.apply {
            text = perm.getDescription(context)
            isGone = text.isNullOrEmpty()
        }

        protectionLabel.isGone = perm !is DeclaredPermission
        protectionInfo.apply {
            isGone = perm !is DeclaredPermission
            if (perm is DeclaredPermission) {
                text = getString(perm.protectionType.labelRes)
                if (perm.protectionFlags.isNotEmpty()) {
                    append(" (${perm.protectionFlags.joinToString(", ")})")
                }
            }
        }
        tagAosp.isInvisible = perm.declaringPkgs.any { it.id == AKnownPkg.AndroidSystem.id }
        tagContainer.isGone = tagContainer.children.all { !it.isVisible }
    }

    data class Item(
        override val permission: BasePermission
    ) : PermissionDetailsAdapter.Item {
        override val stableId: Long
            get() = permission.id.hashCode().toLong()
    }
}