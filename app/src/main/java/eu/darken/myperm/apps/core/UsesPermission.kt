package eu.darken.myperm.apps.core

import android.content.pm.PackageInfo
import eu.darken.myperm.permissions.core.Permission

data class UsesPermission(
    override val id: Permission.Id,
    val flags: Int,
) : Permission {
    enum class PermissionStatus {
        GRANTED,
        GRANTED_IN_USE,
        DENIED,
    }

    val status: PermissionStatus
        get() = when {
            flags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 -> PermissionStatus.GRANTED
            else -> PermissionStatus.DENIED
        }
    val isGranted: Boolean
        get() = status == PermissionStatus.GRANTED
}