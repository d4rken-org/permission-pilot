package eu.darken.myperm.apps.core.types

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import eu.darken.myperm.permissions.core.PermissionId

class NormalApp(
    override val packageInfo: PackageInfo,
    val label: String?,
    override val requestedPermissions: Collection<UsesPermission>,
    override val declaredPermissions: Collection<PermissionInfo>,
) : BaseApp() {

    val packageName: String
        get() = packageInfo.packageName

    val applicationInfo: ApplicationInfo?
        get() = packageInfo.applicationInfo

    override val isSystemApp: Boolean
        get() = applicationInfo?.run { flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: true

    override val id: String
        get() = packageInfo.packageName

    override fun requestsPermission(id: PermissionId): Boolean = requestedPermissions.any { it.id == id }

    override fun declaresPermission(id: PermissionId): Boolean {
        return declaredPermissions.any { it.name == id.value }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NormalApp) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()
}
