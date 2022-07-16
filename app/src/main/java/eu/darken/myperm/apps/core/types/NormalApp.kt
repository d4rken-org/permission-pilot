package eu.darken.myperm.apps.core.types

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import eu.darken.myperm.apps.core.InstallerInfo
import eu.darken.myperm.apps.core.InternetAccess
import eu.darken.myperm.apps.core.UsesPermission
import eu.darken.myperm.permissions.core.AndroidPermissions
import eu.darken.myperm.permissions.core.Permission
import java.time.Instant

class NormalApp(
    override val packageInfo: PackageInfo,
    override val label: String?,
    override val requestedPermissions: Collection<UsesPermission>,
    override val declaredPermissions: Collection<PermissionInfo>,
    override val installerInfo: InstallerInfo?,
) : BaseApp() {

    var siblings: Set<BaseApp> = emptySet()

    override val sharedUserId: String?
        get() = packageInfo.sharedUserId

    override val id: Pkg.Id by lazy {
        Pkg.Id(packageInfo.packageName)
    }

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

    override fun requestsPermission(id: Permission.Id): Boolean = requestedPermissions.any { it.id == id }

    override fun declaresPermission(id: Permission.Id): Boolean = declaredPermissions.any { it.name == id.value }

    override fun getPermission(id: Permission.Id): UsesPermission? {
        return requestedPermissions.singleOrNull { it.id == id }
    }

    override val internetAccess: InternetAccess by lazy {
        when {
            isSystemApp || getPermission(AndroidPermissions.INTERNET.id)?.isGranted == true -> InternetAccess.DIRECT
            siblings.any { it.getPermission(AndroidPermissions.INTERNET.id)?.isGranted == true } -> InternetAccess.INDIRECT
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
