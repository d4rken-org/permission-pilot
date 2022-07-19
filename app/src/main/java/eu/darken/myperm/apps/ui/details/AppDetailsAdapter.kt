package eu.darken.myperm.apps.ui.details

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import eu.darken.myperm.apps.ui.details.items.*
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.common.lists.differ.AsyncDiffer
import eu.darken.myperm.common.lists.differ.DifferItem
import eu.darken.myperm.common.lists.differ.HasAsyncDiffer
import eu.darken.myperm.common.lists.differ.setupDiffer
import eu.darken.myperm.common.lists.modular.ModularAdapter
import eu.darken.myperm.common.lists.modular.mods.DataBinderMod
import eu.darken.myperm.common.lists.modular.mods.TypedVHCreatorMod
import javax.inject.Inject


class AppDetailsAdapter @Inject constructor() :
    ModularAdapter<AppDetailsAdapter.BaseVH<AppDetailsAdapter.Item, ViewBinding>>(),
    HasAsyncDiffer<AppDetailsAdapter.Item> {

    override val asyncDiffer: AsyncDiffer<*, Item> = setupDiffer()

    override fun getItemCount(): Int = data.size

    init {
        modules.add(DataBinderMod(data))
        modules.add(TypedVHCreatorMod({ data[it] is AppOverviewVH.Item }) { AppOverviewVH(it) })
        modules.add(TypedVHCreatorMod({ data[it] is AppTwinsVH.Item }) { AppTwinsVH(it) })
        modules.add(TypedVHCreatorMod({ data[it] is AppSiblingsVH.Item }) { AppSiblingsVH(it) })
        modules.add(TypedVHCreatorMod({ data[it] is DeclaredPermissionVH.Item }) { DeclaredPermissionVH(it) })
        modules.add(TypedVHCreatorMod({ data[it] is UnknownPermissionVH.Item }) { UnknownPermissionVH(it) })
    }

    abstract class BaseVH<Item : AppDetailsAdapter.Item, VB : ViewBinding>(
        @LayoutRes layoutRes: Int,
        parent: ViewGroup
    ) : ModularAdapter.VH(layoutRes, parent), BindableVH<Item, VB>

    interface Item : DifferItem

}