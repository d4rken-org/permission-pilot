package eu.darken.myperm.apps.core.types

import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import eu.darken.myperm.permissions.core.PermissionId
import eu.darken.myperm.permissions.core.types.BasePermission
import java.time.Instant

sealed class BaseApp {

    abstract val id: String
    abstract val packageInfo: PackageInfo
    abstract val isSystemApp: Boolean
    abstract val label: String?

    abstract val installedAt: Instant
    abstract val updatedAt: Instant

    abstract val requestedPermissions: Collection<UsesPermission>
    abstract fun requestsPermission(id: PermissionId): Boolean

    abstract val declaredPermissions: Collection<PermissionInfo>
    abstract fun declaresPermission(id: PermissionId): Boolean

    abstract fun getPermission(id: PermissionId): UsesPermission?

    abstract val internetAccess: InternetAccess

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

    enum class InternetAccess {
        DIRECT,
        INDIRECT,
        NONE
    }
}

fun BaseApp.requestsPermission(permission: BasePermission) = requestsPermission(permission.id)