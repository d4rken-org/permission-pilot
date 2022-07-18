package eu.darken.myperm.apps.core.features

import android.content.pm.PackageInfo
import eu.darken.myperm.permissions.core.Permission

data class UsesPermission(
    override val id: Permission.Id,
    val flags: Int?,
) : Permission {
    enum class Status {
        GRANTED,
        GRANTED_IN_USE,
        DENIED,
        UNKNOWN,
    }

    val status: Status
        get() = when {
            flags == null -> Status.UNKNOWN
            flags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 -> Status.GRANTED
            else -> Status.DENIED
        }

    val isGranted: Boolean
        get() = status == Status.GRANTED
}