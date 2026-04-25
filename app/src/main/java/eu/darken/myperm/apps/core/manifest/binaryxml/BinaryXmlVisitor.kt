package eu.darken.myperm.apps.core.manifest.binaryxml

/**
 * Visitor surface emitted by [BinaryXmlStreamer].
 *
 * Event ordering contract:
 * - [onStartNamespace] for a binding is always emitted before the [onStartElement]
 *   whose attributes may use the bound prefix.
 * - [onEndNamespace] follows the corresponding [onEndElement] (innermost first).
 *
 * [BinaryXmlStreamer] honours this because AXML chunk layout is namespace-then-element
 * by construction. Visitors that defer namespace bookkeeping (e.g. accumulating
 * pending declarations to attach to the next start element) rely on this invariant.
 */
interface BinaryXmlVisitor {
    fun onStartDocument() {}
    fun onEndDocument() {}
    fun onStartNamespace(prefix: String, uri: String) {}
    fun onEndNamespace(prefix: String, uri: String) {}
    fun onStartElement(
        namespace: String?,
        prefix: String?,
        name: String,
        attributes: List<BinaryXmlAttribute>,
        lineNumber: Int,
    ) {}
    fun onEndElement(namespace: String?, prefix: String?, name: String, lineNumber: Int) {}
    fun onCdata(text: String, lineNumber: Int) {}
}

class CompositeVisitor(private val visitors: List<BinaryXmlVisitor>) : BinaryXmlVisitor {

    override fun onStartDocument() {
        visitors.forEach { it.onStartDocument() }
    }

    override fun onEndDocument() {
        visitors.forEach { it.onEndDocument() }
    }

    override fun onStartNamespace(prefix: String, uri: String) {
        visitors.forEach { it.onStartNamespace(prefix, uri) }
    }

    override fun onEndNamespace(prefix: String, uri: String) {
        visitors.forEach { it.onEndNamespace(prefix, uri) }
    }

    override fun onStartElement(
        namespace: String?,
        prefix: String?,
        name: String,
        attributes: List<BinaryXmlAttribute>,
        lineNumber: Int,
    ) {
        visitors.forEach { it.onStartElement(namespace, prefix, name, attributes, lineNumber) }
    }

    override fun onEndElement(namespace: String?, prefix: String?, name: String, lineNumber: Int) {
        visitors.forEach { it.onEndElement(namespace, prefix, name, lineNumber) }
    }

    override fun onCdata(text: String, lineNumber: Int) {
        visitors.forEach { it.onCdata(text, lineNumber) }
    }
}
