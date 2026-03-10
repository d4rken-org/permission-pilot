package eu.darken.myperm.common.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.Moshi
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.log

inline fun <reified T> moshiReader(
    moshi: Moshi,
    defaultValue: T,
    fallbackToDefault: Boolean = false,
): (Any?) -> T {
    val adapter = moshi.adapter(T::class.java)
    return { rawValue ->
        rawValue as String?
        rawValue
            ?.let {
                try {
                    adapter.fromJson(it)
                } catch (e: Exception) {
                    log("Moshi", ERROR) { "Failed to decode rawValue=$rawValue, returning default=$defaultValue" }
                    if (fallbackToDefault) null else throw e
                }
            }
            ?: defaultValue
    }
}

inline fun <reified T> moshiWriter(
    moshi: Moshi,
): (T) -> Any? {
    val adapter = moshi.adapter(T::class.java)
    return { newValue: T ->
        newValue?.let { adapter.toJson(it) }
    }
}

inline fun <reified T : Any?> DataStore<Preferences>.createValue(
    keyName: String,
    defaultValue: T = null as T,
    moshi: Moshi,
    fallbackToDefault: Boolean = true,
): DataStoreValue<T> = DataStoreValue(
    dataStore = this,
    key = stringPreferencesKey(keyName),
    reader = moshiReader(moshi, defaultValue, fallbackToDefault),
    writer = moshiWriter(moshi),
)
