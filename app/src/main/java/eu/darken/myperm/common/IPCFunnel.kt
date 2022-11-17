package eu.darken.myperm.common

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PermissionGroupInfo
import android.content.pm.PermissionInfo
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.getPackageInfo2
import eu.darken.myperm.apps.core.getPermissionInfo2
import eu.darken.myperm.apps.core.tryCreateUserHandle
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.permissions.core.Permission
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IPCFunnel @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val execLock = Semaphore(
        when {
            hasApiLevel(31) -> 3
            hasApiLevel(29) -> 2
            else -> 1
        }.also { log { "IPCFunnel init with parallelization set to $it" } }
    )

    suspend fun <R> execute(block: suspend IPCFunnel.() -> R): R = execLock.withPermit { block(this) }

    val packageManager by lazy { PackageManager2(this) }

    class PackageManager2(private val ipcFunnel: IPCFunnel) {
        private val service = ipcFunnel.context.packageManager
        suspend fun getPackageInfo(packageName: String, flags: Int) = ipcFunnel.execute {
            service.getPackageInfo2(packageName, flags)
        }

        suspend fun getInstalledPackages(flags: Int): List<PackageInfo> = ipcFunnel.execute {
            service.getInstalledPackages(flags)
        }

        suspend fun getInstallSourceInfo(packageName: String) = ipcFunnel.execute {
            service.getInstallSourceInfo(packageName)
        }

        suspend fun getPackageArchiveInfo(path: String, flags: Int) = ipcFunnel.execute {
            service.getPackageArchiveInfo(path, flags)
        }

        @Suppress("DEPRECATION")
        suspend fun getInstallerPackageName(packageName: String) = ipcFunnel.execute {
            service.getInstallerPackageName(packageName)
        }

        suspend fun getAllPermissionGroups(flags: Int): List<PermissionGroupInfo> = ipcFunnel.execute {
            service.getAllPermissionGroups(flags)
        }

        suspend fun queryPermissionsByGroup(packageName: String?, flags: Int): List<PermissionInfo> =
            ipcFunnel.execute {
                service.queryPermissionsByGroup(packageName, flags)
            }

        suspend fun getPermissionInfo2(permissionId: Permission.Id, flags: Int) = ipcFunnel.execute {
            service.getPermissionInfo2(permissionId, flags)
        }
    }


    val powerManager by lazy { PowerManager2(this) }

    class PowerManager2(private val ipcFunnel: IPCFunnel) {
        private val service = ipcFunnel.context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager

        suspend fun isIgnoringBatteryOptimizations(packageName: String) = ipcFunnel.execute {
            service.isIgnoringBatteryOptimizations(packageName)
        }
    }

    val accessibilityManager by lazy { AccessibilityManager2(this) }

    class AccessibilityManager2(private val ipcFunnel: IPCFunnel) {
        private val service =
            ipcFunnel.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager

        suspend fun getEnabledAccessibilityServiceList(flags: Int): List<AccessibilityServiceInfo> =
            service.getEnabledAccessibilityServiceList(flags)
    }

    val launcherApps by lazy { LauncherApps2(this) }

    class LauncherApps2(private val ipcFunnel: IPCFunnel) {
        private val service =
            ContextCompat.getSystemService(ipcFunnel.context, android.content.pm.LauncherApps::class.java)!!

        suspend fun getActivityList(packageName: String?, userHandle: UserHandle): List<LauncherActivityInfo> =
            ipcFunnel.execute {
                service.getActivityList(packageName, userHandle)
            }
    }

    val userManager by lazy { UserManager2(this) }

    class UserManager2(private val ipcFunnel: IPCFunnel) {
        private val service = ContextCompat.getSystemService(ipcFunnel.context, UserManager::class.java)!!

        suspend fun userProfiles(): List<UserHandle> = ipcFunnel.execute {
            service.userProfiles
        }

        suspend fun tryCreateUserHandle(handle: Int) = ipcFunnel.execute {
            service.tryCreateUserHandle(handle)
        }
    }
}