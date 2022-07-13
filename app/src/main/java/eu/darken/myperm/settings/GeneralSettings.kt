package eu.darken.myperm.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.preferences.PreferenceStoreMapper
import eu.darken.myperm.common.preferences.Settings
import eu.darken.myperm.common.preferences.createFlowPreference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneralSettings @Inject constructor(
    @ApplicationContext private val context: Context
) : Settings() {

    override val preferences: SharedPreferences = context.getSharedPreferences("settings_core", Context.MODE_PRIVATE)

    val isBugTrackingEnabled = preferences.createFlowPreference("core.bugtracking.enabled", true)

    override val preferenceDataStore: PreferenceDataStore = PreferenceStoreMapper(
        isBugTrackingEnabled
    )

    companion object {
        internal val TAG = logTag("Core", "Settings")
    }
}