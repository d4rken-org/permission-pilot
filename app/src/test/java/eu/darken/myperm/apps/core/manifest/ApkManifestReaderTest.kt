package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.Pkg
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.BaseTest
import testhelper.binaryxml.AxmlFixtureBuilder
import testhelper.binaryxml.AxmlFixtureBuilder.Attr
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkManifestReaderTest : BaseTest() {

    private fun readerWithFakeHeap(maxHeap: Long, freeHeap: Long): ApkManifestReader {
        val resolver = mockk<ResourceNameResolver>(relaxed = true)
        return ApkManifestReader(resolver).also {
            it.heapInfoProvider = { ApkManifestReader.HeapInfo(maxHeap = maxHeap, freeHeap = freeHeap) }
        }
    }

    private fun writeApk(
        parent: File,
        name: String = "fake.apk",
        manifestBytes: ByteArray? = simpleManifest(),
        includeArsc: Boolean = true,
    ): File {
        val apk = File(parent, name)
        ZipOutputStream(apk.outputStream()).use { zip ->
            if (includeArsc) {
                zip.putNextEntry(ZipEntry("resources.arsc"))
                zip.write(ByteArray(16))
                zip.closeEntry()
            }
            if (manifestBytes != null) {
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                zip.write(manifestBytes)
                zip.closeEntry()
            }
        }
        return apk
    }

    private fun simpleManifest(): ByteArray {
        val b = AxmlFixtureBuilder()
        return b
            .startNamespace("android", ANDROID_NS)
            .startElement(
                namespace = null,
                name = "manifest",
                attributes = listOf(
                    Attr(ANDROID_NS, "package", type = AxmlFixtureBuilder.RES_TYPE_STRING, data = 0, rawValueString = "com.example")
                )
            )
            .endElement(null, "manifest")
            .endNamespace("android", ANDROID_NS)
            .build()
    }

    private fun manifestWithQueries(pkg: String = "com.target"): ByteArray {
        val b = AxmlFixtureBuilder()
        fun strAttr(ns: String?, name: String, v: String) =
            Attr(ns, name, type = AxmlFixtureBuilder.RES_TYPE_STRING, data = 0, rawValueString = v)
        return b
            .startNamespace("android", ANDROID_NS)
            .startElement(null, "manifest")
            .startElement(null, "queries")
            .startElement(null, "package", attributes = listOf(strAttr(ANDROID_NS, "name", pkg)))
            .endElement(null, "package")
            .endElement(null, "queries")
            .endElement(null, "manifest")
            .endNamespace("android", ANDROID_NS)
            .build()
    }

    @Test
    fun `readQueries returns APK_NOT_FOUND when file missing`() {
        val reader = readerWithFakeHeap(maxHeap = 100_000_000L, freeHeap = 95_000_000L)
        val result = reader.readQueries("/does/not/exist.apk").shouldBeInstanceOf<QueriesReadResult.Unavailable>()
        result.reason shouldBe UnavailableReason.APK_NOT_FOUND
    }

    @Test
    fun `readQueries returns LOW_MEMORY when entry heap gate fails`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir)
        val reader = readerWithFakeHeap(maxHeap = 100_000_000L, freeHeap = 5_000_000L) // <10%
        val result = reader.readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesReadResult.Unavailable>()
        result.reason shouldBe UnavailableReason.LOW_MEMORY
    }

    @Test
    fun `readQueries returns MALFORMED_APK when manifest entry missing`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, manifestBytes = null)
        val reader = readerWithFakeHeap(maxHeap = 512_000_000L, freeHeap = 500_000_000L)
        val result = reader.readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesReadResult.Unavailable>()
        result.reason shouldBe UnavailableReason.MALFORMED_APK
    }

    @Test
    fun `readQueries returns Success when manifest parses cleanly - arsc absent is OK`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, manifestBytes = manifestWithQueries("com.foo"), includeArsc = false)
        val reader = readerWithFakeHeap(maxHeap = 512_000_000L, freeHeap = 500_000_000L)

        val result = reader.readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesReadResult.Success>()
        result.info.packageQueries shouldBe listOf("com.foo")
    }

    @Test
    fun `readQueries returns MALFORMED_APK for non-zip file`(@TempDir tempDir: File) {
        val notApk = File(tempDir, "nope.apk").apply { writeText("definitely not a zip") }
        val reader = readerWithFakeHeap(maxHeap = 512_000_000L, freeHeap = 500_000_000L)

        val result = reader.readQueries(notApk.absolutePath).shouldBeInstanceOf<QueriesReadResult.Unavailable>()
        result.reason shouldBe UnavailableReason.MALFORMED_APK
    }

    @Test
    fun `readQueries returns APK_TOO_LARGE when estimated peak exceeds budget`(@TempDir tempDir: File) {
        // Manifest * 4 multiplier + slack must exceed maxHeap * 0.25.
        val bigManifest = ByteArray(20 * 1024 * 1024)  // 20 MB of garbage — zip will fail parse, but preflight comes first.
        val apk = writeApk(tempDir, manifestBytes = bigManifest)
        val reader = readerWithFakeHeap(maxHeap = 100_000_000L, freeHeap = 95_000_000L)

        val result = reader.readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesReadResult.Unavailable>()
        result.reason shouldBe UnavailableReason.APK_TOO_LARGE
    }

    @Test
    fun `readQueries classifies BinaryXmlException as Error not Malformed`(@TempDir tempDir: File) {
        // Garbage bytes: preflight passes (non-empty), but BinaryXmlStreamer will fail on the root chunk.
        val apk = writeApk(tempDir, manifestBytes = ByteArray(64) { 0xFF.toByte() })
        val reader = readerWithFakeHeap(maxHeap = 512_000_000L, freeHeap = 500_000_000L)

        // Parser errors are surfaced as Error (transient), NOT Unavailable(MALFORMED_APK).
        // The caller path in ManifestHintRepo treats Error (Failure outcome) as retry-on-next-scan
        // without deleting stale hints, whereas MALFORMED_APK would delete them.
        reader.readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesReadResult.Error>()
    }

    @Test
    fun `readFullManifest returns rawXml and queries in a single pass`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, manifestBytes = manifestWithQueries("com.both"))
        val reader = readerWithFakeHeap(maxHeap = 512_000_000L, freeHeap = 500_000_000L)

        val result = reader.readFullManifest(apk.absolutePath, Pkg.Name("com.other"))
        val raw = result.rawXml.shouldBeInstanceOf<RawXmlResult.Success>()
        val queries = result.queries.shouldBeInstanceOf<QueriesResult.Success>()
        raw.xml.shouldContain("<manifest")
        queries.info.packageQueries shouldBe listOf("com.both")
    }

    @Test
    fun `readFullManifest preflight uses higher budget than readQueries`(@TempDir tempDir: File) {
        // Size the manifest so queries budget (4x) fits but full budget (6x) exceeds 25% heap.
        val manifestSize = 5 * 1024 * 1024  // 5 MB
        val junk = ByteArray(manifestSize)
        val apk = writeApk(tempDir, manifestBytes = junk)
        // maxHeap = 120 MB → budget = 30 MB.
        // queries peak = 5 * 4 + 2 = 22 MB < 30 MB → passes preflight (parse then fails).
        // full peak    = 5 * 6 + 2 = 32 MB > 30 MB → APK_TOO_LARGE.
        val reader = readerWithFakeHeap(maxHeap = 120_000_000L, freeHeap = 110_000_000L)

        val full = reader.readFullManifest(apk.absolutePath, Pkg.Name("com.x"))
        val unavailable = full.rawXml.shouldBeInstanceOf<RawXmlResult.Unavailable>()
        unavailable.reason shouldBe UnavailableReason.APK_TOO_LARGE
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
