package eu.darken.myperm.apps.core.features

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityManager
import eu.darken.myperm.permissions.core.known.APerm

data class AccessibilityService(
    val isEnabled: Boolean,
    val label: String,
)

fun PackageInfo.determineAccessibilityServices(context: Context): List<AccessibilityService> {
    val pm = context.packageManager
    val acsMan = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES)

    val enabledAcs = acsMan.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

    return pkgInfo.services
        ?.filter { it.permission == APerm.BIND_ACCESSIBILITY_SERVICE.id.value }
        ?.map {
            AccessibilityService(
                isEnabled = enabledAcs.any { acs -> acs.resolveInfo.serviceInfo.name == it.name },
                label = it.name ?: it.packageName
            )
        }
        ?: emptyList()
}