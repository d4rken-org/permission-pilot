package eu.darken.myperm.apps.core.features

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.permissions.core.known.APerm

data class AccessibilityService(
    val isEnabled: Boolean,
    val label: String,
)

suspend fun PackageInfo.determineAccessibilityServices(ipcFunnel: IPCFunnel): List<AccessibilityService> {
    val pkgInfo = ipcFunnel.packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES)

    val enabledAcs =
        ipcFunnel.accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

    return pkgInfo?.services
        ?.filter { it.permission == APerm.BIND_ACCESSIBILITY_SERVICE.id.value }
        ?.map {
            AccessibilityService(
                isEnabled = enabledAcs.any { acs -> acs.resolveInfo.serviceInfo.name == it.name },
                label = it.name ?: it.packageName
            )
        }
        ?: emptyList()
}