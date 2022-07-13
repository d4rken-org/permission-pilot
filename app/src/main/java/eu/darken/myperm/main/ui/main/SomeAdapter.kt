package eu.darken.myperm.main.ui.main

import android.view.ViewGroup
import eu.darken.myperm.R
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.common.lists.differ.AsyncDiffer
import eu.darken.myperm.common.lists.differ.DifferItem
import eu.darken.myperm.common.lists.differ.HasAsyncDiffer
import eu.darken.myperm.common.lists.differ.setupDiffer
import eu.darken.myperm.common.lists.modular.ModularAdapter
import eu.darken.myperm.common.lists.modular.mods.DataBinderMod
import eu.darken.myperm.common.lists.modular.mods.SimpleVHCreatorMod
import eu.darken.myperm.databinding.SomeItemLineBinding
import javax.inject.Inject


class SomeAdapter @Inject constructor() : ModularAdapter<SomeAdapter.ItemVH>(),
    HasAsyncDiffer<SomeAdapter.Item> {

    override val asyncDiffer: AsyncDiffer<*, Item> = setupDiffer()

    override fun getItemCount(): Int = data.size

    init {
        modules.add(DataBinderMod(data))
        modules.add(SimpleVHCreatorMod { ItemVH(it) })
    }

    data class Item(
        val label: String,
        val number: Long,
        val onClickAction: (Long) -> Unit
    ) : DifferItem {
        override val stableId: Long = label.hashCode().toLong()
    }

    class ItemVH(parent: ViewGroup) : ModularAdapter.VH(R.layout.some_item_line, parent),
        BindableVH<Item, SomeItemLineBinding> {

        override val viewBinding: Lazy<SomeItemLineBinding> = lazy {
            SomeItemLineBinding.bind(itemView)
        }

        override val onBindData: SomeItemLineBinding.(
            item: Item,
            payloads: List<Any>
        ) -> Unit = { item, _ ->
            numberDisplay.text = "${item.label} #${item.number}"

            itemView.setOnClickListener { item.onClickAction(item.number) }
        }
    }
}