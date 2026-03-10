package eu.darken.myperm.common.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.BaseTest
import testhelper.json.toComparableJson
import java.io.File

class DataStoreValueMoshiTest : BaseTest() {

    @TempDir
    lateinit var tempDir: File

    private val moshi = Moshi.Builder().build()
    private lateinit var testScope: TestScope
    private var dsCounter = 0

    @BeforeEach
    fun setup() {
        testScope = TestScope(UnconfinedTestDispatcher() + Job())
        dsCounter = 0
    }

    private fun createDataStore() = PreferenceDataStoreFactory.create(
        scope = testScope,
        produceFile = { File(tempDir, "test_${dsCounter++}.preferences_pb") }
    )

    @JsonClass(generateAdapter = true)
    data class TestData(
        val string: String = "",
        val boolean: Boolean = true,
        val float: Float = 1.0f,
        val int: Int = 1,
        val long: Long = 1L
    )

    @Test
    fun `round-trip with default value`() = runTest {
        val ds = createDataStore()
        val default = TestData(string = "teststring")
        val pref = ds.createValue("test.moshi", default, moshi)

        pref.value() shouldBe default
        pref.flow.first() shouldBe default
    }

    @Test
    fun `round-trip with updated value`() = runTest {
        val ds = createDataStore()
        val default = TestData(string = "default")
        val pref = ds.createValue("test.moshi", default, moshi)

        val updated = TestData(string = "update", boolean = false, float = 2.5f, int = 42, long = 9999L)
        pref.value(updated)

        pref.value() shouldBe updated
        pref.flow.first() shouldBe updated
    }

    @Test
    fun `JSON output matches expected format`() = runTest {
        val ds = createDataStore()
        val default = TestData(string = "teststring")
        val pref = ds.createValue("test.moshi", default, moshi)

        val updated = TestData(string = "update")
        pref.value(updated)

        // Read the raw JSON string from DataStore
        val rawPrefs = ds.data.first()
        val rawJson = rawPrefs[stringPreferencesKey("test.moshi")] as String

        rawJson.toComparableJson() shouldBe """
            {
                "string":"update",
                "boolean":true,
                "float":1.0,
                "int":1,
                "long":1
            }
        """.toComparableJson()
    }

    @Test
    fun `backward compatibility - pre-populated JSON read by DataStoreValue`() = runTest {
        val ds = createDataStore()

        // Simulate JSON written by the old FlowPreference Moshi writer
        val oldJson = """{"string":"migrated","boolean":false,"float":3.14,"int":7,"long":42}"""
        val spKey = stringPreferencesKey("test.moshi")
        ds.edit { prefs -> prefs[spKey] = oldJson }

        // Now create DataStoreValue and read it
        val pref = ds.createValue("test.moshi", TestData(), moshi)
        val result = pref.value()

        result shouldBe TestData(string = "migrated", boolean = false, float = 3.14f, int = 7, long = 42L)
    }

    @Test
    fun `backward compatibility - JSON format identical between old and new writer`() = runTest {
        // The old FlowPreference system and new DataStoreValue system both use Moshi adapters
        // to produce JSON. Verify the output is character-for-character identical.
        val adapter = moshi.adapter(TestData::class.java)
        val testObj = TestData(string = "compare", boolean = false, float = 2.0f, int = 5, long = 100L)

        // JSON as old FlowPreference would write it
        val oldJson = adapter.toJson(testObj)

        // JSON as new DataStoreValue writes it (through moshiWriter)
        val writer = moshiWriter<TestData>(moshi)
        val newJson = writer(testObj) as String

        oldJson shouldBe newJson

        // Both should round-trip correctly
        val fromOld = adapter.fromJson(oldJson)
        val reader = moshiReader(moshi, TestData(), fallbackToDefault = true)
        val fromNew = reader(newJson)

        fromOld shouldBe testObj
        fromNew shouldBe testObj
        fromOld shouldBe fromNew
    }

    @Test
    fun `malformed JSON with fallbackToDefault returns default`() = runTest {
        val ds = createDataStore()

        // Pre-populate with malformed JSON
        val spKey = stringPreferencesKey("test.moshi")
        ds.edit { prefs -> prefs[spKey] = "{invalid json!!!" }

        val default = TestData(string = "fallback")
        val pref = ds.createValue("test.moshi", default, moshi, fallbackToDefault = true)

        pref.value() shouldBe default
    }

    @Test
    fun `malformed JSON without fallbackToDefault throws`() = runTest {
        val ds = createDataStore()

        // Pre-populate with malformed JSON
        val spKey = stringPreferencesKey("test.moshi")
        ds.edit { prefs -> prefs[spKey] = "{invalid json!!!" }

        val pref = ds.createValue("test.moshi", TestData(), moshi, fallbackToDefault = false)

        shouldThrow<Exception> {
            pref.value()
        }
    }

    @Test
    fun `empty string treated as missing returns default`() = runTest {
        val ds = createDataStore()

        // Pre-populate with empty string - Moshi won't parse this
        val spKey = stringPreferencesKey("test.moshi")
        ds.edit { prefs -> prefs[spKey] = "" }

        val default = TestData(string = "default")
        val pref = ds.createValue("test.moshi", default, moshi, fallbackToDefault = true)

        pref.value() shouldBe default
    }

    @Test
    fun `null value returns default`() = runTest {
        val ds = createDataStore()
        // No pre-population, key doesn't exist → reader receives null → returns default
        val default = TestData(string = "default")
        val pref = ds.createValue("test.moshi", default, moshi)

        pref.value() shouldBe default
    }

    // --- Enum serialization tests ---

    @JsonClass(generateAdapter = false)
    enum class TestEnum {
        @Json(name = "enum.a") A,
        @Json(name = "enum.b") B,
        @Json(name = "enum.c") C
    }

    @Test
    fun `enum round-trip`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.enum", TestEnum.A, moshi)

        pref.value() shouldBe TestEnum.A

        pref.value(TestEnum.B)
        pref.value() shouldBe TestEnum.B

        // Verify raw JSON uses @Json name
        val rawPrefs = ds.data.first()
        val rawJson = rawPrefs[stringPreferencesKey("test.enum")] as String
        rawJson shouldBe "\"enum.b\""
    }

    @Test
    fun `enum backward compatibility - pre-populated JSON value`() = runTest {
        val ds = createDataStore()

        // Simulate JSON written by old FlowPreference for enum
        val spKey = stringPreferencesKey("test.enum")
        ds.edit { prefs -> prefs[spKey] = "\"enum.c\"" }

        val pref = ds.createValue("test.enum", TestEnum.A, moshi)
        pref.value() shouldBe TestEnum.C
    }

    @Test
    fun `unknown enum value with fallbackToDefault returns default`() = runTest {
        val ds = createDataStore()

        // Pre-populate with an unknown enum JSON name
        val spKey = stringPreferencesKey("test.enum")
        ds.edit { prefs -> prefs[spKey] = "\"enum.unknown\"" }

        val pref = ds.createValue("test.enum", TestEnum.A, moshi, fallbackToDefault = true)
        pref.value() shouldBe TestEnum.A
    }

    @Test
    fun `unknown enum value without fallbackToDefault throws`() = runTest {
        val ds = createDataStore()

        // Pre-populate with an unknown enum JSON name
        val spKey = stringPreferencesKey("test.enum")
        ds.edit { prefs -> prefs[spKey] = "\"enum.unknown\"" }

        val pref = ds.createValue("test.enum", TestEnum.A, moshi, fallbackToDefault = false)

        shouldThrow<Exception> {
            pref.value()
        }
    }

    // --- Nullable type test ---

    @Test
    fun `nullable moshi type with null default`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue<TestData?>("test.nullable", null, moshi)

        pref.value() shouldBe null

        val data = TestData(string = "notnull")
        pref.value(data)
        pref.value() shouldBe data

        pref.value(null)
        pref.value() shouldBe null
    }

    // --- Update test ---

    @Test
    fun `update with moshi type`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.moshi", TestData(string = "initial"), moshi)

        pref.update { it.copy(string = "updated", int = 99) }

        pref.value() shouldBe TestData(string = "updated", int = 99)
    }
}
