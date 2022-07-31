package eu.darken.myperm.permissions.core.container

import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.features.getPermission
import eu.darken.myperm.apps.core.features.isGranted
import eu.darken.myperm.apps.core.features.requestsPermission
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.features.PermissionTag

data class UnknownPermission(
    override val id: Permission.Id,
    override val requestingPkgs: List<BasePkg> = emptyList(),
    override val tags: Collection<PermissionTag>,
    override val groupIds: Collection<PermissionGroup.Id>,
) : BasePermission() {

    override val grantingPkgs: Collection<BasePkg> by lazy {
        requestingPkgs
            .filter { it.requestsPermission(this) }
            .filter { it.getPermission(id)?.isGranted == true }
    }

    override val declaringPkgs: Collection<BasePkg>
        get() = emptyList()

    override fun toString(): String = "NormalPermission($id)"
}