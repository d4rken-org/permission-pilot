package eu.darken.myperm.apps.core.manifest

import android.content.Context
import eu.darken.myperm.apps.core.Pkg
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.BaseTest
import java.io.File

class ManifestCacheTest : BaseTest() {

    @TempDir
    lateinit var cacheRoot: File

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private lateinit var context: Context
    private lateinit var cache: ManifestCache

    private val pkg = Pkg.Name("com.example.app")
    private val versionCode = 42L
    private val lastUpdate = 1_700_000_000_000L

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        every { context.cacheDir } returns cacheRoot
        cache = ManifestCache(context, json)
    }

    private fun manifestsDir(): File = File(cacheRoot, "manifests").also { it.mkdirs() }
    private fun fullFile(): File = File(manifestsDir(), "${pkg.value}.json")
    private fun queriesFile(): File = File(manifestsDir(), "${pkg.value}.queries.json")

    private fun sampleData(queries: QueriesInfo? = QueriesInfo(packageQueries = listOf("com.other"))): ManifestData =
        ManifestData(
            sections = SectionsResult.Success(
                listOf(
                    ManifestSection(
                        type = SectionType.OTHER,
                        elementCount = 1,
                        prettyXml = "<application />",
                        isFlagged = false,
                    ),
                ),
            ),
            queries = queries?.let { QueriesOutcome.Success(it) }
                ?: QueriesOutcome.Failure(IllegalStateException("no queries")),
        )

    @Test
    fun `getQueries returns null on complete miss`() {
        cache.getQueries(pkg, versionCode, lastUpdate) shouldBe null
    }

    @Test
    fun `put writes both files and getQueries hits sibling`() {
        val queries = QueriesInfo(packageQueries = listOf("com.target"))
        cache.put(pkg, versionCode, lastUpdate, sampleData(queries))

        fullFile().exists() shouldBe true
        queriesFile().exists() shouldBe true

        val result = cache.getQueries(pkg, versionCode, lastUpdate).shouldNotBeNull()
        result.packageQueries shouldBe listOf("com.target")
    }

    @Test
    fun `sibling file holds queries only — no section payload`() {
        val queries = QueriesInfo(packageQueries = listOf("com.target"))
        cache.put(pkg, versionCode, lastUpdate, sampleData(queries))

        val text = queriesFile().readText()
        text.shouldNotContain("prettyXml")
        text.shouldNotContain("sections")
    }

    @Test
    fun `getQueries returns null when full cache present but sibling missing`() {
        // Backfill path was removed — a full-cache-only state must NOT populate queries.
        val queries = QueriesInfo(packageQueries = listOf("com.existing"))
        cache.put(pkg, versionCode, lastUpdate, sampleData(queries))
        queriesFile().delete()
        queriesFile().exists() shouldBe false

        cache.getQueries(pkg, versionCode, lastUpdate) shouldBe null
        // No side-effect write should happen either.
        queriesFile().exists() shouldBe false
    }

    @Test
    fun `putQueries writes only the sibling`() {
        val queries = QueriesInfo(packageQueries = listOf("com.target"))
        cache.putQueries(pkg, versionCode, lastUpdate, queries)

        queriesFile().exists() shouldBe true
        fullFile().exists() shouldBe false

        val result = cache.getQueries(pkg, versionCode, lastUpdate).shouldNotBeNull()
        result.packageQueries shouldBe listOf("com.target")
    }

    @Test
    fun `raw pre-v2 JSON without formatVersion is deleted on read`() {
        // A pre-migration entry has no formatVersion field at all.
        val rawJson = """
            {
              "versionCode": 42,
              "lastUpdateTime": 1700000000000,
              "rawXml": "<old/>",
              "queries": null
            }
        """.trimIndent()
        fullFile().writeText(rawJson)

        val result = cache.get(pkg, versionCode = 42L, lastUpdateTime = 1_700_000_000_000L)
        result.shouldBeNull()
        fullFile().exists() shouldBe false
    }

    @Test
    fun `wrong formatVersion is deleted on read`() {
        val rawJson = """
            {
              "formatVersion": 1,
              "versionCode": 42,
              "lastUpdateTime": 1700000000000,
              "sections": [],
              "queries": null
            }
        """.trimIndent()
        fullFile().writeText(rawJson)

        val result = cache.get(pkg, versionCode = 42L, lastUpdateTime = 1_700_000_000_000L)
        result.shouldBeNull()
        fullFile().exists() shouldBe false
    }

    @Test
    fun `v2 entry with rawXml is deleted on v3 read — migration via decode failure`() {
        // v2 format had `rawXml: String` (required) and no `sections` field. Decoding under
        // v3's schema fails because `sections` is required and unset; the corruption catch
        // deletes the file.
        val rawJson = """
            {
              "formatVersion": 2,
              "versionCode": 42,
              "lastUpdateTime": 1700000000000,
              "rawXml": "<old/>",
              "queries": null
            }
        """.trimIndent()
        fullFile().writeText(rawJson)

        val result = cache.get(pkg, versionCode = 42L, lastUpdateTime = 1_700_000_000_000L)
        result.shouldBeNull()
        fullFile().exists() shouldBe false
    }

    @Test
    fun `valid sibling survives v2 full-cache deletion during migration`() {
        cache.putQueries(pkg, versionCode, lastUpdate, QueriesInfo(packageQueries = listOf("x")))
        fullFile().writeText(
            """
                {
                  "formatVersion": 2,
                  "versionCode": 42,
                  "lastUpdateTime": 1700000000000,
                  "rawXml": "<old/>",
                  "queries": null
                }
            """.trimIndent()
        )

        cache.get(pkg, versionCode, lastUpdate) shouldBe null
        fullFile().exists() shouldBe false
        cache.getQueries(pkg, versionCode, lastUpdate)!!.packageQueries shouldBe listOf("x")
    }

    @Test
    fun `queries sibling is preserved when full cache is pre-v2 and deleted`() {
        // Write a valid sibling.
        cache.putQueries(pkg, versionCode, lastUpdate, QueriesInfo(packageQueries = listOf("x")))
        // Drop a pre-v2 full file next to it.
        fullFile().writeText(
            """
                {
                  "versionCode": 42,
                  "lastUpdateTime": 1700000000000,
                  "rawXml": "<old/>",
                  "queries": null
                }
            """.trimIndent()
        )

        cache.get(pkg, versionCode, lastUpdate) shouldBe null
        fullFile().exists() shouldBe false
        // Sibling still valid.
        cache.getQueries(pkg, versionCode, lastUpdate)!!.packageQueries shouldBe listOf("x")
    }

    @Test
    fun `getQueries returns null when sibling stale`() {
        queriesFile().writeText("""{"versionCode":1,"lastUpdateTime":1,"queries":{"packageQueries":[]}}""")

        cache.getQueries(pkg, versionCode, lastUpdate) shouldBe null
        queriesFile().exists() shouldBe false
    }

    @Test
    fun `corrupt sibling does not evict valid full file`() {
        cache.put(pkg, versionCode, lastUpdate, sampleData())
        queriesFile().writeText("{not valid json")

        // getQueries deletes the corrupt sibling and returns null (no backfill).
        cache.getQueries(pkg, versionCode, lastUpdate) shouldBe null
        queriesFile().exists() shouldBe false
        // Full file untouched.
        fullFile().exists() shouldBe true
    }

    @Test
    fun `corrupt full does not evict valid sibling`() {
        cache.put(pkg, versionCode, lastUpdate, sampleData())
        fullFile().writeText("{not valid json")

        cache.get(pkg, versionCode, lastUpdate) shouldBe null
        fullFile().exists() shouldBe false
        queriesFile().exists() shouldBe true
        cache.getQueries(pkg, versionCode, lastUpdate).shouldNotBeNull()
    }

    @Test
    fun `stale full cache deletes full but not sibling`() {
        cache.put(pkg, versionCode, lastUpdate, sampleData())

        val result = cache.get(pkg, versionCode = versionCode + 1, lastUpdateTime = lastUpdate)
        result shouldBe null
        fullFile().exists() shouldBe false
        queriesFile().exists() shouldBe true
    }
}
