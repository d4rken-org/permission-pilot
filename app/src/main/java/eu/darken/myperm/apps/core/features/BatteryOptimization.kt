package eu.darken.myperm.apps.core.features

import android.content.pm.PackageInfo
import androidx.room.TypeConverter
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.hasApiLevel
import eu.darken.myperm.permissions.core.known.APerm

enum class BatteryOptimization {
    IGNORED,
    OPTIMIZED,
    MANAGED_BY_SYSTEM,
    UNKNOWN;

    class Converter {
        @TypeConverter
        fun fromEnum(value: BatteryOptimization): String = value.name

        @TypeConverter
        fun toEnum(value: String): BatteryOptimization = valueOf(value)
    }
}

suspend fun PackageInfo.determineBatteryOptimization(ipcFunnel: IPCFunnel): BatteryOptimization {
    if (!hasApiLevel(23)) return BatteryOptimization.IGNORED
    val permissions = requestedPermissions ?: return BatteryOptimization.MANAGED_BY_SYSTEM
    if (permissions.none { it == APerm.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.id.value }) {
        return BatteryOptimization.MANAGED_BY_SYSTEM
    }

    return if (ipcFunnel.powerManager.isIgnoringBatteryOptimizations(packageName)) {
        BatteryOptimization.IGNORED
    } else {
        BatteryOptimization.OPTIMIZED
    }
}