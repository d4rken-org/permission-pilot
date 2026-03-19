package eu.darken.myperm.permissions.core.container

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.room.entity.PkgType

data class PermissionAppRef(
    val pkgName: Pkg.Name,
    val userHandleId: Int,
    val label: String,
    val isSystemApp: Boolean,
    val status: UsesPermission.Status,
    val pkgType: PkgType,
)
