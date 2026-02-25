package eu.darken.myperm.common

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.PluralsRes
import androidx.core.content.ContextCompat


@ColorInt
fun Context.getColorForAttr(@AttrRes attrId: Int): Int {
    var typedArray: TypedArray? = null
    try {
        typedArray = this.theme.obtainStyledAttributes(intArrayOf(attrId))
        return typedArray.getColor(0, 0)
    } finally {
        typedArray?.recycle()
    }
}

@ColorInt
fun Context.getCompatColor(@ColorRes attrId: Int): Int {
    return ContextCompat.getColor(this, attrId)
}

fun Context.dpToPx(dp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
}

fun Context.spToPx(sp: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
}

fun Context.getQuantityString(@PluralsRes pluralRes: Int, quantity: Int): String =
    resources.getQuantityString(pluralRes, quantity, quantity)
