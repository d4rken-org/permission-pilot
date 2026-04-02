package eu.darken.myperm.watcher.core

import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatcherBatteryCapability @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun isBatteryOptimizationIgnored(): Boolean = powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
