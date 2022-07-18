package eu.darken.myperm.apps.core

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import android.provider.Settings
import androidx.core.content.ContextCompat
import eu.darken.myperm.apps.core.known.AKnownPkg
import kotlinx.parcelize.Parcelize

interface Pkg {
    val id: Id

    fun getLabel(context: Context): String? {
        context.packageManager.getLabel2(id)?.let { return it }

        AKnownPkg.values()
            .singleOrNull { it.id == id }
            ?.labelRes
            ?.let { return context.getString(it) }

        return null
    }

    fun getIcon(context: Context): Drawable? {
        context.packageManager.getIcon2(id)?.let { return it }

        AKnownPkg.values()
            .singleOrNull { it.id == id }
            ?.iconRes
            ?.let { ContextCompat.getDrawable(context, it) }
            ?.let { return it }

        return null
    }

    @Parcelize
    data class Id(val value: String) : Parcelable {
        override fun toString(): String = value
    }

    data class Container(override val id: Id) : Pkg
}

fun Pkg.Id.toContainer(): Pkg.Container = Pkg.Container(this)

fun Pkg.getSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${id}")
    }