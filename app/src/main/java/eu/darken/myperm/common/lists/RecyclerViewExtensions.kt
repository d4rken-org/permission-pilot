package eu.darken.myperm.common.lists

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.darken.myperm.common.DividerItemDecorator2

fun RecyclerView.setupDefaults(adapter: RecyclerView.Adapter<*>? = null, dividers: Boolean = true) = apply {
    layoutManager = LinearLayoutManager(context)
    itemAnimator = DefaultItemAnimator()
    if (dividers) addItemDecoration(DividerItemDecorator2(context))
    if (adapter != null) this.adapter = adapter
}