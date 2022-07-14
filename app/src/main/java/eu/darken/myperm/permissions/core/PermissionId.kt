package eu.darken.myperm.permissions.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@JvmInline
value class PermissionId(
    val value: String,
) : Parcelable