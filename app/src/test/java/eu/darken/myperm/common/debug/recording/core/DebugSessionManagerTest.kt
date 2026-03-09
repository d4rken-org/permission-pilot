package eu.darken.myperm.common.debug.recording.core

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

class DebugSessionManagerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `deriveSessionId returns dir name for directory`() {
        val dir = File(tempDir, "myperm_1.0_20260101T120000Z_abc12345")
        dir.mkdirs()
        DebugSessionManager.deriveSessionId(dir) shouldBe "myperm_1.0_20260101T120000Z_abc12345"
    }

    @Test
    fun `deriveSessionId strips zip extension`() {
        val zip = File(tempDir, "myperm_1.0_20260101T120000Z_abc12345.zip")
        zip.createNewFile()
        DebugSessionManager.deriveSessionId(zip) shouldBe "myperm_1.0_20260101T120000Z_abc12345"
    }

    @Test
    fun `deriveSessionId roundtrip - dir and zip yield same ID`() {
        val dir = File(tempDir, "myperm_1.0_20260101T120000Z_abc12345")
        val zip = File(tempDir, "myperm_1.0_20260101T120000Z_abc12345.zip")
        dir.mkdirs()
        zip.createNewFile()

        DebugSessionManager.deriveSessionId(dir) shouldBe DebugSessionManager.deriveSessionId(zip)
    }

    @Test
    fun `parseCreatedAt parses UTC timestamp format`() {
        val name = "myperm_1.0_20260309T143022Z_abc12345"
        val instant = DebugSessionManager.parseCreatedAt(name)
        instant shouldBe Instant.parse("2026-03-09T14:30:22Z")
    }

    @Test
    fun `parseCreatedAt parses legacy millis timestamp`() {
        val millis = 1709999999000L
        val name = "myperm_1.0_${millis}_abc12345"
        val instant = DebugSessionManager.parseCreatedAt(name)
        instant shouldBe Instant.ofEpochMilli(millis)
    }

    @Test
    fun `parseCreatedAt returns EPOCH for unparseable name`() {
        val name = "unknown_format"
        DebugSessionManager.parseCreatedAt(name) shouldBe Instant.EPOCH
    }

    @Test
    fun `computeDiskSize with dir and zip`() {
        val dir = File(tempDir, "session").also { it.mkdirs() }
        File(dir, "core.log").writeText("A".repeat(100))
        val zip = File(tempDir, "session.zip").also { it.writeText("B".repeat(50)) }

        DebugSessionManager.computeDiskSize(dir, zip) shouldBe 150L
    }

    @Test
    fun `computeDiskSize with only dir`() {
        val dir = File(tempDir, "session").also { it.mkdirs() }
        File(dir, "core.log").writeText("A".repeat(100))

        DebugSessionManager.computeDiskSize(dir, null) shouldBe 100L
    }

    @Test
    fun `computeDiskSize with only zip`() {
        val zip = File(tempDir, "session.zip").also { it.writeText("B".repeat(50)) }

        DebugSessionManager.computeDiskSize(null, zip) shouldBe 50L
    }

    @Test
    fun `computeDiskSize with null dir and null zip`() {
        DebugSessionManager.computeDiskSize(null, null) shouldBe 0L
    }
}
