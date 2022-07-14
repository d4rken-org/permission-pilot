package eu.darken.myperm.apps.core.types

import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import eu.darken.myperm.permissions.core.PermissionId
import eu.darken.myperm.permissions.core.types.BasePermission

sealed class BaseApp {

    abstract val packageInfo: PackageInfo
    abstract val id: String
    abstract val isSystemApp: Boolean

    abstract val requestedPermissions: Collection<UsesPermission>
    abstract fun requestsPermission(id: PermissionId): Boolean

    abstract val declaredPermissions: Collection<PermissionInfo>
    abstract fun declaresPermission(id: PermissionId): Boolean

    abstract fun getPermissionStatus(id: PermissionId): UsesPermission.PermissionStatus?

    data class UsesPermission(
        val id: PermissionId,
        val flags: Int,
    ) {
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
}

fun BaseApp.requestsPermission(permission: BasePermission) = requestsPermission(permission.id)