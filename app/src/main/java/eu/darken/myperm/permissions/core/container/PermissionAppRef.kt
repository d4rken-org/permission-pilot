package eu.darken.myperm.permissions.core.container

import eu.darken.myperm.apps.core.features.UsesPermission

data class PermissionAppRef(
    val pkgName: String,
    val userHandleId: Int,
    val label: String,
    val isSystemApp: Boolean,
    val status: UsesPermission.Status,
)
