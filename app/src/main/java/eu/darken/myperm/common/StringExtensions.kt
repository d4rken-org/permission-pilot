package eu.darken.myperm.common

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun String.colorString(context: Context, @ColorRes colorRes: Int): SpannableString {
    val colored = SpannableString(this)
    colored.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, colorRes)), 0, this.length, 0)
    return colored
}