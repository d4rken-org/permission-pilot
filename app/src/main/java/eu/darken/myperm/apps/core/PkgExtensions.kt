package eu.darken.myperm.apps.core

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.HasIcon
import eu.darken.myperm.common.HasLabel


fun Pkg.Id.toContainer(): Pkg.Container = Pkg.Container(this)

fun Pkg.tryLabel(context: Context): String? {
    (this as? HasLabel)?.let { return it.getLabel(context) }

    context.packageManager.getLabel2(id)?.let { return it }

    AKnownPkg.values()
        .singleOrNull { it.id == id }
        ?.let { it as? HasLabel }
        ?.let { return it.getLabel(context) }

    return null
}

fun Pkg.tryIcon(context: Context): Drawable? {
    (this as? HasIcon)?.let { return it.getIcon(context) }

    context.packageManager.getIcon2(id)?.let { return it }

    AKnownPkg.values()
        .singleOrNull { it.id == id }
        ?.let { it as? HasIcon }
        ?.let { return it.getIcon(context) }

    return null
}

fun Pkg.getSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${id}")
    }