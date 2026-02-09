package eu.darken.myperm.common.preferences

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.json.toComparableJson
import testhelpers.preferences.MockSharedPreferences

class FlowPreferenceMoshiTest : BaseTest() {

    private val mockPreferences = MockSharedPreferences()

    @JsonClass(generateAdapter = true)
    data class TestGson(
        val string: String = "",
        val boolean: Boolean = true,
        val float: Float = 1.0f,
        val int: Int = 1,
        val long: Long = 1L
    )

    @Test
    fun `reading and writing using manual reader and writer`() = runTest {
        val testData1 = TestGson(string = "teststring")
        val testData2 = TestGson(string = "update")
        val moshi = Moshi.Builder().build()
        FlowPreference<TestGson?>(
            preferences = mockPreferences,
            key = "testKey",
            rawReader = moshiReader(moshi, testData1),
            rawWriter = moshiWriter(moshi)
        ).apply {
            value shouldBe testData1
            flow.first() shouldBe testData1
            mockPreferences.dataMapPeek.values.isEmpty() shouldBe true

            update {
                it shouldBe testData1
                it!!.copy(string = "update")
            }

            value shouldBe testData2
            flow.first() shouldBe testData2
            (mockPreferences.dataMapPeek.values.first() as String).toComparableJson() shouldBe """
                {
                    "string":"update",
                    "boolean":true,
                    "float":1.0,
                    "int":1,
                    "long":1
                }
            """.toComparableJson()

            update {
                it shouldBe testData2
                null
            }
            value shouldBe testData1
            flow.first() shouldBe testData1
            mockPreferences.dataMapPeek.values.isEmpty() shouldBe true
        }
    }

    @Test
    fun `reading and writing using autocreated reader and writer`() = runTest {
        val testData1 = TestGson(string = "teststring")
        val testData2 = TestGson(string = "update")
        val moshi = Moshi.Builder().build()

        mockPreferences.createFlowPreference<TestGson?>(
            key = "testKey",
            defaultValue = testData1,
            moshi = moshi
        ).apply {
            value shouldBe testData1
            flow.first() shouldBe testData1
            mockPreferences.dataMapPeek.values.isEmpty() shouldBe true

            update {
                it shouldBe testData1
                it!!.copy(string = "update")
            }

            value shouldBe testData2
            flow.first() shouldBe testData2
            (mockPreferences.dataMapPeek.values.first() as String).toComparableJson() shouldBe """
                {
                    "string":"update",
                    "boolean":true,
                    "float":1.0,
                    "int":1,
                    "long":1
                }
            """.toComparableJson()

            update {
                it shouldBe testData2
                null
            }
            value shouldBe testData1
            flow.first() shouldBe testData1
            mockPreferences.dataMapPeek.values.isEmpty() shouldBe true
        }
    }

    enum class Anum {
        @Json(name = "a") A,
        @Json(name = "b") B
    }

    @Test
    fun `enum serialization`() = runTest {
        val moshi = Moshi.Builder().build()
        val monitorMode = mockPreferences.createFlowPreference(
            "test.enum",
            Anum.A,
            moshi
        )

        monitorMode.value shouldBe Anum.A
        monitorMode.update { Anum.B }
        monitorMode.value shouldBe Anum.B
    }
}
