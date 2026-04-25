package eu.darken.myperm.apps.core.manifest

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import eu.darken.myperm.apps.core.Pkg
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider
import java.io.File

class ManifestRepoTest : BaseTest() {

    @TempDir
    lateinit var cacheRoot: File

    private val pkg = Pkg.Name("com.example.app")
    private val versionCode = 42L
    private val lastUpdate = 1_700_000_000_000L
    private val apkPath = "/data/app/com.example.app-1/base.apk"

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var apkReader: ApkManifestReader
    private lateinit var cache: ManifestCache

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun successFullData(queries: List<String> = listOf("com.other")): ManifestData = ManifestData(
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
        queries = QueriesOutcome.Success(QueriesInfo(packageQueries = queries)),
    )

    private fun successQueries(queries: List<String> = listOf("com.other")): QueriesOutcome.Success =
        QueriesOutcome.Success(QueriesInfo(packageQueries = queries))

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        every { context.cacheDir } returns cacheRoot

        packageManager = mockk(relaxed = true)
        every { context.packageManager } returns packageManager

        stubPackageInfo(versionCode, lastUpdate)

        apkReader = mockk()
        cache = ManifestCache(context, json)
    }

    @Suppress("DEPRECATION")
    private fun stubPackageInfo(versionCode: Long, lastUpdate: Long) {
        val appInfo = ApplicationInfo().apply { sourceDir = apkPath }
        val packageInfo = PackageInfo().apply {
            this.packageName = pkg.value
            this.versionCode = versionCode.toInt()
            this.lastUpdateTime = lastUpdate
            this.applicationInfo = appInfo
        }
        every { packageManager.getPackageInfo(pkg.value, 0) } returns packageInfo
    }

    private fun buildRepo(scope: TestScope): ManifestRepo {
        val sharedDispatcher = UnconfinedTestDispatcher(scope.testScheduler)
        val dispatcherProvider = TestDispatcherProvider(sharedDispatcher)
        return ManifestRepo(
            apkManifestReader = apkReader,
            manifestCache = cache,
            dispatcherProvider = dispatcherProvider,
            appScope = scope as CoroutineScope,
            context = context,
        )
    }

    @Test
    fun `getQueriesFor calls readQueries not readFullManifest`() = runTest {
        every { apkReader.readQueries(apkPath) } returns successQueries(listOf("com.target"))
        val repo = buildRepo(this)

        val outcome = repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Success>()
        outcome.info.packageQueries shouldBe listOf("com.target")

        verify(exactly = 1) { apkReader.readQueries(apkPath) }
        verify(exactly = 0) { apkReader.readFullManifest(any(), any()) }
    }

    @Test
    fun `getManifest calls readFullManifest not readQueries`() = runTest {
        every { apkReader.readFullManifest(apkPath, pkg) } returns successFullData()
        val repo = buildRepo(this)

        val data = repo.getManifest(pkg)
        data.sections.shouldBeInstanceOf<SectionsResult.Success>()

        verify(exactly = 1) { apkReader.readFullManifest(apkPath, pkg) }
        verify(exactly = 0) { apkReader.readQueries(any()) }
    }

    @Test
    fun `getQueriesFor memory-caches successful projection`() = runTest {
        every { apkReader.readQueries(apkPath) } returns successQueries(listOf("com.target"))
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg)
        repo.getQueriesFor(pkg)

        verify(exactly = 1) { apkReader.readQueries(apkPath) }
    }

    @Test
    fun `memory cache entry holds only the queries projection — no section payload`() = runTest {
        every { apkReader.readQueries(apkPath) } returns successQueries()
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg)

        val cacheField = ManifestRepo::class.java.getDeclaredField("memoryCache").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val map = cacheField.get(repo) as Map<Any?, Any?>
        map.isEmpty() shouldBe false
        map.values.forEach { value ->
            (value is QueriesOutcome).shouldBeTrue()
            val text = value.toString()
            (!text.contains("prettyXml") && !text.contains("<application")).shouldBeTrue()
        }
    }

    @Test
    fun `cache invalidates when versionCode changes`() = runTest {
        every { apkReader.readQueries(apkPath) } returns successQueries()
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg)

        stubPackageInfo(versionCode = versionCode + 1, lastUpdate = lastUpdate)
        repo.getQueriesFor(pkg)

        verify(exactly = 2) { apkReader.readQueries(apkPath) }
    }

    @Test
    fun `LOW_MEMORY outcome is not memory-cached`() = runTest {
        every { apkReader.readQueries(apkPath) } returns QueriesOutcome.Unavailable(UnavailableReason.LOW_MEMORY)
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Unavailable>()
        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Unavailable>()

        verify(exactly = 2) { apkReader.readQueries(apkPath) }
    }

    @Test
    fun `Failure outcome is not memory-cached`() = runTest {
        every { apkReader.readQueries(apkPath) } returns QueriesOutcome.Failure(RuntimeException("parser boom"))
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Failure>()
        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Failure>()

        verify(exactly = 2) { apkReader.readQueries(apkPath) }
    }

    @Test
    fun `MALFORMED_APK outcome is memory-cached`() = runTest {
        every { apkReader.readQueries(apkPath) } returns QueriesOutcome.Unavailable(UnavailableReason.MALFORMED_APK)
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Unavailable>()
        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Unavailable>()

        // Stable outcome — cached until the app updates.
        verify(exactly = 1) { apkReader.readQueries(apkPath) }
    }

    @Test
    fun `viewer starts first then queries caller - single parse shared`() = runTest {
        every { apkReader.readFullManifest(apkPath, pkg) } returns successFullData(listOf("com.shared"))
        val repo = buildRepo(this)

        val viewer = async { repo.getManifest(pkg) }
        val scanner = async { repo.getQueriesFor(pkg) }
        viewer.await()
        scanner.await()

        verify(exactly = 1) { apkReader.readFullManifest(apkPath, pkg) }
        verify(exactly = 0) { apkReader.readQueries(any()) }
    }

    @Test
    fun `queries-only path does not trigger full parse`() = runTest {
        every { apkReader.readQueries(apkPath) } returns successQueries(listOf("com.x"))
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg)
        repo.getQueriesFor(pkg)

        verify(exactly = 0) { apkReader.readFullManifest(any(), any()) }
    }

    @Test
    fun `getQueriesFor writes only the sibling cache`() = runTest {
        every { apkReader.readQueries(apkPath) } returns successQueries(listOf("com.target"))
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg)

        File(cacheRoot, "manifests/${pkg.value}.queries.json").exists() shouldBe true
        File(cacheRoot, "manifests/${pkg.value}.json").exists() shouldBe false
    }

    @Test
    fun `getManifest writes both cache files`() = runTest {
        every { apkReader.readFullManifest(apkPath, pkg) } returns successFullData(listOf("com.target"))
        val repo = buildRepo(this)

        repo.getManifest(pkg)

        File(cacheRoot, "manifests/${pkg.value}.json").exists() shouldBe true
        File(cacheRoot, "manifests/${pkg.value}.queries.json").exists() shouldBe true
    }
}
