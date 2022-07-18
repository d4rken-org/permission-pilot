package eu.darken.myperm.apps.core.container

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import android.os.Process
import android.os.UserHandle
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.*
import eu.darken.myperm.apps.core.pkgId
import eu.darken.myperm.permissions.core.AndroidPermissions
import eu.darken.myperm.permissions.core.Permission
import java.time.Instant

class NormalApp(
    override val packageInfo: PackageInfo,
    override val installerInfo: InstallerInfo,
    val twins: Collection<WorkProfileApp>,
    val userHandle: UserHandle = Process.myUserHandle(),
) : ApkPkg, InstalledApp {

    var siblings: Set<ApkPkg> = emptySet()

    override val id: Pkg.Id by lazy { packageInfo.pkgId }

    override val sharedUserId: String?
        get() = packageInfo.sharedUserId

    val packageName: String
        get() = packageInfo.packageName

    override val isSystemApp: Boolean
        get() = packageInfo.applicationInfo?.run { flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: true

    override val installedAt: Instant
        get() = Instant.ofEpochMilli(packageInfo.firstInstallTime)

    override val updatedAt: Instant
        get() = Instant.ofEpochMilli(packageInfo.lastUpdateTime)

    override val requestedPermissions: Collection<UsesPermission> by lazy {
        packageInfo.requestedPermissions?.mapIndexed { index, permissionId ->
            val flags = packageInfo.requestedPermissionsFlags[index]

            UsesPermission(
                id = Permission.Id(permissionId),
                flags = flags,
            )
        } ?: emptyList()
    }

    override fun requestsPermission(id: Permission.Id): Boolean = requestedPermissions.any { it.id == id }

    override fun getPermission(id: Permission.Id): UsesPermission? {
        return requestedPermissions.singleOrNull { it.id == id }
    }

    override val declaredPermissions: Collection<PermissionInfo> by lazy {
        packageInfo.permissions?.toSet() ?: emptyList()
    }

    override fun declaresPermission(id: Permission.Id): Boolean = declaredPermissions.any { it.name == id.value }

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
