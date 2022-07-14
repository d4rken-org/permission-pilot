package eu.darken.myperm.permissions.ui.details

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.common.lists.differ.AsyncDiffer
import eu.darken.myperm.common.lists.differ.DifferItem
import eu.darken.myperm.common.lists.differ.HasAsyncDiffer
import eu.darken.myperm.common.lists.differ.setupDiffer
import eu.darken.myperm.common.lists.modular.ModularAdapter
import eu.darken.myperm.common.lists.modular.mods.DataBinderMod
import eu.darken.myperm.common.lists.modular.mods.TypedVHCreatorMod
import eu.darken.myperm.permissions.core.types.BasePermission
import eu.darken.myperm.permissions.ui.details.items.PermissionOverviewVH
import javax.inject.Inject


class PermissionDetailsAdapter @Inject constructor() :
    ModularAdapter<PermissionDetailsAdapter.BaseVH<PermissionDetailsAdapter.Item, ViewBinding>>(),
    HasAsyncDiffer<PermissionDetailsAdapter.Item> {

    override val asyncDiffer: AsyncDiffer<*, Item> = setupDiffer()

    override fun getItemCount(): Int = data.size

    init {
        modules.add(DataBinderMod(data))
        modules.add(TypedVHCreatorMod({ data[it] is PermissionOverviewVH.Item }) { PermissionOverviewVH(it) })
    }

    abstract class BaseVH<Item : PermissionDetailsAdapter.Item, VB : ViewBinding>(
        @LayoutRes layoutRes: Int,
        parent: ViewGroup
    ) : ModularAdapter.VH(layoutRes, parent), BindableVH<Item, VB>

    interface Item : DifferItem {
        val permission: BasePermission
        override val stableId: Long
            get() = permission.id.hashCode().toLong()
    }

}