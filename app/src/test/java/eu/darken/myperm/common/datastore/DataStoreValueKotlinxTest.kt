package eu.darken.myperm.common.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.darken.myperm.apps.ui.details.AppDetailsFilterOptions
import eu.darken.myperm.apps.ui.list.AppsFilterOptions
import eu.darken.myperm.apps.ui.list.AppsSortOptions
import eu.darken.myperm.common.theming.ThemeColor
import eu.darken.myperm.common.theming.ThemeMode
import eu.darken.myperm.common.theming.ThemeStyle
import eu.darken.myperm.permissions.ui.details.PermissionDetailsFilterOptions
import eu.darken.myperm.permissions.ui.list.PermsFilterOptions
import eu.darken.myperm.permissions.ui.list.PermsSortOptions
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.BaseTest
import testhelper.json.toComparableJson
import java.io.File

class DataStoreValueKotlinxTest : BaseTest() {

    @TempDir
    lateinit var tempDir: File

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }
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

    @Serializable
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
        val pref = ds.createValue("test.kotlinx", default, json)

        pref.value() shouldBe default
        pref.flow.first() shouldBe default
    }

    @Test
    fun `round-trip with updated value`() = runTest {
        val ds = createDataStore()
        val default = TestData(string = "default")
        val pref = ds.createValue("test.kotlinx", default, json)

        val updated = TestData(string = "update", boolean = false, float = 2.5f, int = 42, long = 9999L)
        pref.value(updated)

        pref.value() shouldBe updated
        pref.flow.first() shouldBe updated
    }

    @Test
    fun `JSON output matches expected format`() = runTest {
        val ds = createDataStore()
        val default = TestData(string = "teststring")
        val pref = ds.createValue("test.kotlinx", default, json)

        val updated = TestData(string = "update")
        pref.value(updated)

        // Read the raw JSON string from DataStore
        val rawPrefs = ds.data.first()
        val rawJson = rawPrefs[stringPreferencesKey("test.kotlinx")] as String

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

        // Simulate JSON written by the old Moshi writer
        val oldJson = """{"string":"migrated","boolean":false,"float":3.14,"int":7,"long":42}"""
        val spKey = stringPreferencesKey("test.kotlinx")
        ds.edit { prefs -> prefs[spKey] = oldJson }

        val pref = ds.createValue("test.kotlinx", TestData(), json)
        val result = pref.value()

        result shouldBe TestData(string = "migrated", boolean = false, float = 3.14f, int = 7, long = 42L)
    }

    @Test
    fun `backward compatibility - writer produces JSON readable by reader`() = runTest {
        val testObj = TestData(string = "compare", boolean = false, float = 2.0f, int = 5, long = 100L)

        val writer = kotlinxWriter<TestData>(json)
        val written = writer(testObj) as String

        val reader = kotlinxReader(json, TestData(), fallbackToDefault = true)
        val readBack = reader(written)

        readBack shouldBe testObj
    }

    @Test
    fun `malformed JSON with fallbackToDefault returns default`() = runTest {
        val ds = createDataStore()

        val spKey = stringPreferencesKey("test.kotlinx")
        ds.edit { prefs -> prefs[spKey] = "{invalid json!!!" }

        val default = TestData(string = "fallback")
        val pref = ds.createValue("test.kotlinx", default, json, fallbackToDefault = true)

        pref.value() shouldBe default
    }

    @Test
    fun `malformed JSON without fallbackToDefault throws`() = runTest {
        val ds = createDataStore()

        val spKey = stringPreferencesKey("test.kotlinx")
        ds.edit { prefs -> prefs[spKey] = "{invalid json!!!" }

        val pref = ds.createValue("test.kotlinx", TestData(), json, fallbackToDefault = false)

        shouldThrow<Exception> {
            pref.value()
        }
    }

    @Test
    fun `empty string treated as missing returns default`() = runTest {
        val ds = createDataStore()

        val spKey = stringPreferencesKey("test.kotlinx")
        ds.edit { prefs -> prefs[spKey] = "" }

        val default = TestData(string = "default")
        val pref = ds.createValue("test.kotlinx", default, json, fallbackToDefault = true)

        pref.value() shouldBe default
    }

    @Test
    fun `null value returns default`() = runTest {
        val ds = createDataStore()
        val default = TestData(string = "default")
        val pref = ds.createValue("test.kotlinx", default, json)

        pref.value() shouldBe default
    }

    // --- Enum serialization tests ---

    @Serializable
    enum class TestEnum {
        @SerialName("enum.a") A,
        @SerialName("enum.b") B,
        @SerialName("enum.c") C
    }

    @Test
    fun `enum round-trip`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.enum", TestEnum.A, json)

        pref.value() shouldBe TestEnum.A

        pref.value(TestEnum.B)
        pref.value() shouldBe TestEnum.B

        // Verify raw JSON uses @SerialName
        val rawPrefs = ds.data.first()
        val rawJson = rawPrefs[stringPreferencesKey("test.enum")] as String
        rawJson shouldBe "\"enum.b\""
    }

    @Test
    fun `enum backward compatibility - pre-populated JSON value`() = runTest {
        val ds = createDataStore()

        val spKey = stringPreferencesKey("test.enum")
        ds.edit { prefs -> prefs[spKey] = "\"enum.c\"" }

        val pref = ds.createValue("test.enum", TestEnum.A, json)
        pref.value() shouldBe TestEnum.C
    }

    @Test
    fun `unknown enum value with fallbackToDefault returns default`() = runTest {
        val ds = createDataStore()

        val spKey = stringPreferencesKey("test.enum")
        ds.edit { prefs -> prefs[spKey] = "\"enum.unknown\"" }

        val pref = ds.createValue("test.enum", TestEnum.A, json, fallbackToDefault = true)
        pref.value() shouldBe TestEnum.A
    }

    @Test
    fun `unknown enum value without fallbackToDefault throws`() = runTest {
        val ds = createDataStore()

        val spKey = stringPreferencesKey("test.enum")
        ds.edit { prefs -> prefs[spKey] = "\"enum.unknown\"" }

        val pref = ds.createValue("test.enum", TestEnum.A, json, fallbackToDefault = false)

        shouldThrow<Exception> {
            pref.value()
        }
    }

    // --- Nullable type test ---

    @Test
    fun `nullable type with null default`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue<TestData?>("test.nullable", null, json)

        pref.value() shouldBe null

        val data = TestData(string = "notnull")
        pref.value(data)
        pref.value() shouldBe data

        pref.value(null)
        pref.value() shouldBe null
    }

    // --- Update test ---

    @Test
    fun `update with kotlinx type`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.kotlinx", TestData(string = "initial"), json)

        pref.update { it.copy(string = "updated", int = 99) }

        pref.value() shouldBe TestData(string = "updated", int = 99)
    }

    // --- Golden JSON backward-compatibility tests ---
    // These are the EXACT strings stored by Moshi on user devices.
    // The new kotlinx reader must deserialize them correctly.

    @Nested
    inner class MoshiBackwardCompatibility {

        @Test
        fun `ThemeMode - all Moshi-produced values`() = runTest {
            val ds = createDataStore()

            mapOf(
                "\"theme.mode.system\"" to ThemeMode.SYSTEM,
                "\"theme.mode.dark\"" to ThemeMode.DARK,
                "\"theme.mode.light\"" to ThemeMode.LIGHT,
            ).forEach { (moshiJson, expected) ->
                val spKey = stringPreferencesKey("theme.mode.$expected")
                ds.edit { it[spKey] = moshiJson }
                val pref = ds.createValue("theme.mode.$expected", ThemeMode.SYSTEM, json)
                pref.value() shouldBe expected
            }
        }

        @Test
        fun `ThemeStyle - all Moshi-produced values`() = runTest {
            val ds = createDataStore()

            mapOf(
                "\"theme.style.default\"" to ThemeStyle.DEFAULT,
                "\"theme.style.materialyou\"" to ThemeStyle.MATERIAL_YOU,
                "\"theme.style.mediumcontrast\"" to ThemeStyle.MEDIUM_CONTRAST,
                "\"theme.style.highcontrast\"" to ThemeStyle.HIGH_CONTRAST,
            ).forEach { (moshiJson, expected) ->
                val spKey = stringPreferencesKey("theme.style.$expected")
                ds.edit { it[spKey] = moshiJson }
                val pref = ds.createValue("theme.style.$expected", ThemeStyle.DEFAULT, json)
                pref.value() shouldBe expected
            }
        }

        @Test
        fun `ThemeColor - all Moshi-produced values`() = runTest {
            val ds = createDataStore()

            mapOf(
                "\"theme.color.blue\"" to ThemeColor.BLUE,
                "\"theme.color.green\"" to ThemeColor.GREEN,
                "\"theme.color.amber\"" to ThemeColor.AMBER,
            ).forEach { (moshiJson, expected) ->
                val spKey = stringPreferencesKey("theme.color.$expected")
                ds.edit { it[spKey] = moshiJson }
                val pref = ds.createValue("theme.color.$expected", ThemeColor.BLUE, json)
                pref.value() shouldBe expected
            }
        }

        @Test
        fun `AppsFilterOptions - Moshi golden JSON`() = runTest {
            val ds = createDataStore()
            val moshiJson = """{"filters":["USER_APP","SYSTEM_APP"]}"""
            val spKey = stringPreferencesKey("apps.filter")
            ds.edit { it[spKey] = moshiJson }

            val pref = ds.createValue("apps.filter", AppsFilterOptions(), json)
            pref.value() shouldBe AppsFilterOptions(
                keys = setOf(AppsFilterOptions.Filter.USER_APP, AppsFilterOptions.Filter.SYSTEM_APP)
            )
        }

        @Test
        fun `AppsSortOptions - Moshi golden JSON`() = runTest {
            val ds = createDataStore()
            val moshiJson = """{"mainSort":"UPDATED_AT","reversed":false}"""
            val spKey = stringPreferencesKey("apps.sort")
            ds.edit { it[spKey] = moshiJson }

            val pref = ds.createValue("apps.sort", AppsSortOptions(), json)
            pref.value() shouldBe AppsSortOptions(mainSort = AppsSortOptions.Sort.UPDATED_AT, reversed = false)
        }

        @Test
        fun `AppDetailsFilterOptions - Moshi golden JSON`() = runTest {
            val ds = createDataStore()
            val moshiJson = """{"filters":["GRANTED","DENIED","CONFIGURABLE"]}"""
            val spKey = stringPreferencesKey("appdetails.filter")
            ds.edit { it[spKey] = moshiJson }

            val pref = ds.createValue("appdetails.filter", AppDetailsFilterOptions(), json)
            pref.value() shouldBe AppDetailsFilterOptions(
                keys = setOf(
                    AppDetailsFilterOptions.Filter.GRANTED,
                    AppDetailsFilterOptions.Filter.DENIED,
                    AppDetailsFilterOptions.Filter.CONFIGURABLE,
                )
            )
        }

        @Test
        fun `PermsFilterOptions - Moshi golden JSON`() = runTest {
            val ds = createDataStore()
            val moshiJson = """{"filters":["MANIFEST","SYSTEM","NOT_INSTALLTIME"]}"""
            val spKey = stringPreferencesKey("perms.filter")
            ds.edit { it[spKey] = moshiJson }

            val pref = ds.createValue("perms.filter", PermsFilterOptions(), json)
            pref.value() shouldBe PermsFilterOptions(
                keys = setOf(
                    PermsFilterOptions.Filter.MANIFEST,
                    PermsFilterOptions.Filter.SYSTEM,
                    PermsFilterOptions.Filter.NOT_INSTALLTIME,
                )
            )
        }

        @Test
        fun `PermsSortOptions - Moshi golden JSON`() = runTest {
            val ds = createDataStore()
            val moshiJson = """{"mainSort":"RELEVANCE","reversed":false}"""
            val spKey = stringPreferencesKey("perms.sort")
            ds.edit { it[spKey] = moshiJson }

            val pref = ds.createValue("perms.sort", PermsSortOptions(), json)
            pref.value() shouldBe PermsSortOptions(mainSort = PermsSortOptions.Sort.RELEVANCE, reversed = false)
        }

        @Test
        fun `PermissionDetailsFilterOptions - Moshi golden JSON`() = runTest {
            val ds = createDataStore()
            val moshiJson = """{"filters":["USER_APP","SYSTEM_APP"]}"""
            val spKey = stringPreferencesKey("permdetails.filter")
            ds.edit { it[spKey] = moshiJson }

            val pref = ds.createValue("permdetails.filter", PermissionDetailsFilterOptions(), json)
            pref.value() shouldBe PermissionDetailsFilterOptions(
                keys = setOf(
                    PermissionDetailsFilterOptions.Filter.USER_APP,
                    PermissionDetailsFilterOptions.Filter.SYSTEM_APP,
                )
            )
        }
    }

    // --- Format match tests ---
    // Assert kotlinx writer produces JSON identical to what Moshi produced

    @Nested
    inner class FormatMatch {

        @Test
        fun `ThemeMode writer produces Moshi-compatible JSON`() {
            val writer = kotlinxWriter<ThemeMode>(json)
            writer(ThemeMode.SYSTEM) shouldBe "\"theme.mode.system\""
            writer(ThemeMode.DARK) shouldBe "\"theme.mode.dark\""
            writer(ThemeMode.LIGHT) shouldBe "\"theme.mode.light\""
        }

        @Test
        fun `ThemeStyle writer produces Moshi-compatible JSON`() {
            val writer = kotlinxWriter<ThemeStyle>(json)
            writer(ThemeStyle.DEFAULT) shouldBe "\"theme.style.default\""
            writer(ThemeStyle.MATERIAL_YOU) shouldBe "\"theme.style.materialyou\""
            writer(ThemeStyle.MEDIUM_CONTRAST) shouldBe "\"theme.style.mediumcontrast\""
            writer(ThemeStyle.HIGH_CONTRAST) shouldBe "\"theme.style.highcontrast\""
        }

        @Test
        fun `ThemeColor writer produces Moshi-compatible JSON`() {
            val writer = kotlinxWriter<ThemeColor>(json)
            writer(ThemeColor.BLUE) shouldBe "\"theme.color.blue\""
            writer(ThemeColor.GREEN) shouldBe "\"theme.color.green\""
            writer(ThemeColor.AMBER) shouldBe "\"theme.color.amber\""
        }

        @Test
        fun `AppsSortOptions writer produces Moshi-compatible JSON`() {
            val writer = kotlinxWriter<AppsSortOptions>(json)
            val written = writer(AppsSortOptions()) as String
            written.toComparableJson() shouldBe """{"mainSort":"UPDATED_AT","reversed":false}""".toComparableJson()
        }

        @Test
        fun `PermsSortOptions writer produces Moshi-compatible JSON`() {
            val writer = kotlinxWriter<PermsSortOptions>(json)
            val written = writer(PermsSortOptions()) as String
            written.toComparableJson() shouldBe """{"mainSort":"RELEVANCE","reversed":false}""".toComparableJson()
        }

        @Test
        fun `AppsFilterOptions decoded equality`() {
            val writer = kotlinxWriter<AppsFilterOptions>(json)
            val reader = kotlinxReader(json, AppsFilterOptions(), fallbackToDefault = true)

            val original = AppsFilterOptions(keys = setOf(AppsFilterOptions.Filter.USER_APP, AppsFilterOptions.Filter.SYSTEM_APP))
            val written = writer(original)
            val readBack = reader(written)
            readBack shouldBe original
        }

        @Test
        fun `PermsFilterOptions decoded equality`() {
            val writer = kotlinxWriter<PermsFilterOptions>(json)
            val reader = kotlinxReader(json, PermsFilterOptions(), fallbackToDefault = true)

            val original = PermsFilterOptions(keys = setOf(PermsFilterOptions.Filter.MANIFEST, PermsFilterOptions.Filter.SYSTEM))
            val written = writer(original)
            val readBack = reader(written)
            readBack shouldBe original
        }

        @Test
        fun `PermissionDetailsFilterOptions decoded equality`() {
            val writer = kotlinxWriter<PermissionDetailsFilterOptions>(json)
            val reader = kotlinxReader(json, PermissionDetailsFilterOptions(), fallbackToDefault = true)

            val original = PermissionDetailsFilterOptions()
            val written = writer(original)
            val readBack = reader(written)
            readBack shouldBe original
        }

        @Test
        fun `AppDetailsFilterOptions decoded equality`() {
            val writer = kotlinxWriter<AppDetailsFilterOptions>(json)
            val reader = kotlinxReader(json, AppDetailsFilterOptions(), fallbackToDefault = true)

            val original = AppDetailsFilterOptions()
            val written = writer(original)
            val readBack = reader(written)
            readBack shouldBe original
        }
    }
}
