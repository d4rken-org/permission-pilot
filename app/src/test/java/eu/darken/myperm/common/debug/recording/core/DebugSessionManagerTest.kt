package eu.darken.myperm.common.debug.recording.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

class DebugSessionManagerTest {

    @TempDir
    lateinit var tempDir: File

    @Nested
    inner class DeriveBaseName {
        @Test
        fun `returns dir name for directory`() {
            val dir = File(tempDir, "myperm_1.0_20260101T120000Z_abc12345")
            dir.mkdirs()
            DebugSessionManager.deriveBaseName(dir) shouldBe "myperm_1.0_20260101T120000Z_abc12345"
        }

        @Test
        fun `strips zip extension`() {
            val zip = File(tempDir, "myperm_1.0_20260101T120000Z_abc12345.zip")
            zip.createNewFile()
            DebugSessionManager.deriveBaseName(zip) shouldBe "myperm_1.0_20260101T120000Z_abc12345"
        }

        @Test
        fun `dir and zip yield same base name`() {
            val dir = File(tempDir, "myperm_1.0_20260101T120000Z_abc12345")
            val zip = File(tempDir, "myperm_1.0_20260101T120000Z_abc12345.zip")
            dir.mkdirs()
            zip.createNewFile()
            DebugSessionManager.deriveBaseName(dir) shouldBe DebugSessionManager.deriveBaseName(zip)
        }
    }

    @Nested
    inner class DeriveSessionId {
        @Test
        fun `adds ext prefix for external storage`() {
            val extDir = File(tempDir, "ext/debug/logs").also { it.mkdirs() }
            val cacheDir = File(tempDir, "cache/debug/logs").also { it.mkdirs() }
            val session = File(extDir, "myperm_1.0_20260101T120000Z_abc12345").also { it.mkdirs() }

            DebugSessionManager.deriveSessionId(session, extDir, cacheDir) shouldBe
                    "ext:myperm_1.0_20260101T120000Z_abc12345"
        }

        @Test
        fun `adds cache prefix for cache storage`() {
            val extDir = File(tempDir, "ext/debug/logs").also { it.mkdirs() }
            val cacheDir = File(tempDir, "cache/debug/logs").also { it.mkdirs() }
            val session = File(cacheDir, "myperm_1.0_20260101T120000Z_abc12345").also { it.mkdirs() }

            DebugSessionManager.deriveSessionId(session, extDir, cacheDir) shouldBe
                    "cache:myperm_1.0_20260101T120000Z_abc12345"
        }

        @Test
        fun `returns bare name for unknown storage location`() {
            val extDir = File(tempDir, "ext/debug/logs").also { it.mkdirs() }
            val cacheDir = File(tempDir, "cache/debug/logs").also { it.mkdirs() }
            val otherDir = File(tempDir, "other/debug/logs").also { it.mkdirs() }
            val session = File(otherDir, "myperm_1.0_20260101T120000Z_abc12345").also { it.mkdirs() }

            DebugSessionManager.deriveSessionId(session, extDir, cacheDir) shouldBe
                    "myperm_1.0_20260101T120000Z_abc12345"
        }

        @Test
        fun `handles null external dir`() {
            val cacheDir = File(tempDir, "cache/debug/logs").also { it.mkdirs() }
            val session = File(cacheDir, "myperm_1.0_20260101T120000Z_abc12345").also { it.mkdirs() }

            DebugSessionManager.deriveSessionId(session, null, cacheDir) shouldBe
                    "cache:myperm_1.0_20260101T120000Z_abc12345"
        }

        @Test
        fun `strips zip extension before prefixing`() {
            val extDir = File(tempDir, "ext/debug/logs").also { it.mkdirs() }
            val cacheDir = File(tempDir, "cache/debug/logs").also { it.mkdirs() }
            val zip = File(extDir, "myperm_1.0_20260101T120000Z_abc12345.zip").also { it.createNewFile() }

            DebugSessionManager.deriveSessionId(zip, extDir, cacheDir) shouldBe
                    "ext:myperm_1.0_20260101T120000Z_abc12345"
        }

        @Test
        fun `dir and zip in same storage yield same ID`() {
            val extDir = File(tempDir, "ext/debug/logs").also { it.mkdirs() }
            val cacheDir = File(tempDir, "cache/debug/logs").also { it.mkdirs() }
            val dir = File(extDir, "myperm_1.0_20260101T120000Z_abc12345").also { it.mkdirs() }
            val zip = File(extDir, "myperm_1.0_20260101T120000Z_abc12345.zip").also { it.createNewFile() }

            DebugSessionManager.deriveSessionId(dir, extDir, cacheDir) shouldBe
                    DebugSessionManager.deriveSessionId(zip, extDir, cacheDir)
        }

        @Test
        fun `same name in different storage yields different IDs`() {
            val extDir = File(tempDir, "ext/debug/logs").also { it.mkdirs() }
            val cacheDir = File(tempDir, "cache/debug/logs").also { it.mkdirs() }
            val extSession = File(extDir, "myperm_1.0_20260101T120000Z_abc12345").also { it.mkdirs() }
            val cacheSession = File(cacheDir, "myperm_1.0_20260101T120000Z_abc12345").also { it.mkdirs() }

            val extId = DebugSessionManager.deriveSessionId(extSession, extDir, cacheDir)
            val cacheId = DebugSessionManager.deriveSessionId(cacheSession, extDir, cacheDir)
            extId shouldBe "ext:myperm_1.0_20260101T120000Z_abc12345"
            cacheId shouldBe "cache:myperm_1.0_20260101T120000Z_abc12345"
        }
    }

    @Nested
    inner class ParseSessionId {
        @Test
        fun `splits prefixed ID`() {
            DebugSessionManager.parseSessionId("ext:session_name") shouldBe Pair("ext", "session_name")
            DebugSessionManager.parseSessionId("cache:session_name") shouldBe Pair("cache", "session_name")
        }

        @Test
        fun `returns null prefix for bare ID`() {
            DebugSessionManager.parseSessionId("session_name") shouldBe Pair(null, "session_name")
        }

        @Test
        fun `handles ID with multiple colons`() {
            DebugSessionManager.parseSessionId("ext:name:with:colons") shouldBe Pair("ext", "name:with:colons")
        }
    }

    @Nested
    inner class ParseCreatedAt {
        @Test
        fun `parses UTC timestamp format`() {
            val name = "myperm_1.0_20260309T143022Z_abc12345"
            val instant = DebugSessionManager.parseCreatedAt(name)
            instant shouldBe Instant.parse("2026-03-09T14:30:22Z")
        }

        @Test
        fun `parses legacy millis timestamp`() {
            val millis = 1709999999000L
            val name = "myperm_1.0_${millis}_abc12345"
            val instant = DebugSessionManager.parseCreatedAt(name)
            instant shouldBe Instant.ofEpochMilli(millis)
        }

        @Test
        fun `returns EPOCH for unparseable name`() {
            val name = "unknown_format"
            DebugSessionManager.parseCreatedAt(name) shouldBe Instant.EPOCH
        }

        @Test
        fun `handles version name with underscores`() {
            val name = "myperm_1.0.0_beta_20260309T143022Z_abc12345"
            val instant = DebugSessionManager.parseCreatedAt(name)
            instant shouldBe Instant.parse("2026-03-09T14:30:22Z")
        }
    }

    @Nested
    inner class ComputeDiskSize {
        @Test
        fun `with dir and zip`() {
            val dir = File(tempDir, "session").also { it.mkdirs() }
            File(dir, "core.log").writeText("A".repeat(100))
            val zip = File(tempDir, "session.zip").also { it.writeText("B".repeat(50)) }

            DebugSessionManager.computeDiskSize(dir, zip) shouldBe 150L
        }

        @Test
        fun `with only dir`() {
            val dir = File(tempDir, "session").also { it.mkdirs() }
            File(dir, "core.log").writeText("A".repeat(100))

            DebugSessionManager.computeDiskSize(dir, null) shouldBe 100L
        }

        @Test
        fun `with only zip`() {
            val zip = File(tempDir, "session.zip").also { it.writeText("B".repeat(50)) }

            DebugSessionManager.computeDiskSize(null, zip) shouldBe 50L
        }

        @Test
        fun `with null dir and null zip`() {
            DebugSessionManager.computeDiskSize(null, null) shouldBe 0L
        }
    }
}
