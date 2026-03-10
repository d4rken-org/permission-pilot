package eu.darken.myperm.common.debug.autoreport

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.datastore.createValue
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(
    name = "debug_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "debug_settings"))
    },
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

@Singleton
class DebugSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val isAutoReportingEnabled = context.dataStore.createValue(
        keyName = "debug.bugreport.automatic.enabled",
        // Reporting is opt-out for gplay, and opt-in for github builds
        defaultValue = BuildConfigWrap.FLAVOR == BuildConfigWrap.Flavor.GPLAY
    )

}
