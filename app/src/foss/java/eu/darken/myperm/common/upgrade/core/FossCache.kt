package eu.darken.myperm.common.upgrade.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.datastore.createValue
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(
    name = "settings_foss",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "settings_foss"))
    },
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

@Singleton
class FossCache internal constructor(
    // Test seam: the store is handed in so a test can supply its own DataStore instead of the
    // Context-bound production delegate.
    private val dataStore: DataStore<Preferences>,
    json: Json,
) {

    @Inject constructor(
        @ApplicationContext context: Context,
        json: Json,
    ) : this(context.dataStore, json)

    val upgrade = dataStore.createValue<FossUpgrade?>(
        keyName = "foss.upgrade",
        json = json,
        defaultValue = null,
    )

}
