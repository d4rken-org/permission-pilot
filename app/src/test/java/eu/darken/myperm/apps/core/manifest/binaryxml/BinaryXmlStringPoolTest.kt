package eu.darken.myperm.apps.core.manifest.binaryxml

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import java.io.ByteArrayOutputStream

/**
 * Direct tests for the string pool decoder. The streamer end-to-end tests exercise the happy
 * path via [testhelper.binaryxml.AxmlFixtureBuilder], which always produces well-formed pools;
 * these tests target the decoder's bounds / overflow / truncation guards that the builder can't
 * easily express.
 */
class BinaryXmlStringPoolTest : BaseTest() {

    @Test
    fun `utf8 string with two-byte char-count varlen decodes correctly`() {
        // 128 ASCII characters -> charCount 0x80 forces 2-byte varlen encoding on first length field.
        val longAscii = "a".repeat(128)
        val chunk = buildPool(listOf(longAscii), utf8 = true)
        val decoded = BinaryXmlStringPool.decode(chunk, 0, chunk.headerSize(), chunk.chunkSize())
        decoded.size shouldBe 1
        decoded[0] shouldBe longAscii
    }

    @Test
    fun `utf8 string with two-byte byte-count varlen decodes correctly`() {
        // 80 × 2-byte UTF-8 chars (U+00A0) -> 80 chars, 160 bytes. charCount fits in 1 byte,
        // byteCount is 0xA0 which requires 2-byte varlen encoding on the second length field.
        val twoByteChar = " "
        val mixed = twoByteChar.repeat(80)
        val chunk = buildPool(listOf(mixed), utf8 = true)
        val decoded = BinaryXmlStringPool.decode(chunk, 0, chunk.headerSize(), chunk.chunkSize())
        decoded.size shouldBe 1
        decoded[0] shouldBe mixed
    }

    @Test
    fun `utf16 string with two-word varlen decodes correctly`() {
        // 32768 UTF-16 code units -> charLen = 0x8000 forces 2-word varlen encoding.
        val huge = "x".repeat(0x8000)
        val chunk = buildPool(listOf(huge), utf8 = false)
        val decoded = BinaryXmlStringPool.decode(chunk, 0, chunk.headerSize(), chunk.chunkSize())
        decoded[0] shouldBe huge
    }

    @Test
    fun `utf16 oversized charLen is rejected before any large allocation`() {
        // Two layers of defense for the same OOM scenario:
        //   1. MAX_STRING_CHAR_COUNT cap rejects pathological charLen up front.
        //   2. Long-safe `byteLen = charLen * 2L` bounds check would reject overflow values
        //      that slipped past (e.g., if the cap were ever raised above Int.MAX_VALUE / 2).
        // The cap fires first with a more specific message; either rejection prevents the
        // multi-GB CharArray allocation that the original bug would have triggered.
        val chunk = craftUtf16PoolWithOverflowCharLen()
        val ex = shouldThrow<BinaryXmlException> {
            BinaryXmlStringPool.decode(chunk, 0, chunk.headerSize(), chunk.chunkSize())
        }
        ex.message!!.shouldContain("exceeds limit")
    }

    @Test
    fun `stringsStart below headerSize is rejected`() {
        val chunk = buildPool(listOf("a"), utf8 = true)
        // stringsStart lives at offset 20..23 in the pool header. Overwrite it to something
        // smaller than headerSize (28) -> "stringsStart inside header".
        writeU32(chunk, 20, 16)
        val ex = shouldThrow<BinaryXmlException> {
            BinaryXmlStringPool.decode(chunk, 0, chunk.headerSize(), chunk.chunkSize())
        }
        ex.message!!.shouldContain("stringsStart inside header")
    }

    @Test
    fun `stringsStart past chunkSize is rejected`() {
        val chunk = buildPool(listOf("a"), utf8 = true)
        val size = chunk.chunkSize()
        writeU32(chunk, 20, size + 100) // stringsStart > chunkSize
        val ex = shouldThrow<BinaryXmlException> {
            BinaryXmlStringPool.decode(chunk, 0, chunk.headerSize(), chunk.chunkSize())
        }
        ex.message!!.shouldContain("stringsStart past chunkSize")
    }

    @Test
    fun `string offset pointing past chunk end is rejected`() {
        val chunk = buildPool(listOf("a"), utf8 = true)
        // First offset lives at headerSize (28). Overwrite it to a value way past the chunk.
        writeU32(chunk, 28, 0x0FFFFFFF)
        val ex = shouldThrow<BinaryXmlException> {
            BinaryXmlStringPool.decode(chunk, 0, chunk.headerSize(), chunk.chunkSize())
        }
        ex.message!!.shouldContain("past chunk end")
    }

    @Test
    fun `headerSize below minimum is rejected`() {
        val chunk = buildPool(listOf("a"), utf8 = true)
        val ex = shouldThrow<BinaryXmlException> {
            // headerSize = 16 is below the spec minimum of 28 for string pool chunks.
            BinaryXmlStringPool.decode(chunk, 0, 16, chunk.chunkSize())
        }
        ex.message!!.shouldContain("string pool header too small")
    }

    @Test
    fun `chunkSize below headerSize is rejected`() {
        val chunk = buildPool(listOf("a"), utf8 = true)
        val ex = shouldThrow<BinaryXmlException> {
            BinaryXmlStringPool.decode(chunk, 0, chunk.headerSize(), chunk.headerSize() - 4)
        }
        ex.message!!.shouldContain("chunkSize < headerSize")
    }

    @Test
    fun `truncated utf8 varlen throws before reading past chunk`() {
        // Craft a minimal pool where the first string offset points at the very last byte of
        // the chunk. UTF-8 varlen read of the second length field (byte count) then needs to
        // look past the chunk limit, which must throw a BinaryXmlException, not an AIOOBE.
        val chunk = craftUtf8PoolWithTruncatedVarLen()
        val ex = shouldThrow<BinaryXmlException> {
            BinaryXmlStringPool.decode(chunk, 0, chunk.headerSize(), chunk.chunkSize())
        }
        // The guard may trigger at either the char-count or byte-count read, depending on layout.
        (ex.message!!.contains("varlen") || ex.message!!.contains("past chunk")).shouldBeTrue()
    }

    @Test
    fun `truncated utf16 varlen throws before reading past chunk`() {
        val chunk = craftUtf16PoolWithTruncatedVarLen()
        shouldThrow<BinaryXmlException> {
            BinaryXmlStringPool.decode(chunk, 0, chunk.headerSize(), chunk.chunkSize())
        }
    }

    // --- helpers ---

    private fun buildPool(strings: List<String>, utf8: Boolean): ByteArray {
        val stringData = ByteArrayOutputStream()
        val offsets = IntArray(strings.size)
        for ((i, s) in strings.withIndex()) {
            offsets[i] = stringData.size()
            if (utf8) writeUtf8(stringData, s) else writeUtf16(stringData, s)
        }
        while (stringData.size() % 4 != 0) stringData.write(0)

        val headerSize = 28
        val stringsStart = headerSize + offsets.size * 4
        val chunkSize = stringsStart + stringData.size()

        val out = ByteArrayOutputStream()
        out.writeU16(ChunkTypes.RES_STRING_POOL_TYPE)
        out.writeU16(headerSize)
        out.writeU32(chunkSize)
        out.writeU32(strings.size)
        out.writeU32(0)
        out.writeU32(if (utf8) ChunkTypes.STRING_POOL_UTF8_FLAG else 0)
        out.writeU32(stringsStart)
        out.writeU32(0)
        for (off in offsets) out.writeU32(off)
        out.write(stringData.toByteArray())
        return out.toByteArray()
    }

    /**
     * Crafts a UTF-16 pool where the single string declares charLen = 0x7FFFFFFF via two-word
     * varlen encoding. The pool itself is only ~40 bytes, so any real char read would walk off
     * the end — the decoder must reject via the Long-safe overflow check before allocating.
     */
    private fun craftUtf16PoolWithOverflowCharLen(): ByteArray {
        val headerSize = 28
        val stringsStart = headerSize + 4 // one offset
        // String data: two-word varlen encoding of 0x7FFFFFFF:
        //   w0 = 0x8000 | 0x7FFF = 0xFFFF; w1 = 0xFFFF
        // That's 4 bytes. Total string-data = 4 bytes (aligned to 4).
        val stringDataLen = 4
        val chunkSize = stringsStart + stringDataLen

        val out = ByteArrayOutputStream()
        out.writeU16(ChunkTypes.RES_STRING_POOL_TYPE)
        out.writeU16(headerSize)
        out.writeU32(chunkSize)
        out.writeU32(1)
        out.writeU32(0)
        out.writeU32(0)             // flags = UTF-16
        out.writeU32(stringsStart)
        out.writeU32(0)
        out.writeU32(0)             // offset[0] = 0
        // string data: 0xFFFF 0xFFFF (two LE u16 words)
        out.writeU16(0xFFFF)
        out.writeU16(0xFFFF)
        return out.toByteArray()
    }

    private fun craftUtf8PoolWithTruncatedVarLen(): ByteArray {
        val headerSize = 28
        val stringsStart = headerSize + 4
        val stringDataLen = 1       // only one byte of data — far too small for a varlen + body
        val chunkSize = stringsStart + stringDataLen

        val out = ByteArrayOutputStream()
        out.writeU16(ChunkTypes.RES_STRING_POOL_TYPE)
        out.writeU16(headerSize)
        out.writeU32(chunkSize)
        out.writeU32(1)
        out.writeU32(0)
        out.writeU32(ChunkTypes.STRING_POOL_UTF8_FLAG)
        out.writeU32(stringsStart)
        out.writeU32(0)
        out.writeU32(0)             // offset[0] = 0
        // String data: a single high-bit byte, which signals a two-byte varlen but the second
        // byte is missing (would fall past chunkLimit).
        out.write(0x80)
        return out.toByteArray()
    }

    private fun craftUtf16PoolWithTruncatedVarLen(): ByteArray {
        val headerSize = 28
        val stringsStart = headerSize + 4
        // String data: two bytes, first word has high bit set (signaling two-word varlen) but
        // the second word is missing.
        val stringDataLen = 2
        val chunkSize = stringsStart + stringDataLen

        val out = ByteArrayOutputStream()
        out.writeU16(ChunkTypes.RES_STRING_POOL_TYPE)
        out.writeU16(headerSize)
        out.writeU32(chunkSize)
        out.writeU32(1)
        out.writeU32(0)
        out.writeU32(0)             // UTF-16
        out.writeU32(stringsStart)
        out.writeU32(0)
        out.writeU32(0)             // offset[0]
        // w0 = 0x8000 (high bit set) — two-word varlen expected but w1 missing.
        out.writeU16(0x8000)
        return out.toByteArray()
    }

    private fun writeUtf8(out: ByteArrayOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        writeUtf8VarLen(out, s.length)
        writeUtf8VarLen(out, bytes.size)
        out.write(bytes)
        out.write(0)
    }

    private fun writeUtf16(out: ByteArrayOutputStream, s: String) {
        writeUtf16VarLen(out, s.length)
        for (c in s) out.writeU16(c.code)
        out.writeU16(0)
    }

    private fun writeUtf8VarLen(out: ByteArrayOutputStream, value: Int) {
        if (value < 0x80) {
            out.write(value)
        } else {
            out.write(0x80 or ((value shr 8) and 0x7F))
            out.write(value and 0xFF)
        }
    }

    private fun writeUtf16VarLen(out: ByteArrayOutputStream, value: Int) {
        if (value < 0x8000) {
            out.writeU16(value)
        } else {
            out.writeU16(0x8000 or ((value shr 16) and 0x7FFF))
            out.writeU16(value and 0xFFFF)
        }
    }

    private fun ByteArrayOutputStream.writeU16(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeU32(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 24) and 0xFF)
    }

    private fun ByteArray.headerSize(): Int = (this[2].toInt() and 0xFF) or ((this[3].toInt() and 0xFF) shl 8)

    private fun ByteArray.chunkSize(): Int {
        val b0 = this[4].toInt() and 0xFF
        val b1 = this[5].toInt() and 0xFF
        val b2 = this[6].toInt() and 0xFF
        val b3 = this[7].toInt() and 0xFF
        return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    }

    private fun writeU32(buf: ByteArray, at: Int, value: Int) {
        buf[at] = (value and 0xFF).toByte()
        buf[at + 1] = ((value ushr 8) and 0xFF).toByte()
        buf[at + 2] = ((value ushr 16) and 0xFF).toByte()
        buf[at + 3] = ((value ushr 24) and 0xFF).toByte()
    }
}
