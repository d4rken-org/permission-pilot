package eu.darken.myperm.common.preferences

import android.content.SharedPreferences
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

inline fun <reified T : Any?> SharedPreferences.createFlowPreference(
    key: String,
    defaultValue: T = null as T,
    moshi: Moshi,
) = FlowPreference(
    preferences = this,
    key = key,
    rawReader = moshiReader(moshi, defaultValue),
    rawWriter = moshiWriter(moshi)
)