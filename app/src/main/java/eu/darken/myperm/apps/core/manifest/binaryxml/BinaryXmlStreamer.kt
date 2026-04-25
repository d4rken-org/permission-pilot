package eu.darken.myperm.apps.core.manifest.binaryxml

/**
 * Streaming parser for Android binary XML.
 *
 * Chunk layout reference (AOSP `frameworks/base/libs/androidfw/include/androidfw/ResourceTypes.h`):
 * - Each chunk begins with a `ResChunk_header { u16 type, u16 headerSize, u32 size }`.
 * - Root chunk is `RES_XML_TYPE`. Its body contains child chunks laid out back-to-back:
 *   `RES_STRING_POOL_TYPE`, optional `RES_XML_RESOURCE_MAP_TYPE`, then a stream of
 *   namespace / element / cdata chunks.
 * - Every XML node chunk begins with `ResXMLTree_node { u32 lineNumber, u32 commentRef }`
 *   right after the chunk header.
 */
class BinaryXmlStreamer {

    fun parse(bytes: ByteArray, visitor: BinaryXmlVisitor) {
        if (bytes.size < 8) throw BinaryXmlException("buffer too small for root chunk")

        val rootType = readU16(bytes, 0)
        val rootHeader = readU16(bytes, 2)
        val rootSize = readU32(bytes, 4)

        if (rootType != ChunkTypes.RES_XML_TYPE) {
            throw BinaryXmlException("expected RES_XML_TYPE root, got 0x${"%04X".format(rootType)}")
        }
        validateChunkBounds(rootHeader, rootSize, bytes.size.toLong(), "root")

        visitor.onStartDocument()

        var stringPool: Array<String?>? = null
        var resourceMap: IntArray = IntArray(0)

        var cursor = rootHeader   // start of root body
        val rootLimit = rootSize  // end of root (root chunk occupies [0, rootSize))
        val namespaceStack = ArrayDeque<NamespaceEntry>()

        while (cursor + 8 <= rootLimit) {
            val type = readU16(bytes, cursor)
            val headerSize = readU16(bytes, cursor + 2)
            val chunkSize = readU32(bytes, cursor + 4)
            validateChunkBounds(headerSize, chunkSize, (rootLimit - cursor).toLong(), "chunk@$cursor")

            val safeSize = chunkSize.toInt()
            when (type) {
                ChunkTypes.RES_STRING_POOL_TYPE -> {
                    stringPool = BinaryXmlStringPool.decode(bytes, cursor, headerSize, safeSize)
                }
                ChunkTypes.RES_XML_RESOURCE_MAP_TYPE -> {
                    val body = safeSize - headerSize
                    if (body < 0 || body % 4 != 0) throw BinaryXmlException("bad resource map size: $body")
                    val count = body / 4
                    resourceMap = IntArray(count)
                    for (i in 0 until count) {
                        resourceMap[i] = readU32(bytes, cursor + headerSize + i * 4)
                    }
                }
                ChunkTypes.RES_XML_START_NAMESPACE_TYPE -> {
                    val pool = stringPool ?: throw BinaryXmlException("namespace before string pool")
                    val entry = readNamespaceChunk(bytes, cursor, headerSize, safeSize, pool)
                    namespaceStack.addLast(entry)
                    visitor.onStartNamespace(entry.prefix, entry.uri)
                }
                ChunkTypes.RES_XML_END_NAMESPACE_TYPE -> {
                    val pool = stringPool ?: throw BinaryXmlException("namespace before string pool")
                    val entry = readNamespaceChunk(bytes, cursor, headerSize, safeSize, pool)
                    visitor.onEndNamespace(entry.prefix, entry.uri)
                    // Pop the matching namespace (usually the last; robust to out-of-order in malformed input).
                    val idx = namespaceStack.indexOfLast { it.prefix == entry.prefix && it.uri == entry.uri }
                    if (idx >= 0) namespaceStack.removeAt(idx) else namespaceStack.removeLastOrNull()
                }
                ChunkTypes.RES_XML_START_ELEMENT_TYPE -> {
                    val pool = stringPool ?: throw BinaryXmlException("element before string pool")
                    parseStartElement(bytes, cursor, headerSize, safeSize, pool, resourceMap, namespaceStack, visitor)
                }
                ChunkTypes.RES_XML_END_ELEMENT_TYPE -> {
                    val pool = stringPool ?: throw BinaryXmlException("element before string pool")
                    parseEndElement(bytes, cursor, headerSize, pool, namespaceStack, visitor)
                }
                ChunkTypes.RES_XML_CDATA_TYPE -> {
                    val pool = stringPool ?: throw BinaryXmlException("cdata before string pool")
                    parseCdata(bytes, cursor, headerSize, pool, visitor)
                }
                else -> {
                    // Unknown chunk — skip forward via chunkSize (defensive forward-progress).
                }
            }

            cursor += safeSize
        }

        visitor.onEndDocument()
    }

    private fun parseStartElement(
        bytes: ByteArray,
        chunkStart: Int,
        headerSize: Int,
        chunkSize: Int,
        stringPool: Array<String?>,
        resourceMap: IntArray,
        namespaceStack: ArrayDeque<NamespaceEntry>,
        visitor: BinaryXmlVisitor,
    ) {
        // ResXMLTree_node = 8 bytes after the chunk header: lineNumber, commentRef.
        if (headerSize < XML_NODE_HEADER_SIZE) throw BinaryXmlException("element headerSize too small: $headerSize")
        val lineNumber = readU32(bytes, chunkStart + 8)
        val attrExtStart = chunkStart + XML_NODE_HEADER_SIZE

        // ResXMLTree_attrExt (20 bytes): ns, name, attributeStart, attributeSize, attributeCount, idIndex, classIndex, styleIndex
        val chunkLimit = chunkStart + chunkSize
        if (attrExtStart + 20 > chunkLimit) throw BinaryXmlException("attrExt past chunk end")
        val nsIdx = readU32(bytes, attrExtStart + 0)
        val nameIdx = readU32(bytes, attrExtStart + 4)
        val attributeStart = readU16(bytes, attrExtStart + 8)
        val attributeSize = readU16(bytes, attrExtStart + 10)
        val attributeCount = readU16(bytes, attrExtStart + 12)
        // idIndex (14), classIndex (16), styleIndex (18) — not needed for our use case.

        if (attributeStart < 20) throw BinaryXmlException("attributeStart $attributeStart < 20 (overlaps attrExt header)")
        if (attributeSize < 20) throw BinaryXmlException("attributeSize $attributeSize < 20")
        if (attributeCount > MAX_ATTRIBUTE_COUNT) {
            throw BinaryXmlException("attributeCount $attributeCount exceeds limit $MAX_ATTRIBUTE_COUNT")
        }
        val attrBlockStart = attrExtStart + attributeStart
        val attrBlockSize: Long = attributeCount.toLong() * attributeSize.toLong()
        if (attrBlockStart.toLong() + attrBlockSize > chunkLimit.toLong()) {
            throw BinaryXmlException("attribute block overruns chunk")
        }

        val namespace = stringAt(stringPool, nsIdx)
        val name = stringAt(stringPool, nameIdx) ?: throw BinaryXmlException("element name index invalid")
        val prefix = resolvePrefix(namespace, namespaceStack)

        val attrs = ArrayList<BinaryXmlAttribute>(attributeCount)
        for (i in 0 until attributeCount) {
            val attr = parseAttribute(
                bytes = bytes,
                at = attrBlockStart + i * attributeSize,
                stringPool = stringPool,
                resourceMap = resourceMap,
                namespaceStack = namespaceStack,
            )
            attrs.add(attr)
        }

        visitor.onStartElement(namespace, prefix, name, attrs, lineNumber)
    }

    private fun parseAttribute(
        bytes: ByteArray,
        at: Int,
        stringPool: Array<String?>,
        resourceMap: IntArray,
        namespaceStack: ArrayDeque<NamespaceEntry>,
    ): BinaryXmlAttribute {
        val nsIdx = readU32(bytes, at + 0)
        val nameIdx = readU32(bytes, at + 4)
        val rawValueIdx = readU32(bytes, at + 8)
        // ResValue at offset 12: u16 size, u8 pad, u8 type, u32 data (8 bytes total)
        val type = bytes[at + 15].toInt() and 0xFF
        val data = readU32(bytes, at + 16)

        val namespace = stringAt(stringPool, nsIdx)
        val name = stringAt(stringPool, nameIdx) ?: throw BinaryXmlException("attribute name index invalid")
        val prefix = resolvePrefix(namespace, namespaceStack)
        val rawValueString = stringAt(stringPool, rawValueIdx)

        val resourceId = if (nameIdx in resourceMap.indices) resourceMap[nameIdx] else 0

        val typedValue = decodeTypedValue(type, data, rawValueString, stringPool)
        return BinaryXmlAttribute(namespace, prefix, name, resourceId, rawValueString, typedValue)
    }

    private fun decodeTypedValue(
        type: Int,
        data: Int,
        rawValueString: String?,
        stringPool: Array<String?>,
    ): TypedValue = when (type) {
        ResTypes.TYPE_STRING -> {
            // Prefer rawValueString; fall back to pool lookup via `data` if unset.
            val str = rawValueString ?: stringAt(stringPool, data) ?: ""
            TypedValue.Str(str)
        }
        ResTypes.TYPE_REFERENCE -> TypedValue.Reference(data)
        ResTypes.TYPE_ATTRIBUTE -> TypedValue.AttributeRef(data)
        ResTypes.TYPE_DYNAMIC_REFERENCE -> TypedValue.DynamicReference(data)
        ResTypes.TYPE_DYNAMIC_ATTRIBUTE -> TypedValue.DynamicAttributeRef(data)
        ResTypes.TYPE_INT_DEC -> TypedValue.IntDec(data)
        ResTypes.TYPE_INT_HEX -> TypedValue.IntHex(data)
        ResTypes.TYPE_INT_BOOLEAN -> TypedValue.Bool(data != 0)
        ResTypes.TYPE_FLOAT -> TypedValue.Flt(Float.fromBits(data))
        ResTypes.TYPE_DIMENSION -> TypedValue.Dimension(data)
        ResTypes.TYPE_FRACTION -> TypedValue.Fraction(data)
        ResTypes.TYPE_INT_COLOR_ARGB8 -> TypedValue.Color(data, TypedValue.Color.Width.ARGB8)
        ResTypes.TYPE_INT_COLOR_RGB8 -> TypedValue.Color(data, TypedValue.Color.Width.RGB8)
        ResTypes.TYPE_INT_COLOR_ARGB4 -> TypedValue.Color(data, TypedValue.Color.Width.ARGB4)
        ResTypes.TYPE_INT_COLOR_RGB4 -> TypedValue.Color(data, TypedValue.Color.Width.RGB4)
        ResTypes.TYPE_NULL -> TypedValue.Null(undefined = data == ResTypes.DATA_NULL_UNDEFINED)
        else -> TypedValue.Unknown(type, data)
    }

    private fun parseEndElement(
        bytes: ByteArray,
        chunkStart: Int,
        headerSize: Int,
        stringPool: Array<String?>,
        namespaceStack: ArrayDeque<NamespaceEntry>,
        visitor: BinaryXmlVisitor,
    ) {
        if (headerSize < XML_NODE_HEADER_SIZE) throw BinaryXmlException("end-element header too small")
        val lineNumber = readU32(bytes, chunkStart + 8)
        val endExtStart = chunkStart + XML_NODE_HEADER_SIZE
        val nsIdx = readU32(bytes, endExtStart + 0)
        val nameIdx = readU32(bytes, endExtStart + 4)
        val namespace = stringAt(stringPool, nsIdx)
        val name = stringAt(stringPool, nameIdx) ?: throw BinaryXmlException("end-element name invalid")
        val prefix = resolvePrefix(namespace, namespaceStack)
        visitor.onEndElement(namespace, prefix, name, lineNumber)
    }

    private fun parseCdata(
        bytes: ByteArray,
        chunkStart: Int,
        headerSize: Int,
        stringPool: Array<String?>,
        visitor: BinaryXmlVisitor,
    ) {
        if (headerSize < XML_NODE_HEADER_SIZE) throw BinaryXmlException("cdata header too small")
        val lineNumber = readU32(bytes, chunkStart + 8)
        val dataStart = chunkStart + XML_NODE_HEADER_SIZE
        val dataIdx = readU32(bytes, dataStart + 0)
        val text = stringAt(stringPool, dataIdx) ?: ""
        visitor.onCdata(text, lineNumber)
    }

    private fun readNamespaceChunk(
        bytes: ByteArray,
        chunkStart: Int,
        headerSize: Int,
        chunkSize: Int,
        stringPool: Array<String?>,
    ): NamespaceEntry {
        if (headerSize < XML_NODE_HEADER_SIZE) throw BinaryXmlException("namespace header too small")
        val extStart = chunkStart + XML_NODE_HEADER_SIZE
        // The 8-byte ResXMLTree_namespaceExt (prefix + uri) must fit inside the chunk.
        if (extStart + 8 > chunkStart + chunkSize) {
            throw BinaryXmlException("namespace ext past chunk end")
        }
        val prefixIdx = readU32(bytes, extStart + 0)
        val uriIdx = readU32(bytes, extStart + 4)
        val prefix = stringAt(stringPool, prefixIdx) ?: ""
        val uri = stringAt(stringPool, uriIdx) ?: ""
        return NamespaceEntry(prefix, uri)
    }

    private fun resolvePrefix(namespace: String?, namespaceStack: ArrayDeque<NamespaceEntry>): String? {
        if (namespace.isNullOrEmpty()) return null
        // Most-recently-pushed binding wins on URI collisions.
        for (i in namespaceStack.indices.reversed()) {
            val entry = namespaceStack[i]
            if (entry.uri == namespace) return entry.prefix
        }
        return null
    }

    private fun stringAt(pool: Array<String?>, index: Int): String? {
        if (index == ChunkTypes.NULL_INDEX) return null
        if (index < 0 || index >= pool.size) throw BinaryXmlException("string index $index out of range ${pool.size}")
        return pool[index]
    }

    private fun validateChunkBounds(headerSize: Int, chunkSize: Int, remaining: Long, label: String) {
        if (headerSize < 8) throw BinaryXmlException("$label: headerSize $headerSize < 8")
        if (chunkSize < 0) throw BinaryXmlException("$label: negative chunkSize $chunkSize")
        if (chunkSize < headerSize) throw BinaryXmlException("$label: chunkSize $chunkSize < headerSize $headerSize")
        if (chunkSize.toLong() > remaining) throw BinaryXmlException("$label: chunkSize $chunkSize > remaining $remaining")
    }

    internal data class NamespaceEntry(val prefix: String, val uri: String)

    companion object {
        private const val XML_NODE_HEADER_SIZE = 16  // 8 chunk header + 8 ResXMLTree_node
        // Defensive cap to prevent pathological allocation on adversarial AXML. Real
        // manifests have well under 100 attributes per element.
        private const val MAX_ATTRIBUTE_COUNT = 4096

        private fun readU16(bytes: ByteArray, at: Int): Int {
            if (at + 2 > bytes.size) throw BinaryXmlException("u16 read past buffer at $at")
            val lo = bytes[at].toInt() and 0xFF
            val hi = bytes[at + 1].toInt() and 0xFF
            return (hi shl 8) or lo
        }

        private fun readU32(bytes: ByteArray, at: Int): Int {
            if (at + 4 > bytes.size) throw BinaryXmlException("u32 read past buffer at $at")
            val b0 = bytes[at].toInt() and 0xFF
            val b1 = bytes[at + 1].toInt() and 0xFF
            val b2 = bytes[at + 2].toInt() and 0xFF
            val b3 = bytes[at + 3].toInt() and 0xFF
            return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
        }
    }
}
