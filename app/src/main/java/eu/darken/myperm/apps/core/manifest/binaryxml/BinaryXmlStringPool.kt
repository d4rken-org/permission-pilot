package eu.darken.myperm.apps.core.manifest.binaryxml

/**
 * Decoder for the `ResStringPool` chunk used throughout the AXML/ARSC format.
 *
 * - Each string is addressed by a 32-bit offset stored in `stringOffsets`, relative to
 *   `stringsStart` inside the chunk.
 * - UTF-8 strings are laid out as:
 *     [char-count varlen (in characters)][byte-length varlen (in bytes)][utf8 bytes][0x00 terminator]
 * - UTF-16 strings are laid out as:
 *     [char-length varlen (in u16 units)][le-u16 code units][u16 0x0000 terminator]
 *
 * Variable-length encoding:
 *   - UTF-8 uses 1 or 2 *bytes*: if the high bit of the first byte is set, the actual length
 *     is ((b0 & 0x7F) shl 8) or b1.
 *   - UTF-16 uses 1 or 2 *u16 words*: if the high bit of the first u16 is set, the actual length
 *     is ((w0 & 0x7FFF) shl 16) or w1.
 */
internal object BinaryXmlStringPool {

    fun decode(
        chunk: ByteArray,
        chunkStart: Int,
        headerSize: Int,
        chunkSize: Int,
    ): Array<String?> {
        if (headerSize < 28) throw BinaryXmlException("string pool header too small: $headerSize")
        if (chunkSize < headerSize) throw BinaryXmlException("string pool chunkSize < headerSize")
        val headerLimit = chunkStart + headerSize
        val chunkLimit = chunkStart + chunkSize
        if (chunkLimit > chunk.size) throw BinaryXmlException("string pool chunk overruns buffer")

        // ResStringPool_header: chunk header (8 bytes), then
        //   u32 stringCount, u32 styleCount, u32 flags, u32 stringsStart, u32 stylesStart
        val stringCount = readU32(chunk, chunkStart + 8)
        val flags = readU32(chunk, chunkStart + 16)
        val stringsStart = readU32(chunk, chunkStart + 20)

        if (stringCount < 0) throw BinaryXmlException("invalid stringCount: $stringCount")
        if (stringsStart < headerSize) throw BinaryXmlException("stringsStart inside header: $stringsStart")
        if (stringsStart > chunkSize) throw BinaryXmlException("stringsStart past chunkSize: $stringsStart > $chunkSize")

        val offsetsStart = headerLimit
        val offsetsEnd = offsetsStart + stringCount * 4L
        if (offsetsEnd > chunkLimit) throw BinaryXmlException("string offset table overruns chunk")

        val utf8 = (flags and ChunkTypes.STRING_POOL_UTF8_FLAG) != 0
        val result = arrayOfNulls<String>(stringCount)
        val stringsBase = chunkStart + stringsStart

        for (i in 0 until stringCount) {
            val offset = readU32(chunk, offsetsStart + i * 4)
            if (offset < 0) throw BinaryXmlException("negative string offset at $i")
            val absolute = stringsBase + offset
            if (absolute >= chunkLimit) throw BinaryXmlException("string #$i offset past chunk end")
            result[i] = if (utf8) readUtf8(chunk, absolute, chunkLimit) else readUtf16(chunk, absolute, chunkLimit)
        }

        return result
    }

    private fun readUtf8(chunk: ByteArray, at: Int, chunkLimit: Int): String {
        // Two varlen length fields: the first is char-count (not used here), the second is byte-length.
        var p = at
        p = skipUtf8VarLen(chunk, p, chunkLimit) // char count
        val byteLen = readUtf8VarLen(chunk, p, chunkLimit)
        p += if ((chunk[p].toInt() and 0x80) != 0) 2 else 1
        if (byteLen < 0) throw BinaryXmlException("negative utf8 byte length")
        if (p + byteLen > chunkLimit) throw BinaryXmlException("utf8 string overruns chunk")
        return String(chunk, p, byteLen, Charsets.UTF_8)
    }

    private fun readUtf16(chunk: ByteArray, at: Int, chunkLimit: Int): String {
        val charLen = readUtf16VarLen(chunk, at, chunkLimit)
        val p = at + if (((readU16(chunk, at)) and 0x8000) != 0) 4 else 2
        if (charLen < 0) throw BinaryXmlException("negative utf16 char length")
        val byteLen = charLen * 2
        if (p + byteLen > chunkLimit) throw BinaryXmlException("utf16 string overruns chunk")
        val chars = CharArray(charLen)
        var cp = p
        for (i in 0 until charLen) {
            val lo = chunk[cp].toInt() and 0xFF
            val hi = chunk[cp + 1].toInt() and 0xFF
            chars[i] = ((hi shl 8) or lo).toChar()
            cp += 2
        }
        return String(chars)
    }

    private fun skipUtf8VarLen(chunk: ByteArray, at: Int, chunkLimit: Int): Int {
        if (at >= chunkLimit) throw BinaryXmlException("utf8 varlen past chunk")
        return at + if ((chunk[at].toInt() and 0x80) != 0) 2 else 1
    }

    private fun readUtf8VarLen(chunk: ByteArray, at: Int, chunkLimit: Int): Int {
        if (at >= chunkLimit) throw BinaryXmlException("utf8 varlen past chunk")
        val b0 = chunk[at].toInt() and 0xFF
        return if ((b0 and 0x80) != 0) {
            if (at + 1 >= chunkLimit) throw BinaryXmlException("truncated utf8 varlen")
            val b1 = chunk[at + 1].toInt() and 0xFF
            ((b0 and 0x7F) shl 8) or b1
        } else {
            b0
        }
    }

    private fun readUtf16VarLen(chunk: ByteArray, at: Int, chunkLimit: Int): Int {
        if (at + 1 >= chunkLimit) throw BinaryXmlException("utf16 varlen past chunk")
        val w0 = readU16(chunk, at)
        return if ((w0 and 0x8000) != 0) {
            if (at + 3 >= chunkLimit) throw BinaryXmlException("truncated utf16 varlen")
            val w1 = readU16(chunk, at + 2)
            ((w0 and 0x7FFF) shl 16) or w1
        } else {
            w0
        }
    }

    private fun readU16(chunk: ByteArray, at: Int): Int {
        val lo = chunk[at].toInt() and 0xFF
        val hi = chunk[at + 1].toInt() and 0xFF
        return (hi shl 8) or lo
    }

    private fun readU32(chunk: ByteArray, at: Int): Int {
        val b0 = chunk[at].toInt() and 0xFF
        val b1 = chunk[at + 1].toInt() and 0xFF
        val b2 = chunk[at + 2].toInt() and 0xFF
        val b3 = chunk[at + 3].toInt() and 0xFF
        return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    }
}
