package eu.darken.myperm.common.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.log
import kotlinx.serialization.json.Json

inline fun <reified T> kotlinxReader(
    json: Json,
    defaultValue: T,
    fallbackToDefault: Boolean = false,
): (Any?) -> T {
    return { rawValue ->
        (rawValue as? String)
            ?.let {
                try {
                    json.decodeFromString<T>(it)
                } catch (e: Exception) {
                    log("Kotlinx", ERROR) { "Failed to decode rawValue=$rawValue, returning default=$defaultValue" }
                    if (fallbackToDefault) null else throw e
                }
            }
            ?: defaultValue
    }
}

inline fun <reified T> kotlinxWriter(
    json: Json,
): (T) -> Any? {
    return { newValue: T ->
        newValue?.let { json.encodeToString<T>(it) }
    }
}

inline fun <reified T : Any?> DataStore<Preferences>.createValue(
    keyName: String,
    defaultValue: T = null as T,
    json: Json,
    fallbackToDefault: Boolean = true,
): DataStoreValue<T> = DataStoreValue(
    dataStore = this,
    key = stringPreferencesKey(keyName),
    reader = kotlinxReader(json, defaultValue, fallbackToDefault),
    writer = kotlinxWriter(json),
)
