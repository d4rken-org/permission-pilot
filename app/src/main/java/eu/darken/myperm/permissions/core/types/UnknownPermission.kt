package eu.darken.myperm.permissions.core.types

import eu.darken.myperm.apps.core.features.HasApkData
import eu.darken.myperm.apps.core.features.requestsPermission
import eu.darken.myperm.permissions.core.Permission

data class UnknownPermission(
    override val id: Permission.Id,
    override val requestingPkgs: List<HasApkData> = emptyList(),
) : BasePermission() {

    override val grantingPkgs: Collection<HasApkData> by lazy {
        requestingPkgs
            .filter { it.requestsPermission(this) }
            .filter { it.getPermission(id)?.isGranted == true }
    }

    override val declaringPkgs: Collection<HasApkData>
        get() = emptyList()

    override fun toString(): String = "NormalPermission($id)"
}