package eu.darken.myperm.permissions.core.types

import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.requestsPermission
import eu.darken.myperm.permissions.core.PermissionId

class UnknownPermission(
    override val id: PermissionId,
    override val label: String? = null,
    override val description: String? = null,
    override val requestingApps: List<BaseApp> = emptyList(),
) : BasePermission() {

    override val grantedApps: Collection<BaseApp>
        get() = requestingApps.filter { it.requestsPermission(this) }

    override val declaringApps: Collection<BaseApp>
        get() = emptyList()

    override val isAospPermission: Boolean
        get() = true

    override fun toString(): String = "NormalPermission($id)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnknownPermission) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()
}