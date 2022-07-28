package eu.darken.myperm.permissions.core.container

import android.content.pm.PackageInfo
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.features.PermissionState
import eu.darken.myperm.permissions.core.features.PermissionState.Status

data class UsedPermissionStateful(
    override val id: Permission.Id,
    val flags: Int?,
) : Permission, PermissionState {

    override val status: Status = when {
        flags == null -> Status.UNKNOWN
        flags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 -> Status.GRANTED
        else -> Status.DENIED
    }
}
