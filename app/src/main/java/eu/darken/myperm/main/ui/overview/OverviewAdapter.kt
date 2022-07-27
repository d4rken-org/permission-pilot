package eu.darken.myperm.main.ui.overview

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
import eu.darken.myperm.main.ui.overview.items.SummaryVH
import javax.inject.Inject


class OverviewAdapter @Inject constructor() :
    ModularAdapter<OverviewAdapter.BaseVH<OverviewAdapter.Item, ViewBinding>>(),
    HasAsyncDiffer<OverviewAdapter.Item> {

    override val asyncDiffer: AsyncDiffer<*, Item> = setupDiffer()

    override fun getItemCount(): Int = data.size

    init {
        modules.add(DataBinderMod(data))
        modules.add(TypedVHCreatorMod({ data[it] is SummaryVH.Item }) { SummaryVH(it) })
    }

    abstract class BaseVH<Item : OverviewAdapter.Item, VB : ViewBinding>(
        @LayoutRes layoutRes: Int,
        parent: ViewGroup
    ) : ModularAdapter.VH(layoutRes, parent), BindableVH<Item, VB>

    interface Item : DifferItem

}