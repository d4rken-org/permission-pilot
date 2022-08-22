package eu.darken.myperm.apps.core.features

import android.content.Context
import android.content.pm.PackageInfo
import android.os.PowerManager
import eu.darken.myperm.common.hasApiLevel
import eu.darken.myperm.permissions.core.known.APerm

enum class BatteryOptimization {
    IGNORED,
    OPTIMIZED,
    MANAGED_BY_SYSTEM,
    UNKNOWN,
}

fun PackageInfo.determineBatteryOptimization(context: Context): BatteryOptimization {
    if (!hasApiLevel(23)) return BatteryOptimization.IGNORED
    if (requestedPermissions == null) return BatteryOptimization.MANAGED_BY_SYSTEM
    if (requestedPermissions.none { it == APerm.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.id.value }) {
        return BatteryOptimization.MANAGED_BY_SYSTEM
    }

    val pwrm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    return if (pwrm.isIgnoringBatteryOptimizations(packageName)) {
        BatteryOptimization.IGNORED
    } else {
        BatteryOptimization.OPTIMIZED
    }
}