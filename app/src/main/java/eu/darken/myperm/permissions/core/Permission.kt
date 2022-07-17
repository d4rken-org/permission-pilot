package eu.darken.myperm.permissions.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface Permission {
    val id: Id

    @Parcelize
    data class Id(val value: String) : Parcelable

    data class Container(override val id: Id) : Permission
}
