package eu.darken.myperm.common.upgrade.core

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.datastore.createValue
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
class FossCache @Inject constructor(
    @ApplicationContext context: Context,
    moshi: Moshi
) {

    val upgrade = context.dataStore.createValue<FossUpgrade?>(
        keyName = "foss.upgrade",
        moshi = moshi,
        defaultValue = null,
    )

}
