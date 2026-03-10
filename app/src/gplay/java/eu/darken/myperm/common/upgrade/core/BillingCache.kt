package eu.darken.myperm.common.upgrade.core

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.datastore.createValue
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(
    name = "settings_gplay",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "settings_gplay"))
    },
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

@Singleton
class BillingCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val lastProStateAt = context.dataStore.createValue(
        "gplay.cache.lastProAt",
        0L
    )
}
