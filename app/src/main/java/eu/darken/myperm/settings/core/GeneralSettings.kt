package eu.darken.myperm.settings.core

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.ui.details.AppDetailsFilterOptions
import eu.darken.myperm.apps.ui.list.AppsFilterOptions
import eu.darken.myperm.apps.ui.list.AppsSortOptions
import eu.darken.myperm.common.datastore.createValue
import eu.darken.myperm.common.datastore.moshiReader
import eu.darken.myperm.common.datastore.moshiWriter
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.theming.ThemeColor
import eu.darken.myperm.common.theming.ThemeMode
import eu.darken.myperm.common.theming.ThemeStyle
import eu.darken.myperm.permissions.ui.details.PermissionDetailsFilterOptions
import eu.darken.myperm.permissions.ui.list.PermsFilterOptions
import eu.darken.myperm.permissions.ui.list.PermsSortOptions
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
    private val moshi: Moshi,
) {

    private val dataStore get() = context.dataStore

    val launchCount = dataStore.createValue("core.stats.launches", 0)

    val isOnboardingFinished = dataStore.createValue("core.onboarding.finished", false)

    val themeMode = dataStore.createValue(
        key = stringPreferencesKey("core.ui.theme.mode"),
        reader = moshiReader(moshi, ThemeMode.SYSTEM, fallbackToDefault = true),
        writer = moshiWriter(moshi),
    )
    val themeStyle = dataStore.createValue(
        key = stringPreferencesKey("core.ui.theme.style"),
        reader = moshiReader(moshi, ThemeStyle.DEFAULT, fallbackToDefault = true),
        writer = moshiWriter(moshi),
    )
    val themeColor = dataStore.createValue(
        key = stringPreferencesKey("core.ui.theme.color"),
        reader = moshiReader(moshi, ThemeColor.BLUE, fallbackToDefault = true),
        writer = moshiWriter(moshi),
    )

    val appsFilterOptions = dataStore.createValue(
        key = stringPreferencesKey("apps.list.options.filter"),
        reader = moshiReader(moshi, AppsFilterOptions(), fallbackToDefault = true),
        writer = moshiWriter(moshi),
    )
    val appsSortOptions = dataStore.createValue(
        key = stringPreferencesKey("apps.list.options.sort"),
        reader = moshiReader(moshi, AppsSortOptions(), fallbackToDefault = true),
        writer = moshiWriter(moshi),
    )

    val appDetailsFilterOptions = dataStore.createValue(
        key = stringPreferencesKey("apps.details.options.filter"),
        reader = moshiReader(moshi, AppDetailsFilterOptions(), fallbackToDefault = true),
        writer = moshiWriter(moshi),
    )

    val permissionsFilterOptions = dataStore.createValue(
        key = stringPreferencesKey("permissions.list.options.filter"),
        reader = moshiReader(moshi, PermsFilterOptions(), fallbackToDefault = true),
        writer = moshiWriter(moshi),
    )
    val permissionsSortOptions = dataStore.createValue(
        key = stringPreferencesKey("permissions.list.options.sort"),
        reader = moshiReader(moshi, PermsSortOptions(), fallbackToDefault = true),
        writer = moshiWriter(moshi),
    )

    val permissionDetailsFilterOptions = dataStore.createValue(
        key = stringPreferencesKey("permissions.details.options.filter"),
        reader = moshiReader(moshi, PermissionDetailsFilterOptions(), fallbackToDefault = true),
        writer = moshiWriter(moshi),
    )

    val ipcParallelisation = dataStore.createValue("core.ipc.parallelisation", 0)

    companion object {
        internal val TAG = logTag("Core", "Settings")
    }
}
