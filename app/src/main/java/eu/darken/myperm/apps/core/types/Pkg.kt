package eu.darken.myperm.apps.core.types

import android.content.Context
import android.os.Parcelable
import eu.darken.myperm.common.pks.getLabel
import kotlinx.parcelize.Parcelize

interface Pkg {
    val id: Id

    @JvmInline
    @Parcelize
    value class Id(val value: String) : Parcelable {
        override fun toString(): String = value
    }

    fun getLabel(context: Context): String? = context.packageManager.getLabel(id)
}