package testhelper.binaryxml

import java.io.ByteArrayOutputStream

/**
 * Programmatic builder for synthetic binary AXML test fixtures.
 *
 * Layout mirrors AOSP `frameworks/base/libs/androidfw/include/androidfw/ResourceTypes.h`.
 *
 * Intended use: construct small, targeted fixtures — edge cases in string pool encoding,
 * malformed chunk bounds, attribute-size variations — that would be tedious to produce via
 * aapt2. Real-world blob validation should use checked-in aapt2-generated fixtures instead.
 */
class AxmlFixtureBuilder {

    private val strings = mutableListOf<String>()
    private val stringIndex = HashMap<String, Int>()
    private var utf8 = true

    private val chunks = ByteArrayOutputStream()
    private var resourceMap: IntArray? = null

    fun utf8(enabled: Boolean): AxmlFixtureBuilder {
        utf8 = enabled
        return this
    }

    fun str(value: String): Int {
        stringIndex[value]?.let { return it }
        val idx = strings.size
        strings.add(value)
        stringIndex[value] = idx
        return idx
    }

    fun nullStr(): Int = NULL_INDEX

    fun resourceMap(ids: IntArray): AxmlFixtureBuilder {
        resourceMap = ids
        return this
    }

    fun startNamespace(prefix: String, uri: String, lineNumber: Int = 1): AxmlFixtureBuilder {
        writeNodeChunk(TYPE_START_NAMESPACE, lineNumber) { body ->
            body.writeU32(str(prefix))
            body.writeU32(str(uri))
        }
        return this
    }

    fun endNamespace(prefix: String, uri: String, lineNumber: Int = 1): AxmlFixtureBuilder {
        writeNodeChunk(TYPE_END_NAMESPACE, lineNumber) { body ->
            body.writeU32(str(prefix))
            body.writeU32(str(uri))
        }
        return this
    }

    fun startElement(
        namespace: String?,
        name: String,
        lineNumber: Int = 1,
        attributes: List<Attr> = emptyList(),
        attributeSize: Int = 20,
    ): AxmlFixtureBuilder {
        if (attributeSize < 20) throw IllegalArgumentException("attributeSize must be >= 20 for valid fixtures; use rawChunk for malformed cases")
        val extStart = ByteArrayOutputStream()
        extStart.writeU32(if (namespace == null) NULL_INDEX else str(namespace))
        extStart.writeU32(str(name))
        extStart.writeU16(20) // attributeStart (relative to ResXMLTree_attrExt = this extStart)
        extStart.writeU16(attributeSize)
        extStart.writeU16(attributes.size)
        extStart.writeU16(0) // idIndex
        extStart.writeU16(0) // classIndex
        extStart.writeU16(0) // styleIndex
        for (attr in attributes) {
            val attrBuf = ByteArrayOutputStream()
            attrBuf.writeU32(if (attr.namespace == null) NULL_INDEX else str(attr.namespace))
            attrBuf.writeU32(str(attr.name))
            attrBuf.writeU32(attr.rawValueString?.let { str(it) } ?: NULL_INDEX)
            attrBuf.writeU16(8)                  // typedSize
            attrBuf.writeU8(0)                   // pad
            attrBuf.writeU8(attr.type)           // type
            attrBuf.writeU32(attr.data)          // data
            // Pad to attributeSize if requested larger than 20.
            val written = attrBuf.size()
            repeat(attributeSize - written) { attrBuf.write(0) }
            extStart.write(attrBuf.toByteArray())
        }
        writeNodeChunk(TYPE_START_ELEMENT, lineNumber) { body -> body.write(extStart.toByteArray()) }
        return this
    }

    fun endElement(namespace: String?, name: String, lineNumber: Int = 1): AxmlFixtureBuilder {
        writeNodeChunk(TYPE_END_ELEMENT, lineNumber) { body ->
            body.writeU32(if (namespace == null) NULL_INDEX else str(namespace))
            body.writeU32(str(name))
        }
        return this
    }

    fun cdata(text: String, lineNumber: Int = 1): AxmlFixtureBuilder {
        writeNodeChunk(TYPE_CDATA, lineNumber) { body ->
            body.writeU32(str(text))
            body.writeU16(8)   // typedSize
            body.writeU8(0)    // pad
            body.writeU8(0x03) // TYPE_STRING
            body.writeU32(str(text))
        }
        return this
    }

    /**
     * Append an arbitrary raw chunk. Use for malformed-case tests.
     * The caller supplies the full chunk bytes including its `ResChunk_header`.
     */
    fun rawChunk(bytes: ByteArray): AxmlFixtureBuilder {
        chunks.write(bytes)
        return this
    }

    fun build(): ByteArray {
        val inner = ByteArrayOutputStream()
        inner.write(buildStringPool())
        resourceMap?.let { inner.write(buildResourceMap(it)) }
        inner.write(chunks.toByteArray())
        val out = ByteArrayOutputStream()
        // Root: RES_XML_TYPE, headerSize=8, size=8 + inner.size
        val totalSize = 8 + inner.size()
        out.writeU16(TYPE_XML)
        out.writeU16(8)
        out.writeU32(totalSize)
        out.write(inner.toByteArray())
        return out.toByteArray()
    }

    private fun buildStringPool(): ByteArray {
        val flags = if (utf8) FLAG_UTF8 else 0
        val stringData = ByteArrayOutputStream()
        val offsets = IntArray(strings.size)

        for ((i, s) in strings.withIndex()) {
            offsets[i] = stringData.size()
            if (utf8) writeUtf8StringTo(stringData, s) else writeUtf16StringTo(stringData, s)
        }
        // Align string data to 4 bytes.
        while (stringData.size() % 4 != 0) stringData.write(0)

        val headerSize = 28
        val offsetsSize = offsets.size * 4
        val stringsStart = headerSize + offsetsSize
        val chunkSize = stringsStart + stringData.size()

        val out = ByteArrayOutputStream()
        out.writeU16(TYPE_STRING_POOL)
        out.writeU16(headerSize)
        out.writeU32(chunkSize)
        out.writeU32(strings.size)       // stringCount
        out.writeU32(0)                  // styleCount
        out.writeU32(flags)              // flags
        out.writeU32(stringsStart)       // stringsStart
        out.writeU32(0)                  // stylesStart
        for (off in offsets) out.writeU32(off)
        out.write(stringData.toByteArray())
        return out.toByteArray()
    }

    private fun writeUtf8StringTo(out: ByteArrayOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        writeUtf8VarLen(out, s.length)       // char count
        writeUtf8VarLen(out, bytes.size)     // byte count
        out.write(bytes)
        out.write(0)                         // null terminator
    }

    private fun writeUtf16StringTo(out: ByteArrayOutputStream, s: String) {
        val units = s.toCharArray()
        writeUtf16VarLen(out, units.size)
        for (c in units) out.writeU16(c.code)
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

    private fun buildResourceMap(ids: IntArray): ByteArray {
        val out = ByteArrayOutputStream()
        val chunkSize = 8 + ids.size * 4
        out.writeU16(TYPE_RESOURCE_MAP)
        out.writeU16(8)
        out.writeU32(chunkSize)
        for (id in ids) out.writeU32(id)
        return out.toByteArray()
    }

    private fun writeNodeChunk(type: Int, lineNumber: Int, writeBody: (ByteArrayOutputStream) -> Unit) {
        val body = ByteArrayOutputStream()
        writeBody(body)
        val headerSize = 16 // 8 ResChunk_header + 8 ResXMLTree_node
        val chunkSize = headerSize + body.size()
        chunks.writeU16(type)
        chunks.writeU16(headerSize)
        chunks.writeU32(chunkSize)
        chunks.writeU32(lineNumber)
        chunks.writeU32(-1) // commentRef = 0xFFFFFFFF
        chunks.write(body.toByteArray())
    }

    data class Attr(
        val namespace: String?,
        val name: String,
        val type: Int,
        val data: Int,
        val rawValueString: String? = null,
    )

    companion object {
        const val NULL_INDEX: Int = -1

        const val TYPE_STRING_POOL = 0x0001
        const val TYPE_XML = 0x0003
        const val TYPE_RESOURCE_MAP = 0x0180
        const val TYPE_START_NAMESPACE = 0x0100
        const val TYPE_END_NAMESPACE = 0x0101
        const val TYPE_START_ELEMENT = 0x0102
        const val TYPE_END_ELEMENT = 0x0103
        const val TYPE_CDATA = 0x0104

        const val FLAG_UTF8 = 1 shl 8

        const val RES_TYPE_NULL = 0x00
        const val RES_TYPE_REFERENCE = 0x01
        const val RES_TYPE_ATTRIBUTE = 0x02
        const val RES_TYPE_STRING = 0x03
        const val RES_TYPE_FLOAT = 0x04
        const val RES_TYPE_DIMENSION = 0x05
        const val RES_TYPE_FRACTION = 0x06
        const val RES_TYPE_DYNAMIC_REFERENCE = 0x07
        const val RES_TYPE_DYNAMIC_ATTRIBUTE = 0x08
        const val RES_TYPE_INT_DEC = 0x10
        const val RES_TYPE_INT_HEX = 0x11
        const val RES_TYPE_INT_BOOLEAN = 0x12
        const val RES_TYPE_INT_COLOR_ARGB8 = 0x1C
        const val RES_TYPE_INT_COLOR_RGB8 = 0x1D
        const val RES_TYPE_INT_COLOR_ARGB4 = 0x1E
        const val RES_TYPE_INT_COLOR_RGB4 = 0x1F
    }
}

internal fun ByteArrayOutputStream.writeU8(value: Int) {
    write(value and 0xFF)
}

internal fun ByteArrayOutputStream.writeU16(value: Int) {
    write(value and 0xFF)
    write((value ushr 8) and 0xFF)
}

internal fun ByteArrayOutputStream.writeU32(value: Int) {
    write(value and 0xFF)
    write((value ushr 8) and 0xFF)
    write((value ushr 16) and 0xFF)
    write((value ushr 24) and 0xFF)
}
