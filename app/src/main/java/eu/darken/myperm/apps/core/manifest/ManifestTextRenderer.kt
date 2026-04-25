package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.manifest.BinaryXmlFormatting.ANDROID_NS
import eu.darken.myperm.apps.core.manifest.BinaryXmlFormatting.ANDROID_PREFIX
import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlAttribute
import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlVisitor

/**
 * Visitor that emits a textual XML representation of the manifest, consumed by
 * [ManifestSectionParser] (which runs namespace-unaware). Attributes bound to the Android
 * namespace URI always render with the `android:` prefix regardless of the prefix actually
 * declared in the binary XML — required for the downstream textual attribute lookup.
 */
class ManifestTextRenderer(
    private val resolver: ResourceRefResolver,
) : BinaryXmlVisitor {

    private val out = StringBuilder(INITIAL_CAPACITY)
    private val pendingNamespaces = ArrayDeque<Pair<String, String>>()
    private var indent = 0
    private var pendingElementStart = false

    fun result(): String = out.toString()

    override fun onStartNamespace(prefix: String, uri: String) {
        pendingNamespaces.addLast(prefix to uri)
    }

    override fun onStartElement(
        namespace: String?,
        prefix: String?,
        name: String,
        attributes: List<BinaryXmlAttribute>,
        lineNumber: Int,
    ) {
        flushPendingOpen()
        out.append(INDENT.repeat(indent))
        out.append('<').append(BinaryXmlFormatting.qualifiedName(namespace, prefix, name))
        while (pendingNamespaces.isNotEmpty()) {
            val (boundPrefix, uri) = pendingNamespaces.removeFirst()
            val effectivePrefix = if (uri == ANDROID_NS) ANDROID_PREFIX else boundPrefix
            out.append('\n').append(INDENT.repeat(indent + 1))
            out.append("xmlns:").append(effectivePrefix).append("=\"").append(BinaryXmlFormatting.escapeXml(uri)).append("\"")
        }
        for (attr in attributes) {
            out.append('\n').append(INDENT.repeat(indent + 1))
            out.append(BinaryXmlFormatting.attributeName(attr))
            out.append("=\"")
            out.append(BinaryXmlFormatting.escapeXml(BinaryXmlFormatting.formatAttributeValue(attr, resolver)))
            out.append('"')
        }
        pendingElementStart = true
        indent++
    }

    override fun onEndElement(namespace: String?, prefix: String?, name: String, lineNumber: Int) {
        indent--
        if (pendingElementStart) {
            out.append(" />\n")
            pendingElementStart = false
        } else {
            out.append(INDENT.repeat(indent))
            out.append("</").append(BinaryXmlFormatting.qualifiedName(namespace, prefix, name)).append(">\n")
        }
    }

    override fun onCdata(text: String, lineNumber: Int) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        flushPendingOpen()
        out.append(INDENT.repeat(indent)).append(BinaryXmlFormatting.escapeXml(trimmed)).append('\n')
    }

    private fun flushPendingOpen() {
        if (pendingElementStart) {
            out.append(">\n")
            pendingElementStart = false
        }
    }

    companion object {
        private const val INITIAL_CAPACITY = 32 * 1024
        private const val INDENT = "  "
    }
}
