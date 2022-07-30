package eu.darken.myperm.permissions.ui.list.permissions

import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.ui.list.PermissionsAdapter

sealed class PermissionItem : PermissionsAdapter.Item {
    abstract val permission: Permission

    val permissionId: Permission.Id
        get() = permission.id
}