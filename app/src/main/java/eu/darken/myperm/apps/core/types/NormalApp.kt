package eu.darken.myperm.apps.core.types

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import eu.darken.myperm.permissions.core.AndroidPermission
import eu.darken.myperm.permissions.core.PermissionId
import java.time.Instant

class NormalApp(
    override val packageInfo: PackageInfo,
    override val label: String?,
    override val requestedPermissions: Collection<UsesPermission>,
    override val declaredPermissions: Collection<PermissionInfo>,
) : BaseApp() {

    var siblings: Set<BaseApp> = emptySet()

    override val id: String
        get() = packageInfo.packageName

    val packageName: String
        get() = packageInfo.packageName

    val applicationInfo: ApplicationInfo?
        get() = packageInfo.applicationInfo

    override val isSystemApp: Boolean
        get() = applicationInfo?.run { flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: true

    override val installedAt: Instant
        get() = Instant.ofEpochMilli(packageInfo.firstInstallTime)

    override val updatedAt: Instant
        get() = Instant.ofEpochMilli(packageInfo.lastUpdateTime)

    override fun requestsPermission(id: PermissionId): Boolean = requestedPermissions.any { it.id == id }

    override fun declaresPermission(id: PermissionId): Boolean = declaredPermissions.any { it.name == id.value }

    override fun getPermission(id: PermissionId): UsesPermission? {
        return requestedPermissions.singleOrNull { it.id == id }
    }

    override val internetAccess: InternetAccess by lazy {
        when {
            getPermission(AndroidPermission.INTERNET)?.isGranted == true -> {
                InternetAccess.DIRECT
            }
            siblings.any { it.getPermission(AndroidPermission.INTERNET)?.isGranted == true } -> {
                InternetAccess.INDIRECT
            }
            else -> InternetAccess.NONE
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NormalApp) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()
}
