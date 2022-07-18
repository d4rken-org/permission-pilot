package eu.darken.myperm.apps.ui.list

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import eu.darken.myperm.apps.core.features.ApkPkg
import eu.darken.myperm.apps.ui.list.apps.NormalAppVH
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.common.lists.differ.AsyncDiffer
import eu.darken.myperm.common.lists.differ.DifferItem
import eu.darken.myperm.common.lists.differ.HasAsyncDiffer
import eu.darken.myperm.common.lists.differ.setupDiffer
import eu.darken.myperm.common.lists.modular.ModularAdapter
import eu.darken.myperm.common.lists.modular.mods.DataBinderMod
import eu.darken.myperm.common.lists.modular.mods.TypedVHCreatorMod
import javax.inject.Inject


class AppsAdapter @Inject constructor() : ModularAdapter<AppsAdapter.BaseVH<AppsAdapter.Item, ViewBinding>>(),
    HasAsyncDiffer<AppsAdapter.Item> {

    override val asyncDiffer: AsyncDiffer<*, Item> = setupDiffer()

    override fun getItemCount(): Int = data.size

    init {
        modules.add(DataBinderMod(data))
        modules.add(TypedVHCreatorMod({ data[it] is NormalAppVH.Item }) { NormalAppVH(it) })
    }

    abstract class BaseVH<Item : AppsAdapter.Item, VB : ViewBinding>(
        @LayoutRes layoutRes: Int,
        parent: ViewGroup
    ) : ModularAdapter.VH(layoutRes, parent), BindableVH<Item, VB>

    interface Item : DifferItem {
        val app: ApkPkg
        override val stableId: Long
            get() = app.id.hashCode().toLong()
    }

}