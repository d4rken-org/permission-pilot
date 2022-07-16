package eu.darken.myperm.apps.core.types

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface Pkg {
    val id: Id

    @JvmInline
    @Parcelize
    value class Id(val value: String) : Parcelable {
        override fun toString(): String = value
    }
}