package eu.darken.myperm.apps.core.types

import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import eu.darken.myperm.permissions.core.types.BasePermission

sealed class BaseApp {

    abstract val id: String
    abstract val isSystemApp: Boolean

    abstract val requestedPermissions: Collection<UsesPermission>
    abstract fun requestsPermission(id: String): Boolean

    abstract val declaredPermissions: Collection<PermissionInfo>

    data class UsesPermission(
        val id: String,
        val flags: Int,
    ) {
        enum class PermissionStatus {
            GRANTED,
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
}

fun BaseApp.requestsPermission(permission: BasePermission) = requestsPermission(permission.id)