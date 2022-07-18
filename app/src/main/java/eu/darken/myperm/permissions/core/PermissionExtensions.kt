package eu.darken.myperm.permissions.core

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import eu.darken.myperm.apps.core.getPermissionInfo2
import eu.darken.myperm.common.HasDescription
import eu.darken.myperm.common.HasIcon
import eu.darken.myperm.common.HasLabel
import eu.darken.myperm.permissions.core.known.AKnownPermissions


fun Permission.Id.toContainer(): Permission.Container = Permission.Container(this)

fun Permission.tryLabel(context: Context): String? {
    (this as? HasLabel)?.let { return it.getLabel(context) }

    val pm = context.packageManager

    pm
        .getPermissionInfo2(id, PackageManager.GET_META_DATA)
        ?.takeIf { it.labelRes != 0 }
        ?.loadLabel(pm)
        ?.takeIf { it.isNotEmpty() && it != id.value }
        ?.let { return it.toString() }

    AKnownPermissions.values()
        .filter { it.id == id }
        .filterIsInstance<HasLabel>()
        .singleOrNull()
        ?.getLabel(context)
        ?.let { return it }

    return null
}

fun Permission.tryDescription(context: Context): String? {

    (this as? HasDescription)?.let { return it.getDescription(context) }

    val pm = context.packageManager

    pm
        .getPermissionInfo2(id, PackageManager.GET_META_DATA)
        ?.takeIf { it.labelRes != 0 }
        ?.loadDescription(pm)
        ?.takeIf { it.isNotEmpty() && it != id.value }
        ?.let { return it.toString() }

    AKnownPermissions.values()
        .filter { it.id == id }
        .filterIsInstance<HasDescription>()
        .singleOrNull()
        ?.getDescription(context)
        ?.let { return it }

    return null
}

fun Permission.tryIcon(context: Context): Drawable? {
    (this as? HasIcon)?.let { return it.getIcon(context) }

    val pm = context.packageManager

    pm
        .getPermissionInfo2(id, PackageManager.GET_META_DATA)
        ?.takeIf { it.icon != 0 }
        ?.loadIcon(pm)
        ?.let { return it }

    AKnownPermissions.values().singleOrNull { it.id == id }
        ?.iconRes
        ?.let { ContextCompat.getDrawable(context, it) }
        ?.let { return it }

    return null
}
