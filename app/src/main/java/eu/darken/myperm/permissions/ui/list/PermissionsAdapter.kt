package eu.darken.myperm.permissions.ui.list

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
import eu.darken.myperm.permissions.ui.list.groups.PermissionGroupVH
import eu.darken.myperm.permissions.ui.list.permissions.DeclaredPermissionVH
import eu.darken.myperm.permissions.ui.list.permissions.ExtraPermissionVH
import eu.darken.myperm.permissions.ui.list.permissions.UnknownPermissionVH
import javax.inject.Inject


class PermissionsAdapter @Inject constructor() :
    ModularAdapter<PermissionsAdapter.BaseVH<PermissionsAdapter.Item, ViewBinding>>(),
    HasAsyncDiffer<PermissionsAdapter.Item> {

    override val asyncDiffer: AsyncDiffer<*, Item> = setupDiffer()

    override fun getItemCount(): Int = data.size

    init {
        modules.add(DataBinderMod(data))
        modules.add(TypedVHCreatorMod({ data[it] is PermissionGroupVH.Item }) { PermissionGroupVH(it) })
        modules.add(TypedVHCreatorMod({ data[it] is DeclaredPermissionVH.Item }) { DeclaredPermissionVH(it) })
        modules.add(TypedVHCreatorMod({ data[it] is ExtraPermissionVH.Item }) { ExtraPermissionVH(it) })
        modules.add(TypedVHCreatorMod({ data[it] is UnknownPermissionVH.Item }) { UnknownPermissionVH(it) })
    }

    abstract class BaseVH<Item : PermissionsAdapter.Item, VB : ViewBinding>(
        @LayoutRes layoutRes: Int,
        parent: ViewGroup
    ) : ModularAdapter.VH(layoutRes, parent), BindableVH<Item, VB>

    interface Item : DifferItem
}