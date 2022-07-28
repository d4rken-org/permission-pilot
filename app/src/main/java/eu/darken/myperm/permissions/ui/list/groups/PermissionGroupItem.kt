package eu.darken.myperm.permissions.ui.list.groups

import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.ui.list.PermissionsAdapter

interface PermissionGroupItem : PermissionsAdapter.Item {
    val groupId: PermissionGroup.Id
}