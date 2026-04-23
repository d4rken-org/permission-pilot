package eu.darken.myperm.apps.core.manifest

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.BaseTest
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkManifestReaderTest : BaseTest() {

    private fun ApkManifestReader.setHeap(maxHeap: Long, freeHeap: Long) {
        heapInfoProvider = { ApkManifestReader.HeapInfo(maxHeap = maxHeap, freeHeap = freeHeap) }
    }

    private fun writeApk(
        parent: File,
        name: String = "fake.apk",
        arscBytes: ByteArray? = ByteArray(16),
        manifestBytes: ByteArray? = ByteArray(16),
    ): File {
        val apk = File(parent, name)
        ZipOutputStream(apk.outputStream()).use { zip ->
            if (arscBytes != null) {
                zip.putNextEntry(ZipEntry("resources.arsc"))
                zip.write(arscBytes)
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

    @Test
    fun `returns APK_NOT_FOUND when file missing`() {
        val reader = ApkManifestReader()
        val result = reader.readManifest("/does/not/exist.apk")

        val raw = result.rawXml.shouldBeInstanceOf<RawXmlResult.Unavailable>()
        raw.reason shouldBe UnavailableReason.APK_NOT_FOUND
    }

    @Test
    fun `returns LOW_MEMORY when entry heap gate fails`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir)
        val reader = ApkManifestReader().also {
            // freeHeap = 10% of max < 20% entry threshold
            it.setHeap(maxHeap = 100_000_000L, freeHeap = 10_000_000L)
        }

        val result = reader.readManifest(apk.absolutePath)

        val raw = result.rawXml.shouldBeInstanceOf<RawXmlResult.Unavailable>()
        raw.reason shouldBe UnavailableReason.LOW_MEMORY
    }

    @Test
    fun `returns MALFORMED_APK when resources_arsc missing`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, arscBytes = null)
        val reader = ApkManifestReader().also {
            it.setHeap(maxHeap = 512_000_000L, freeHeap = 500_000_000L)
        }

        val result = reader.readManifest(apk.absolutePath)

        val raw = result.rawXml.shouldBeInstanceOf<RawXmlResult.Unavailable>()
        raw.reason shouldBe UnavailableReason.MALFORMED_APK
    }

    @Test
    fun `returns MALFORMED_APK when AndroidManifest_xml missing`(@TempDir tempDir: File) {
        val apk = writeApk(tempDir, manifestBytes = null)
        val reader = ApkManifestReader().also {
            it.setHeap(maxHeap = 512_000_000L, freeHeap = 500_000_000L)
        }

        val result = reader.readManifest(apk.absolutePath)

        val raw = result.rawXml.shouldBeInstanceOf<RawXmlResult.Unavailable>()
        raw.reason shouldBe UnavailableReason.MALFORMED_APK
    }

    @Test
    fun `returns MALFORMED_APK for non-zip file`(@TempDir tempDir: File) {
        val notApk = File(tempDir, "not_an_apk.apk").apply { writeText("definitely not a zip") }
        val reader = ApkManifestReader().also {
            it.setHeap(maxHeap = 512_000_000L, freeHeap = 500_000_000L)
        }

        val result = reader.readManifest(notApk.absolutePath)

        val raw = result.rawXml.shouldBeInstanceOf<RawXmlResult.Unavailable>()
        raw.reason shouldBe UnavailableReason.MALFORMED_APK
    }

    @Test
    fun `returns APK_TOO_LARGE when estimated peak exceeds budget`(@TempDir tempDir: File) {
        // Synthesize a zip where resources.arsc is 30 MB. With budget = maxHeap * 0.25
        // and maxHeap = 100 MB, budget is 25 MB. arscSize * 3 = 90 MB > 25 MB → reject.
        val oversizedArsc = ByteArray(30 * 1024 * 1024) { 0 }
        val apk = writeApk(tempDir, arscBytes = oversizedArsc)
        val reader = ApkManifestReader().also {
            it.setHeap(maxHeap = 100_000_000L, freeHeap = 95_000_000L)
        }

        val result = reader.readManifest(apk.absolutePath)

        val raw = result.rawXml.shouldBeInstanceOf<RawXmlResult.Unavailable>()
        raw.reason shouldBe UnavailableReason.APK_TOO_LARGE
    }

    @Test
    fun `returns LOW_MEMORY on post-preflight heap check`(@TempDir tempDir: File) {
        // Budget check passes but second heap check fails.
        // arscSize * 3 + manifestSize * 2 + slack must be LESS than maxHeap * 0.25 AND
        // MORE than the mocked freeHeap.
        val smallArsc = ByteArray(2 * 1024 * 1024)        // 2 MB
        val smallManifest = ByteArray(512 * 1024)         // 0.5 MB
        val apk = writeApk(tempDir, arscBytes = smallArsc, manifestBytes = smallManifest)

        // estimatedPeak ≈ 2*3 + 0.5*2 + 2 = 9 MB
        // maxHeap * 0.25 = 40 MB → budget OK
        // freeHeap = 8 MB < estimatedPeak + slack (9 + 2 = 11 MB) → LOW_MEMORY
        val reader = ApkManifestReader().also {
            it.setHeap(maxHeap = 160_000_000L, freeHeap = 8 * 1024 * 1024L)
        }

        val result = reader.readManifest(apk.absolutePath)

        val raw = result.rawXml.shouldBeInstanceOf<RawXmlResult.Unavailable>()
        raw.reason shouldBe UnavailableReason.LOW_MEMORY
    }
}
