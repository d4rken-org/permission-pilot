package eu.darken.myperm.permissions.core.container

import android.content.pm.PermissionInfo
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.features.getPermission
import eu.darken.myperm.apps.core.features.isGranted
import eu.darken.myperm.apps.core.features.requestsPermission
import eu.darken.myperm.permissions.core.*
import eu.darken.myperm.permissions.core.features.PermissionTag


data class DeclaredPermission(
    val permissionInfo: PermissionInfo,
    override val requestingPkgs: List<BasePkg> = emptyList(),
    override val declaringPkgs: Collection<BasePkg> = emptyList(),
    override val tags: Collection<PermissionTag>,
    override val groupIds: Collection<PermissionGroup.Id>,
) : BasePermission() {

    override val id: Permission.Id
        get() = Permission.Id(permissionInfo.name)

    override val grantingPkgs: Collection<BasePkg> by lazy {
        requestingPkgs
            .filter { it.requestsPermission(this) }
            .filter { it.getPermission(id)?.isGranted == true }
    }

    val protectionType: ProtectionType by lazy { permissionInfo.protectionTypeCompat }

    val protectionFlags: Set<ProtectionFlag> by lazy { permissionInfo.protectionFlagsCompat }
}