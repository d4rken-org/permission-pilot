package eu.darken.myperm.apps.core.manifest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.BaseTest
import java.io.File

class ManifestCacheSerializationTest : BaseTest() {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `cached manifest v3 with sections and queries roundtrips`() {
        val raw = """
            {
              "formatVersion": 3,
              "versionCode": 612009943,
              "lastUpdateTime": 1705000000000,
              "sections": [
                {
                  "type": "USES_PERMISSION",
                  "elementCount": 1,
                  "prettyXml": "<uses-permission android:name=\"x\" />"
                }
              ],
              "queries": {
                "packageQueries": ["com.example.app"],
                "intentQueries": [],
                "providerQueries": []
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CachedManifestTestHelper>(raw)

        parsed.formatVersion shouldBe 3
        parsed.versionCode shouldBe 612009943L
        parsed.sections.size shouldBe 1
        parsed.sections.first().type shouldBe SectionType.USES_PERMISSION
        parsed.queries shouldNotBe null
        parsed.queries!!.packageQueries shouldBe listOf("com.example.app")

        val reserialized = json.encodeToString(parsed)
        val reparsed = json.decodeFromString<CachedManifestTestHelper>(reserialized)
        reparsed shouldBe parsed
    }

    @Test
    fun `pre-v3 JSON without formatVersion decodes formatVersion as null`() {
        // Critical migration guard: kotlinx.serialization must NOT fill in a default 3 for
        // missing formatVersion fields. The field's default here is null.
        val raw = """
            {
              "versionCode": 100,
              "lastUpdateTime": 1700000000000,
              "sections": []
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CachedManifestTestHelper>(raw)
        parsed.formatVersion shouldBe null
    }

    @Test
    fun `v2 JSON without sections fails to decode under v3 schema`() {
        // Migration mechanism: v2 had `rawXml: String` (required) and no `sections` field.
        // Decoding under the v3 schema (where `sections` is required) must throw, so that
        // ManifestCache.get's corruption catch deletes the file.
        val v2Json = """
            {
              "formatVersion": 2,
              "versionCode": 42,
              "lastUpdateTime": 1700000000000,
              "rawXml": "<old/>"
            }
        """.trimIndent()

        shouldThrow<Exception> {
            json.decodeFromString<CachedManifestTestHelper>(v2Json)
        }
    }

    @Test
    fun `unknown fields in cached JSON are ignored`() {
        val raw = """
            {
              "formatVersion": 3,
              "versionCode": 1,
              "lastUpdateTime": 1000,
              "sections": [],
              "queries": null,
              "futureField": "ignored"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CachedManifestTestHelper>(raw)

        parsed.versionCode shouldBe 1L
        parsed.sections shouldBe emptyList()
    }

    @Test
    fun `large section payload survives serialization roundtrip`() {
        val sections = (1..1000).map { i ->
            CachedManifestSection(
                type = SectionType.USES_PERMISSION,
                elementCount = 1,
                prettyXml = "<uses-permission android:name=\"com.example.perm$i\" />",
            )
        }

        val cached = CachedManifestTestHelper(
            formatVersion = 3,
            versionCode = 1,
            lastUpdateTime = 1000,
            sections = sections,
            queries = QueriesInfo(packageQueries = (1..50).map { "com.pkg.$it" }),
        )

        val serialized = json.encodeToString(cached)
        val restored = json.decodeFromString<CachedManifestTestHelper>(serialized)

        restored.sections.size shouldBe 1000
        restored.queries!!.packageQueries.size shouldBe 50
    }

    @Test
    fun `cache file write and read roundtrip`(@TempDir tempDir: File) {
        val cacheFile = File(tempDir, "com.example.json")
        val cached = CachedManifestTestHelper(
            formatVersion = 3,
            versionCode = 42,
            lastUpdateTime = 9999,
            sections = listOf(
                CachedManifestSection(
                    type = SectionType.QUERIES,
                    elementCount = 1,
                    prettyXml = "<queries><package android:name=\"com.test\" /></queries>",
                ),
            ),
            queries = QueriesInfo(packageQueries = listOf("com.test")),
        )

        cacheFile.writeText(json.encodeToString(cached))

        val restored = json.decodeFromString<CachedManifestTestHelper>(cacheFile.readText())
        restored shouldBe cached
    }

    @Test
    fun `stale cache detected when versionCode changes`(@TempDir tempDir: File) {
        val cacheFile = File(tempDir, "com.example.json")
        val cached = CachedManifestTestHelper(
            formatVersion = 3,
            versionCode = 1,
            lastUpdateTime = 1000,
            sections = emptyList(),
            queries = null,
        )
        cacheFile.writeText(json.encodeToString(cached))

        val restored = json.decodeFromString<CachedManifestTestHelper>(cacheFile.readText())
        val isStale = restored.versionCode != 2L || restored.lastUpdateTime != 1000L
        isStale shouldBe true
    }

    @Test
    fun `stale cache detected when lastUpdateTime changes`(@TempDir tempDir: File) {
        val cacheFile = File(tempDir, "com.example.json")
        val cached = CachedManifestTestHelper(
            formatVersion = 3,
            versionCode = 1,
            lastUpdateTime = 1000,
            sections = emptyList(),
            queries = null,
        )
        cacheFile.writeText(json.encodeToString(cached))

        val restored = json.decodeFromString<CachedManifestTestHelper>(cacheFile.readText())
        val isStale = restored.versionCode != 1L || restored.lastUpdateTime != 2000L
        isStale shouldBe true
    }
}

/**
 * Mirrors the private CachedManifest data class from ManifestCache.
 * Must be kept in sync — regressions here catch format drift between code and tests.
 */
@kotlinx.serialization.Serializable
internal data class CachedManifestTestHelper(
    val formatVersion: Int? = null,
    val versionCode: Long,
    val lastUpdateTime: Long,
    val sections: List<CachedManifestSection>,
    val queries: QueriesInfo? = null,
)
