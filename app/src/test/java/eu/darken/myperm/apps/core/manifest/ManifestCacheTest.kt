package eu.darken.myperm.apps.core.manifest

import android.content.Context
import eu.darken.myperm.apps.core.Pkg
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
            rawXml = RawXmlResult.Success("<manifest/>"),
            queries = queries?.let { QueriesResult.Success(it) }
                ?: QueriesResult.Error(IllegalStateException("no queries")),
        )

    @Test
    fun `getQueries returns null on complete miss`() {
        cache.getQueries(pkg, versionCode, lastUpdate) shouldBe null
    }

    @Test
    fun `put writes both files and getQueries prefers sibling`() {
        val queries = QueriesInfo(packageQueries = listOf("com.target"))
        cache.put(pkg, versionCode, lastUpdate, sampleData(queries))

        fullFile().exists() shouldBe true
        queriesFile().exists() shouldBe true

        val result = cache.getQueries(pkg, versionCode, lastUpdate).shouldNotBeNull()
        result.packageQueries shouldBe listOf("com.target")
    }

    @Test
    fun `getQueries sibling file never contains rawXml`() {
        val queries = QueriesInfo(packageQueries = listOf("com.target"))
        cache.put(pkg, versionCode, lastUpdate, sampleData(queries))

        // The sibling file must NOT include rawXml — that's the whole point of the split.
        queriesFile().readText().shouldNotContain("rawXml")
    }

    @Test
    fun `getQueries backfills sibling from existing full cache`() {
        // Simulate pre-upgrade state: only the full cache file exists (no sibling).
        val queries = QueriesInfo(packageQueries = listOf("com.existing"))
        cache.put(pkg, versionCode, lastUpdate, sampleData(queries))
        queriesFile().delete()
        queriesFile().exists() shouldBe false

        val result = cache.getQueries(pkg, versionCode, lastUpdate).shouldNotBeNull()
        result.packageQueries shouldBe listOf("com.existing")

        // Sibling should have been written as a side effect of the backfill.
        queriesFile().exists() shouldBe true
        queriesFile().readText().shouldNotContain("rawXml")
    }

    @Test
    fun `getQueries returns null when sibling stale and full absent`() {
        // Write stale sibling.
        queriesFile().writeText("""{"versionCode":1,"lastUpdateTime":1,"queries":{"packageQueries":[]}}""")

        cache.getQueries(pkg, versionCode, lastUpdate) shouldBe null
        // Stale sibling should be cleaned up.
        queriesFile().exists() shouldBe false
    }

    @Test
    fun `corrupt sibling does not evict valid full file`() {
        cache.put(pkg, versionCode, lastUpdate, sampleData())
        queriesFile().writeText("{not valid json")

        // getQueries should fall back to backfilling from full, NOT delete full.
        cache.getQueries(pkg, versionCode, lastUpdate).shouldNotBeNull()
        fullFile().exists() shouldBe true
    }

    @Test
    fun `corrupt full does not evict valid sibling`() {
        cache.put(pkg, versionCode, lastUpdate, sampleData())
        fullFile().writeText("{not valid json")

        // Full read returns null and deletes corrupted full …
        cache.get(pkg, versionCode, lastUpdate) shouldBe null
        fullFile().exists() shouldBe false
        // … but the sibling is untouched and still serves queries.
        queriesFile().exists() shouldBe true
        cache.getQueries(pkg, versionCode, lastUpdate).shouldNotBeNull()
    }

    @Test
    fun `stale full cache deletes full but not sibling`() {
        cache.put(pkg, versionCode, lastUpdate, sampleData())

        // Caller asks for a different version — stale.
        val result = cache.get(pkg, versionCode = versionCode + 1, lastUpdateTime = lastUpdate)
        result shouldBe null
        fullFile().exists() shouldBe false
        queriesFile().exists() shouldBe true
    }
}
