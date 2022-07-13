package eu.darken.myperm.permissions.core.types

import android.content.pm.PermissionInfo
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.requestsPermission

class NormalPermission(
    val permission: PermissionInfo,
    val label: String? = null,
    val description: String? = null,
    override val requestingApps: List<BaseApp> = emptyList(),
) : BasePermission() {

    override val grantedApps: Collection<BaseApp>
        get() = requestingApps.filter { it.requestsPermission(this) }

    override val id: String
        get() = permission.name
}