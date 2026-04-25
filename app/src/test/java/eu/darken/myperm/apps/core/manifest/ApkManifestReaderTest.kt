package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.Pkg
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.BaseTest
import testhelper.binaryxml.AxmlFixtureBuilder
import testhelper.binaryxml.AxmlFixtureBuilder.Attr
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
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
        val result = reader().readQueries("/does/not/exist.apk").shouldBeInstanceOf<QueriesOutcome.Unavailable>()
        result.reason shouldBe UnavailableReason.APK_NOT_FOUND
    }

    @Test
    fun `readQueries returns MALFORMED_APK when manifest entry missing`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, manifestBytes = null)
        val result = reader().readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesOutcome.Unavailable>()
        result.reason shouldBe UnavailableReason.MALFORMED_APK
    }

    @Test
    fun `readQueries returns Success when manifest parses cleanly and arsc is absent`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, manifestBytes = manifestWithQueries("com.foo"), includeArsc = false)
        val result = reader().readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesOutcome.Success>()
        result.info.packageQueries shouldBe listOf("com.foo")
    }

    @Test
    fun `readQueries returns MALFORMED_APK for non-zip file`(@TempDir tempDir: File) {
        val notApk = File(tempDir, "nope.apk").apply { writeText("definitely not a zip") }
        val result = reader().readQueries(notApk.absolutePath).shouldBeInstanceOf<QueriesOutcome.Unavailable>()
        result.reason shouldBe UnavailableReason.MALFORMED_APK
    }

    @Test
    fun `readQueries classifies BinaryXmlException as Error not Malformed`(@TempDir tempDir: File) {
        // Garbage bytes: zip opens fine, manifest entry exists and is readable, but BinaryXmlStreamer
        // rejects the root chunk. That's a parser-layer failure, surfaced as Error (transient) so
        // the scanner retries on the next run and doesn't delete existing hints.
        val apk = writeApk(tempDir, manifestBytes = ByteArray(64) { 0xFF.toByte() })
        reader().readQueries(apk.absolutePath).shouldBeInstanceOf<QueriesOutcome.Failure>()
    }

    @Test
    fun `readFullManifest returns sections and queries in a single pass`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, manifestBytes = manifestWithQueries("com.both"))
        val result = reader().readFullManifest(apk.absolutePath, Pkg.Name("com.other"))
        val sections = result.sections.shouldBeInstanceOf<SectionsResult.Success>()
        val queries = result.queries.shouldBeInstanceOf<QueriesOutcome.Success>()
        sections.sections.map { it.type } shouldContain SectionType.QUERIES
        queries.info.packageQueries shouldBe listOf("com.both")
    }

    // --- readManifestBytes direct edge-case tests ---

    @Test
    fun `readManifestBytes rejects directory entry`() {
        val zip = mockk<ZipFile>()
        // ZipEntry.isDirectory() is final and keyed off a trailing "/" in the name.
        val dirEntry = ZipEntry("AndroidManifest.xml/")
        val ex = shouldThrow<ApkManifestReader.MalformedApkException> {
            reader().readManifestBytes(zip, dirEntry)
        }
        ex.message!!.shouldContain("directory entry")
    }

    @Test
    fun `readManifestBytes rejects zero-size entry`() {
        val zip = mockk<ZipFile>()
        val entry = ZipEntry("AndroidManifest.xml").apply { size = 0L }
        val ex = shouldThrow<ApkManifestReader.MalformedApkException> {
            reader().readManifestBytes(zip, entry)
        }
        ex.message!!.shouldContain("bad manifest size")
    }

    @Test
    fun `readManifestBytes rejects size exceeding Int MAX_VALUE`() {
        val zip = mockk<ZipFile>()
        val entry = ZipEntry("AndroidManifest.xml").apply { size = Int.MAX_VALUE.toLong() + 1L }
        val ex = shouldThrow<ApkManifestReader.MalformedApkException> {
            reader().readManifestBytes(zip, entry)
        }
        ex.message!!.shouldContain("bad manifest size")
    }

    @Test
    fun `readManifestBytes reports short read as malformed`() {
        val zip = mockk<ZipFile>()
        val entry = ZipEntry("AndroidManifest.xml").apply { size = 100L }
        // Only 50 bytes available — the read loop sees -1 before filling the buffer.
        every { zip.getInputStream(entry) } returns ByteArrayInputStream(ByteArray(50))
        val ex = shouldThrow<ApkManifestReader.MalformedApkException> {
            reader().readManifestBytes(zip, entry)
        }
        ex.message!!.shouldContain("short read")
    }

    @Test
    fun `readManifestBytes wraps ZipException from corrupt deflate stream as malformed`() {
        // F5 regression guard: a corrupt DEFLATE stream would previously let ZipException escape
        // the reader and crash the caller. Must be classified as MALFORMED_APK instead.
        val zip = mockk<ZipFile>()
        val entry = ZipEntry("AndroidManifest.xml").apply { size = 10L }
        val throwingStream = object : InputStream() {
            override fun read(): Int = throw ZipException("bad CRC")
            override fun read(b: ByteArray, off: Int, len: Int): Int = throw ZipException("bad CRC")
        }
        every { zip.getInputStream(entry) } returns throwingStream
        val ex = shouldThrow<ApkManifestReader.MalformedApkException> {
            reader().readManifestBytes(zip, entry)
        }
        ex.message!!.shouldContain("zip read failed")
    }

    @Test
    fun `readManifestBytes lets SecurityException propagate for the helper to classify`() {
        // A mid-parse permission revocation can cause getInputStream to throw SecurityException
        // after openApk's preflight has already passed. readManifestBytes intentionally does NOT
        // wrap this — withManifestBytes catches it and classifies as APK_NOT_READABLE.
        val zip = mockk<ZipFile>()
        val entry = ZipEntry("AndroidManifest.xml").apply { size = 10L }
        every { zip.getInputStream(entry) } throws SecurityException("revoked")
        shouldThrow<SecurityException> {
            reader().readManifestBytes(zip, entry)
        }
    }

    @Test
    fun `readManifestBytes wraps generic IOException as malformed`() {
        val zip = mockk<ZipFile>()
        val entry = ZipEntry("AndroidManifest.xml").apply { size = 10L }
        val throwingStream = object : InputStream() {
            override fun read(): Int = throw java.io.IOException("disk hiccup")
            override fun read(b: ByteArray, off: Int, len: Int): Int = throw java.io.IOException("disk hiccup")
        }
        every { zip.getInputStream(entry) } returns throwingStream
        val ex = shouldThrow<ApkManifestReader.MalformedApkException> {
            reader().readManifestBytes(zip, entry)
        }
        ex.message!!.shouldContain("io read failed")
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
