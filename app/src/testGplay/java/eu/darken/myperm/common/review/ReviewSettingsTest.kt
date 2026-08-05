package eu.darken.myperm.common.review

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import testhelper.BaseTest
import testhelpers.TestApplication
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Real DataStore, no mocks: the injected [Json] carries no serializers module, so the timestamps are
 * wired to the ISO serializer by hand — an encode/decode mismatch would only show up on the first
 * real write.
 *
 * One [ReviewSettings] per test method: the production settings object is a `@Singleton` whose
 * DataStore scope is never cancelled, and a second instance on the same file fails DataStore's
 * multi-instance check. What a fresh process would read is covered by going through the raw
 * preference entry instead — writes are asserted on the stored string, reads are fed one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class ReviewSettingsTest : BaseTest() {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun settings() = ReviewSettings(ApplicationProvider.getApplicationContext<Context>(), json)

    private val dismissedKey = stringPreferencesKey("review.dismissedAt")

    @Test
    fun `an unwritten timestamp reads as null`() = runTest {
        withContext(Dispatchers.IO) {
            val settings = settings()

            settings.lastDismissed.value() shouldBe null
            settings.reviewedAt.value() shouldBe null
        }
    }

    @Test
    fun `a written timestamp is stored as an ISO string`() = runTest {
        withContext(Dispatchers.IO) {
            val settings = settings()
            val stamp = Instant.parse("2026-08-05T12:34:56.789Z")

            settings.lastDismissed.value(stamp)

            settings.lastDismissed.value() shouldBe stamp
            // What a fresh process would find on disk: the ISO text, not a numeric epoch.
            settings.dataStore.data.first()[dismissedKey] shouldBe "\"2026-08-05T12:34:56.789Z\""
            // The two keys are independent.
            settings.reviewedAt.value() shouldBe null
        }
    }

    @Test
    fun `a stored ISO string is read back as an Instant`() = runTest {
        withContext(Dispatchers.IO) {
            val settings = settings()
            // What a previous process left behind.
            settings.dataStore.edit { it[dismissedKey] = "\"2026-08-05T12:34:56.789Z\"" }

            settings.lastDismissed.value() shouldBe Instant.parse("2026-08-05T12:34:56.789Z")
        }
    }

    @Test
    fun `a value written back over an existing one replaces it`() = runTest {
        withContext(Dispatchers.IO) {
            val settings = settings()

            settings.reviewedAt.value(Instant.parse("2026-01-01T00:00:00Z"))
            settings.reviewedAt.value(Instant.parse("2026-02-02T00:00:00Z"))

            settings.reviewedAt.value() shouldBe Instant.parse("2026-02-02T00:00:00Z")
        }
    }

    @Test
    fun `an unparsable stored string fails the read`() = runTest {
        withContext(Dispatchers.IO) {
            val settings = settings()
            settings.dataStore.edit { it[dismissedKey] = "not-even-json" }

            // Deliberately not a fallback-to-default: a timestamp we cannot read is not the same as
            // "never dismissed", and defaulting would silently re-ask the user forever.
            shouldThrow<SerializationException> { settings.lastDismissed.value() }
        }
    }

    @Test
    fun `a stored string that is not a timestamp fails the read`() = runTest {
        withContext(Dispatchers.IO) {
            val settings = settings()
            settings.dataStore.edit { it[dismissedKey] = "\"yesterday\"" }

            shouldThrow<DateTimeParseException> { settings.lastDismissed.value() }
        }
    }
}
