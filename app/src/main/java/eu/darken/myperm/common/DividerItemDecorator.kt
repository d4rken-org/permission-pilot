package eu.darken.myperm.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView

class DividerItemDecorator2(context: Context) : RecyclerView.ItemDecoration() {

    private val divider: Drawable by lazy {
        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider))
        val _divider = a.getDrawable(0)!!
        a.recycle()
        _divider
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val dividerLeft = parent.paddingLeft
        val dividerRight = parent.width - parent.paddingRight
        val childCount = parent.childCount

        for (i in 0..childCount - 2) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val viewHolder = parent.getChildViewHolder(child)

            if (viewHolder is SkipDivider) {
                continue
            }

            val dividerTop = child.bottom + params.bottomMargin
            val dividerBottom = dividerTop + divider.intrinsicHeight
            divider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)

            divider.draw(canvas)
        }
    }

    interface SkipDivider
}