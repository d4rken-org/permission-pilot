package eu.darken.myperm.apps.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface Pkg {
    val id: Id

    @Parcelize
    data class Id(val value: String) : Parcelable {
        override fun toString(): String = value
    }

    data class Container(override val id: Id) : Pkg
}