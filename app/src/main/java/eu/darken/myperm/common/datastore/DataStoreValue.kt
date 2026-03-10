package eu.darken.myperm.common.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.darken.myperm.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.myperm.common.debug.logging.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException

class DataStoreValue<T>(
    private val dataStore: DataStore<Preferences>,
    val key: Preferences.Key<*>,
    private val reader: (rawValue: Any?) -> T,
    private val writer: (T) -> Any?,
) {

    val flow: Flow<T> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                log(VERBOSE) { "IOException reading DataStore key=${key.name}, emitting defaults" }
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw e
            }
        }
        .map { prefs -> reader(prefs[key]) }
        .distinctUntilChanged()

    suspend fun value(): T = flow.first()

    suspend fun value(newValue: T) {
        update { newValue }
    }

    suspend fun update(transform: (T) -> T) {
        dataStore.edit { prefs ->
            val current = reader(prefs[key])
            val newValue = transform(current)
            val raw = writer(newValue)
            @Suppress("UNCHECKED_CAST")
            if (raw != null) {
                prefs[key as Preferences.Key<Any>] = raw
            } else {
                prefs.remove(key)
            }
        }
    }

    var valueBlocking: T
        get() = runBlocking { value() }
        set(newValue) = runBlocking { value(newValue) }
}

inline fun <reified T> basicKey(keyName: String, defaultValue: T): Preferences.Key<*> = when (defaultValue) {
    is Boolean -> booleanPreferencesKey(keyName)
    is Int -> intPreferencesKey(keyName)
    is Long -> longPreferencesKey(keyName)
    is Float -> floatPreferencesKey(keyName)
    is String -> stringPreferencesKey(keyName)
    else -> throw NotImplementedError("No basic key for type ${T::class}")
}

inline fun <reified T> basicReader(defaultValue: T): (rawValue: Any?) -> T = { rawValue ->
    (rawValue ?: defaultValue) as T
}

inline fun <reified T> basicWriter(): (T) -> Any? = { value ->
    when (value) {
        is Boolean -> value
        is String -> value
        is Int -> value
        is Long -> value
        is Float -> value
        null -> null
        else -> throw NotImplementedError("No basic writer for type ${value!!::class}")
    }
}

inline fun <reified T> DataStore<Preferences>.createValue(
    keyName: String,
    defaultValue: T,
): DataStoreValue<T> = DataStoreValue(
    dataStore = this,
    key = basicKey(keyName, defaultValue),
    reader = basicReader(defaultValue),
    writer = basicWriter(),
)

fun <T> DataStore<Preferences>.createValue(
    key: Preferences.Key<*>,
    reader: (rawValue: Any?) -> T,
    writer: (T) -> Any?,
): DataStoreValue<T> = DataStoreValue(
    dataStore = this,
    key = key,
    reader = reader,
    writer = writer,
)
