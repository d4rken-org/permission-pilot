package eu.darken.myperm.common.upgrade.core

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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

    // Product id of the last confirmed Pro purchase; determines the grace window length.
    val lastProStateSku = context.dataStore.createValue(
        "gplay.cache.lastProSku",
        ""
    )

    // Both values in one transaction: a process death or interleaved writer must not pair a fresh
    // timestamp with a stale SKU, they select the grace window together.
    suspend fun confirmPro(at: Long, sku: String) {
        context.dataStore.edit { prefs ->
            @Suppress("UNCHECKED_CAST")
            prefs[lastProStateAt.key as Preferences.Key<Long>] = at
            @Suppress("UNCHECKED_CAST")
            prefs[lastProStateSku.key as Preferences.Key<String>] = sku
        }
    }
}
