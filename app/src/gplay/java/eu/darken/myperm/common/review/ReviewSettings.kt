package eu.darken.myperm.common.review

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.common.datastore.createValue
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.serialization.InstantSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    private val Context.dataStore by preferencesDataStore(name = "settings_review_gplay")

    val dataStore: DataStore<Preferences>
        get() = context.dataStore

    // The injected Json has no serializers module, so the reified `Instant?` lookup the kotlinx
    // createValue overload does would fail on the first write. The ISO serializer is wired
    // explicitly instead. A stored value that fails to decode throws: a timestamp we can't read is
    // not the same as "never dismissed", and silently defaulting would re-ask the user forever.
    private fun instantValue(keyName: String) = dataStore.createValue<Instant?>(
        key = stringPreferencesKey(keyName),
        reader = { rawValue -> (rawValue as? String)?.let { json.decodeFromString(InstantSerializer.nullable, it) } },
        writer = { value -> value?.let { json.encodeToString(InstantSerializer.nullable, it) } },
    )

    val lastDismissed = instantValue("review.dismissedAt")
    val reviewedAt = instantValue("review.reviewedAt")

    companion object {
        internal val TAG = logTag("Review", "Settings", "Gplay")
    }
}
