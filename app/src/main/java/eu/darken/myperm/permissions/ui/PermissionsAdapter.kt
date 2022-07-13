package eu.darken.myperm.permissions.ui

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
import eu.darken.myperm.permissions.ui.permissions.NormalPermissionVH
import javax.inject.Inject


class PermissionsAdapter @Inject constructor() :
    ModularAdapter<PermissionsAdapter.BaseVH<PermissionsAdapter.Item, ViewBinding>>(),
    HasAsyncDiffer<PermissionsAdapter.Item> {

    override val asyncDiffer: AsyncDiffer<*, Item> = setupDiffer()

    override fun getItemCount(): Int = data.size

    init {
        modules.add(DataBinderMod(data))
        modules.add(TypedVHCreatorMod({ data[it] is NormalPermissionVH.Item }) { NormalPermissionVH(it) })
    }

    abstract class BaseVH<Item : PermissionsAdapter.Item, VB : ViewBinding>(
        @LayoutRes layoutRes: Int,
        parent: ViewGroup
    ) : ModularAdapter.VH(layoutRes, parent), BindableVH<Item, VB>

    interface Item : DifferItem {
        val perm: BasePermission
        override val stableId: Long
            get() = perm.id.hashCode().toLong()
    }

}