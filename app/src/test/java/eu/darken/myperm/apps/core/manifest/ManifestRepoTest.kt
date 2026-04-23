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

    private fun successData(pkgQueries: List<String> = listOf("com.other")): ManifestData = ManifestData(
        rawXml = RawXmlResult.Success("<manifest/>"),
        queries = QueriesResult.Success(QueriesInfo(packageQueries = pkgQueries)),
    )

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
        // JVM unit tests run against the Android stub jar: setLongVersionCode is a no-op
        // and Build.VERSION.SDK_INT returns the default 0. So resolveAppMeta falls through
        // to `pi.versionCode.toLong()` (the public int field), which we set directly.
        val appInfo = ApplicationInfo().apply { sourceDir = apkPath }
        val packageInfo = PackageInfo().apply {
            this.packageName = pkg.value
            this.versionCode = versionCode.toInt()
            this.lastUpdateTime = lastUpdate
            this.applicationInfo = appInfo
        }
        every { packageManager.getPackageInfo(pkg.value, 0) } returns packageInfo
    }

    /**
     * Builds a ManifestRepo whose IO dispatcher and appScope share [scope]'s TestScheduler,
     * so [runTest]'s scheduler fully drives the repo's internal async work.
     */
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
    fun `getQueriesFor returns Success and memory-caches the projection`() = runTest {
        every { apkReader.readManifest(apkPath) } returns successData(pkgQueries = listOf("com.target"))
        val repo = buildRepo(this)

        val outcome = repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Success>()
        outcome.info.packageQueries shouldBe listOf("com.target")

        // Second call hits memory cache.
        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Success>()
        verify(exactly = 1) { apkReader.readManifest(apkPath) }
    }

    @Test
    fun `memory cache entry never references raw XML`() = runTest {
        every { apkReader.readManifest(apkPath) } returns successData()
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg)

        val cacheField = ManifestRepo::class.java.getDeclaredField("memoryCache").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val map = cacheField.get(repo) as Map<Any?, Any?>
        map.isEmpty() shouldBe false
        map.values.forEach { value ->
            (value is QueriesOutcome).shouldBeTrue()
            val text = value.toString()
            (!text.contains("rawXml") && !text.contains("<manifest")).shouldBeTrue()
        }
    }

    @Test
    fun `cache invalidates when versionCode changes`() = runTest {
        every { apkReader.readManifest(apkPath) } returns successData()
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg)

        // Simulate an app update — new versionCode means a new cache key and a new disk entry.
        stubPackageInfo(versionCode = versionCode + 1, lastUpdate = lastUpdate)
        repo.getQueriesFor(pkg)

        verify(exactly = 2) { apkReader.readManifest(apkPath) }
    }

    @Test
    fun `LOW_MEMORY outcome is not memory-cached`() = runTest {
        every { apkReader.readManifest(apkPath) } returns ManifestData(
            rawXml = RawXmlResult.Unavailable(UnavailableReason.LOW_MEMORY),
            queries = QueriesResult.Error(IllegalStateException("low")),
        )
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Unavailable>()
        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Unavailable>()

        verify(exactly = 2) { apkReader.readManifest(apkPath) }
    }

    @Test
    fun `Failure outcome is not memory-cached`() = runTest {
        every { apkReader.readManifest(apkPath) } returns ManifestData(
            rawXml = RawXmlResult.Error(RuntimeException("parser boom")),
            queries = QueriesResult.Error(RuntimeException("parser boom")),
        )
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Failure>()
        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Failure>()

        verify(exactly = 2) { apkReader.readManifest(apkPath) }
    }

    @Test
    fun `APK_TOO_LARGE outcome is memory-cached`() = runTest {
        every { apkReader.readManifest(apkPath) } returns ManifestData(
            rawXml = RawXmlResult.Unavailable(UnavailableReason.APK_TOO_LARGE),
            queries = QueriesResult.Error(IllegalStateException("too large")),
        )
        val repo = buildRepo(this)

        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Unavailable>()
        repo.getQueriesFor(pkg).shouldBeInstanceOf<QueriesOutcome.Unavailable>()

        // Stable until the app updates — avoid reparsing.
        verify(exactly = 1) { apkReader.readManifest(apkPath) }
    }

    @Test
    fun `getManifest and getQueriesFor share single parse for same key`() = runTest {
        every { apkReader.readManifest(apkPath) } returns successData(pkgQueries = listOf("com.shared"))
        val repo = buildRepo(this)

        val a = async { repo.getQueriesFor(pkg) }
        val b = async { repo.getManifest(pkg) }
        a.await()
        b.await()

        verify(exactly = 1) { apkReader.readManifest(apkPath) }
    }
}
