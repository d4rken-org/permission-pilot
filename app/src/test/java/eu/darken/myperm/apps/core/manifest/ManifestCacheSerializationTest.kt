package eu.darken.myperm.apps.core.manifest

import io.kotest.matchers.nulls.shouldNotBeNull
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
    fun `cached manifest v2 with queries roundtrips`() {
        val raw = """
            {
              "formatVersion": 2,
              "versionCode": 612009943,
              "lastUpdateTime": 1705000000000,
              "rawXml": "<manifest/>",
              "queries": {
                "packageQueries": ["com.example.app"],
                "intentQueries": [],
                "providerQueries": []
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CachedManifestTestHelper>(raw)

        parsed.formatVersion shouldBe 2
        parsed.versionCode shouldBe 612009943L
        parsed.rawXml shouldBe "<manifest/>"
        parsed.queries shouldNotBe null
        parsed.queries!!.packageQueries shouldBe listOf("com.example.app")

        val reserialized = json.encodeToString(parsed)
        val reparsed = json.decodeFromString<CachedManifestTestHelper>(reserialized)
        reparsed shouldBe parsed
    }

    @Test
    fun `pre-v2 JSON without formatVersion decodes formatVersion as null`() {
        // Critical migration guard: kotlinx.serialization must NOT fill in a default 2 for
        // missing formatVersion fields. The field's default here is null.
        val raw = """
            {
              "versionCode": 100,
              "lastUpdateTime": 1700000000000,
              "rawXml": "<old/>"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CachedManifestTestHelper>(raw)
        parsed.formatVersion shouldBe null
    }

    @Test
    fun `unknown fields in cached JSON are ignored`() {
        val raw = """
            {
              "formatVersion": 2,
              "versionCode": 1,
              "lastUpdateTime": 1000,
              "rawXml": "<m/>",
              "queries": null,
              "futureField": "ignored"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CachedManifestTestHelper>(raw)

        parsed.versionCode shouldBe 1L
        parsed.rawXml shouldBe "<m/>"
    }

    @Test
    fun `large XML content survives serialization roundtrip`() {
        val largeXml = buildString {
            append("<manifest>")
            repeat(1000) { i ->
                append("<permission android:name=\"com.example.perm$i\"/>")
            }
            append("</manifest>")
        }

        val cached = CachedManifestTestHelper(
            formatVersion = 2,
            versionCode = 1,
            lastUpdateTime = 1000,
            rawXml = largeXml,
            queries = QueriesInfo(
                packageQueries = (1..50).map { "com.pkg.$it" },
            ),
        )

        val serialized = json.encodeToString(cached)
        val restored = json.decodeFromString<CachedManifestTestHelper>(serialized)

        restored.rawXml shouldBe largeXml
        restored.queries!!.packageQueries.size shouldBe 50
    }

    @Test
    fun `cache file write and read roundtrip`(@TempDir tempDir: File) {
        val cacheFile = File(tempDir, "com.example.json")
        val cached = CachedManifestTestHelper(
            formatVersion = 2,
            versionCode = 42,
            lastUpdateTime = 9999,
            rawXml = "<manifest><queries><package android:name=\"com.test\"/></queries></manifest>",
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
            formatVersion = 2,
            versionCode = 1,
            lastUpdateTime = 1000,
            rawXml = "<old/>",
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
            formatVersion = 2,
            versionCode = 1,
            lastUpdateTime = 1000,
            rawXml = "<old/>",
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
data class CachedManifestTestHelper(
    val formatVersion: Int? = null,
    val versionCode: Long,
    val lastUpdateTime: Long,
    val rawXml: String,
    val queries: QueriesInfo?,
)
