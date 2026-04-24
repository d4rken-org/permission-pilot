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

    private fun reader(): ApkManifestReader {
        val resolver = mockk<ResourceNameResolver>(relaxed = true)
        return ApkManifestReader(resolver)
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
        val result = reader().readQueries("/does/not/exist.apk").shouldBeInstanceOf<QueriesReadResult.Unavailable>()
        result.reason shouldBe UnavailableReason.APK_NOT_FOUND
    }

    @Test
    fun `readQueries returns MALFORMED_APK when manifest entry missing`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, manifestBytes = null)
        val result = reader().readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesReadResult.Unavailable>()
        result.reason shouldBe UnavailableReason.MALFORMED_APK
    }

    @Test
    fun `readQueries returns Success when manifest parses cleanly and arsc is absent`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, manifestBytes = manifestWithQueries("com.foo"), includeArsc = false)
        val result = reader().readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesReadResult.Success>()
        result.info.packageQueries shouldBe listOf("com.foo")
    }

    @Test
    fun `readQueries returns MALFORMED_APK for non-zip file`(@TempDir tempDir: File) {
        val notApk = File(tempDir, "nope.apk").apply { writeText("definitely not a zip") }
        val result = reader().readQueries(notApk.absolutePath).shouldBeInstanceOf<QueriesReadResult.Unavailable>()
        result.reason shouldBe UnavailableReason.MALFORMED_APK
    }

    @Test
    fun `readQueries classifies BinaryXmlException as Error not Malformed`(@TempDir tempDir: File) {
        // Garbage bytes: zip opens fine, manifest entry exists and is readable, but BinaryXmlStreamer
        // rejects the root chunk. That's a parser-layer failure, surfaced as Error (transient) so
        // the scanner retries on the next run and doesn't delete existing hints.
        val apk = writeApk(tempDir, manifestBytes = ByteArray(64) { 0xFF.toByte() })
        reader().readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesReadResult.Error>()
    }

    @Test
    fun `readFullManifest returns rawXml and queries in a single pass`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, manifestBytes = manifestWithQueries("com.both"))
        val result = reader().readFullManifest(apk.absolutePath, Pkg.Name("com.other"))
        val raw = result.rawXml.shouldBeInstanceOf<RawXmlResult.Success>()
        val queries = result.queries.shouldBeInstanceOf<QueriesResult.Success>()
        raw.xml.shouldContain("<manifest")
        queries.info.packageQueries shouldBe listOf("com.both")
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
