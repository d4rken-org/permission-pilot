package eu.darken.myperm.apps.core.features

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.permissions.core.known.APerm

data class DeviceAdmin(
    val isActive: Boolean,
)

suspend fun PackageInfo.determineDeviceAdmins(
    ipcFunnel: IPCFunnel,
    activeAdminPkgs: Set<String>,
): List<DeviceAdmin> {
    val pkgInfo = ipcFunnel.packageManager.getPackageInfo(packageName, PackageManager.GET_RECEIVERS)

    return pkgInfo?.receivers
        ?.filter { it.permission == APerm.BIND_DEVICE_ADMIN.id.value }
        ?.map { DeviceAdmin(isActive = activeAdminPkgs.contains(packageName)) }
        ?: emptyList()
}
