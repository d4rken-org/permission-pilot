package eu.darken.myperm.permissions.core.types

import android.content.pm.PermissionInfo
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.requestsPermission
import eu.darken.myperm.permissions.core.PermissionId

class DeclaredPermission(
    val permission: PermissionInfo,
    override val label: String? = null,
    override val description: String? = null,
    override val requestingApps: List<BaseApp> = emptyList(),
    override val declaringApps: Collection<BaseApp> = emptyList(),
) : BasePermission() {

    override val grantedApps: Collection<BaseApp> by lazy {
        requestingApps
            .filter { it.requestsPermission(this) }
            .filter { it.getPermission(id)?.isGranted == true }
    }

    override val id: PermissionId
        get() = PermissionId(permission.name)

    override val isAospPermission: Boolean
        get() = true

    override fun toString(): String = "DeclaredPermission($id)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeclaredPermission) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()
}