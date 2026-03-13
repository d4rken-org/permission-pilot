package eu.darken.myperm.settings.core

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.ui.details.AppDetailsFilterOptions
import eu.darken.myperm.apps.ui.list.AppsFilterOptions
import eu.darken.myperm.apps.ui.list.AppsSortOptions
import eu.darken.myperm.common.datastore.createValue
import eu.darken.myperm.common.datastore.kotlinxReader
import eu.darken.myperm.common.datastore.kotlinxWriter
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.theming.ThemeColor
import eu.darken.myperm.watcher.core.WatcherScope
import eu.darken.myperm.common.theming.ThemeMode
import eu.darken.myperm.common.theming.ThemeStyle
import eu.darken.myperm.permissions.ui.details.PermissionDetailsFilterOptions
import eu.darken.myperm.permissions.ui.list.PermsFilterOptions
import eu.darken.myperm.permissions.ui.list.PermsSortOptions
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(
    name = "settings_core",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "settings_core"))
    },
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

@Singleton
class GeneralSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    private val dataStore get() = context.dataStore

    val launchCount = dataStore.createValue("core.stats.launches", 0)

    val isOnboardingFinished = dataStore.createValue("core.onboarding.finished", false)

    val themeMode = dataStore.createValue(
        key = stringPreferencesKey("core.ui.theme.mode"),
        reader = kotlinxReader(json, ThemeMode.SYSTEM, fallbackToDefault = true),
        writer = kotlinxWriter(json),
    )
    val themeStyle = dataStore.createValue(
        key = stringPreferencesKey("core.ui.theme.style"),
        reader = kotlinxReader(json, ThemeStyle.DEFAULT, fallbackToDefault = true),
        writer = kotlinxWriter(json),
    )
    val themeColor = dataStore.createValue(
        key = stringPreferencesKey("core.ui.theme.color"),
        reader = kotlinxReader(json, ThemeColor.BLUE, fallbackToDefault = true),
        writer = kotlinxWriter(json),
    )

    val appsFilterOptions = dataStore.createValue(
        key = stringPreferencesKey("apps.list.options.filter"),
        reader = kotlinxReader(json, AppsFilterOptions(), fallbackToDefault = true),
        writer = kotlinxWriter(json),
    )
    val appsSortOptions = dataStore.createValue(
        key = stringPreferencesKey("apps.list.options.sort"),
        reader = kotlinxReader(json, AppsSortOptions(), fallbackToDefault = true),
        writer = kotlinxWriter(json),
    )

    val appDetailsFilterOptions = dataStore.createValue(
        key = stringPreferencesKey("apps.details.options.filter"),
        reader = kotlinxReader(json, AppDetailsFilterOptions(), fallbackToDefault = true),
        writer = kotlinxWriter(json),
    )

    val permissionsFilterOptions = dataStore.createValue(
        key = stringPreferencesKey("permissions.list.options.filter"),
        reader = kotlinxReader(json, PermsFilterOptions(), fallbackToDefault = true),
        writer = kotlinxWriter(json),
    )
    val permissionsSortOptions = dataStore.createValue(
        key = stringPreferencesKey("permissions.list.options.sort"),
        reader = kotlinxReader(json, PermsSortOptions(), fallbackToDefault = true),
        writer = kotlinxWriter(json),
    )

    val permissionDetailsFilterOptions = dataStore.createValue(
        key = stringPreferencesKey("permissions.details.options.filter"),
        reader = kotlinxReader(json, PermissionDetailsFilterOptions(), fallbackToDefault = true),
        writer = kotlinxWriter(json),
    )

    val isWatcherEnabled = dataStore.createValue("watcher.enabled", false)
    val watcherScope = dataStore.createValue(
        key = stringPreferencesKey("watcher.scope"),
        reader = kotlinxReader(json, WatcherScope.NON_SYSTEM, fallbackToDefault = true),
        writer = kotlinxWriter(json),
    )
    val isWatcherNotificationsEnabled = dataStore.createValue("watcher.notifications.enabled", true)
    val watcherRetentionDays = dataStore.createValue("watcher.retention.days", 30)

    val watcherPollingIntervalHours = dataStore.createValue("watcher.polling.interval.hours", 4)

    val lastDiffedSnapshotId = dataStore.createValue("watcher.lastDiffedSnapshotId", null as String?)

    val ipcParallelisation = dataStore.createValue("core.ipc.parallelisation", 0)

    companion object {
        internal val TAG = logTag("Core", "Settings")
    }
}
