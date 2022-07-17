package eu.darken.myperm.permissions.core

import android.content.Context
import android.graphics.drawable.Drawable
import eu.darken.myperm.common.HasIcon
import eu.darken.myperm.common.HasLabel


fun Permission.Id.toContainer(): Permission.Container = Permission.Container(this)

fun Permission.tryLabel(context: Context): String? {
    val pm = context.packageManager

    pm
        .getPermissionInfo(id.value, 0)
        .takeIf { it.labelRes != 0 }
        ?.loadLabel(pm)
        ?.takeIf { it.isNotEmpty() && it != id.value }
        ?.let { return it.toString() }

    (this as? HasLabel)?.let { return it.getLabel(context) }

    return null
}

fun Permission.tryIcon(context: Context): Drawable? {
    val pm = context.packageManager

    pm
        .getPermissionInfo(id.value, 0)
        .takeIf { it.icon != 0 }
        ?.loadIcon(pm)
        ?.let { return it }

    (this as? HasIcon)?.let { return it.getIcon(context) }

    return null
}
