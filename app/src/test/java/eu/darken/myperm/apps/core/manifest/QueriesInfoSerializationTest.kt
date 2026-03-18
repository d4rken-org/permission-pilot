package eu.darken.myperm.apps.core.manifest

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.json.toComparableJson

class QueriesInfoSerializationTest : BaseTest() {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `full QueriesInfo roundtrips with stable JSON format`() {
        val raw = """
            {
              "packageQueries": ["com.example.app1", "com.example.app2"],
              "intentQueries": [
                {
                  "actions": ["android.intent.action.VIEW"],
                  "dataSpecs": ["scheme=https"],
                  "categories": ["android.intent.category.BROWSABLE"]
                }
              ],
              "providerQueries": ["com.example.provider"]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<QueriesInfo>(raw)

        parsed.packageQueries shouldBe listOf("com.example.app1", "com.example.app2")
        parsed.intentQueries.size shouldBe 1
        parsed.intentQueries[0].actions shouldBe listOf("android.intent.action.VIEW")
        parsed.intentQueries[0].dataSpecs shouldBe listOf("scheme=https")
        parsed.intentQueries[0].categories shouldBe listOf("android.intent.category.BROWSABLE")
        parsed.providerQueries shouldBe listOf("com.example.provider")
        parsed.totalCount shouldBe 4
        parsed.isEmpty shouldBe false

        json.encodeToString(parsed).toComparableJson() shouldBe raw.toComparableJson()
    }

    @Test
    fun `empty QueriesInfo roundtrips with stable JSON format`() {
        val raw = """
            {
              "packageQueries": [],
              "intentQueries": [],
              "providerQueries": []
            }
        """.trimIndent()

        val parsed = json.decodeFromString<QueriesInfo>(raw)

        parsed.packageQueries shouldBe emptyList()
        parsed.intentQueries shouldBe emptyList()
        parsed.providerQueries shouldBe emptyList()
        parsed.totalCount shouldBe 0
        parsed.isEmpty shouldBe true

        json.encodeToString(parsed).toComparableJson() shouldBe raw.toComparableJson()
    }

    @Test
    fun `minimal JSON with defaults omitted deserializes correctly`() {
        val parsed = json.decodeFromString<QueriesInfo>("{}")

        parsed.packageQueries shouldBe emptyList()
        parsed.intentQueries shouldBe emptyList()
        parsed.providerQueries shouldBe emptyList()
        parsed.isEmpty shouldBe true
    }

    @Test
    fun `unknown JSON fields are ignored gracefully`() {
        val raw = """
            {
              "packageQueries": ["com.example.app"],
              "futureField": "some_value",
              "anotherNewThing": 42
            }
        """.trimIndent()

        val parsed = json.decodeFromString<QueriesInfo>(raw)

        parsed.packageQueries shouldBe listOf("com.example.app")
        parsed.intentQueries shouldBe emptyList()
    }

    @Test
    fun `IntentQuery with empty lists roundtrips`() {
        val query = QueriesInfo.IntentQuery()

        val serialized = json.encodeToString(query)
        val restored = json.decodeFromString<QueriesInfo.IntentQuery>(serialized)

        restored.actions shouldBe emptyList()
        restored.dataSpecs shouldBe emptyList()
        restored.categories shouldBe emptyList()
    }

    @Test
    fun `totalCount includes all query types`() {
        val info = QueriesInfo(
            packageQueries = listOf("a", "b"),
            intentQueries = listOf(QueriesInfo.IntentQuery(), QueriesInfo.IntentQuery(), QueriesInfo.IntentQuery()),
            providerQueries = listOf("p"),
        )
        info.totalCount shouldBe 6
    }
}
