package eu.darken.myperm.apps.core.types

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcelable
import eu.darken.myperm.apps.core.getIcon2
import eu.darken.myperm.apps.core.getLabel2
import kotlinx.parcelize.Parcelize

interface Pkg {
    val id: Id

    @Parcelize
    data class Id(val value: String) : Parcelable {
        override fun toString(): String = value
    }

    data class Container(override val id: Id) : Pkg
}

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