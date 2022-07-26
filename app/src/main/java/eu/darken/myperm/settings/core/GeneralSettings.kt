package eu.darken.myperm.settings.core

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceDataStore
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.ui.list.AppsFilterOptions
import eu.darken.myperm.apps.ui.list.AppsSortOptions
import eu.darken.myperm.common.debug.autoreport.DebugSettings
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.preferences.*
import eu.darken.myperm.permissions.ui.list.PermsFilterOptions
import eu.darken.myperm.permissions.ui.list.PermsSortOptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneralSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
    private val debugSettings: DebugSettings,

    ) : Settings() {

    override val preferences: SharedPreferences = context.getSharedPreferences("settings_core", Context.MODE_PRIVATE)

    val appsFilterOptions = preferences.createFlowPreference(
        "apps.list.options.filter",
        moshiReader(moshi, AppsFilterOptions(), fallbackToDefault = true),
        moshiWriter(moshi),
    )
    val appsSortOptions = preferences.createFlowPreference(
        "apps.list.options.sort",
        moshiReader(moshi, AppsSortOptions(), fallbackToDefault = true),
        moshiWriter(moshi),
    )

    val permissionsFilterOptions = preferences.createFlowPreference(
        "permissions.list.options.filter",
        moshiReader(moshi, PermsFilterOptions(), fallbackToDefault = true),
        moshiWriter(moshi),
    )
    val permissionsSortOptions = preferences.createFlowPreference(
        "permissions.list.options.sort",
        moshiReader(moshi, PermsSortOptions(), fallbackToDefault = true),
        moshiWriter(moshi),
    )

    override val preferenceDataStore: PreferenceDataStore = PreferenceStoreMapper(
        debugSettings.isAutoReportingEnabled
    )

    companion object {
        internal val TAG = logTag("Core", "Settings")
    }
}