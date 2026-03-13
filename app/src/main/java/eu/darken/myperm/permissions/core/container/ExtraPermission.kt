package eu.darken.myperm.permissions.core.container

import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.features.PermissionTag


data class ExtraPermission(
    override val id: Permission.Id,
    override val requestingApps: List<PermissionAppRef> = emptyList(),
    override val declaringApps: List<PermissionAppRef> = emptyList(),
    override val tags: Collection<PermissionTag>,
    override val groupIds: Collection<PermissionGroup.Id>,
) : BasePermission() {

    override val grantingApps: List<PermissionAppRef> by lazy {
        requestingApps.filter { it.status.isGranted }
    }
}
