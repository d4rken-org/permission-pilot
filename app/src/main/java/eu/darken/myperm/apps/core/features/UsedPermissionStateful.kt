package eu.darken.myperm.apps.core.features

import android.content.pm.PackageInfo
import eu.darken.myperm.apps.core.features.PermissionState.Status
import eu.darken.myperm.permissions.core.Permission

data class UsedPermissionStateful(
    override val id: Permission.Id,
    val flags: Int?,
) : UsesPermission, PermissionState {

    override val status: Status = when {
        flags == null -> Status.UNKNOWN
        flags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 -> Status.GRANTED
        else -> Status.DENIED
    }
}
