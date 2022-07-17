package eu.darken.myperm.apps.core

import android.content.Context
import android.graphics.drawable.Drawable
import eu.darken.myperm.common.HasIcon
import eu.darken.myperm.common.HasLabel


fun Pkg.Id.toContainer(): Pkg.Container = Pkg.Container(this)

fun Pkg.tryLabel(context: Context): String? {
    context.packageManager.getLabel2(id)?.let { return it }

    (this as? HasLabel)?.let { return it.getLabel(context) }

    return null
}

fun Pkg.tryIcon(context: Context): Drawable? {
    context.packageManager.getIcon2(id)?.let { return it }

    (this as? HasIcon)?.let { return it.getIcon(context) }

    return null
}