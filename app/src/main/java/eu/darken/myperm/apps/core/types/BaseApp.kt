package eu.darken.myperm.apps.core.types

import android.content.pm.PackageInfo
import eu.darken.myperm.permissions.core.types.BasePermission

sealed class BaseApp {

    abstract val id: String
    abstract val isSystemApp: Boolean

    abstract fun requestsPermission(id: String): Boolean

    data class UsesPermission(
        val id: String,
        val flags: Int,
    ) {
        val isGranted: Boolean
            get() = flags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
    }
}

fun BaseApp.requestsPermission(permission: BasePermission) = requestsPermission(permission.id)