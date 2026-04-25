package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlAttribute
import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlVisitor

/**
 * Streaming bucketing visitor that produces the per-section pretty XML the manifest viewer
 * displays. Replaces an earlier binary→string→XmlPullParser double-parse path.
 *
 * Section assignment:
 * - Top-level (depth 2) children of `<manifest>` map by tag to USES_PERMISSION / PERMISSION
 *   / QUERIES; anything else falls back to OTHER. `<application>`'s own attributes go to
 *   OTHER as a self-closing fragment; its children (depth 3) map to ACTIVITIES / SERVICES /
 *   RECEIVERS / PROVIDERS / META_DATA, with anything unrecognised falling back to OTHER.
 * - Sections without entries are dropped from the result.
 *
 * Namespace handling: declarations on `<manifest>` are consumed when the manifest element
 * enters and are NOT propagated into any section's pretty XML. Sections sit one level below
 * `<manifest>` and inherit the prefix binding implicitly through the `android:` qualifier
 * on attribute names emitted by [BinaryXmlFormatting.attributeName].
 *
 * Output style: every attribute on its own indented line, empty elements rendered as ` />`.
 *
 * `isFlagged` is intentionally **not** populated. Heuristic-based flagging depends on
 * scanner thresholds that change independently of cache contents; the viewer model
 * recomputes it from the live queries projection on every load.
 */
class ManifestSectionVisitor(
    private val resolver: ResourceRefResolver,
) : BinaryXmlVisitor {

    private val sections = mutableMapOf<SectionType, MutableList<String>>()
    private val pendingNamespaces = ArrayDeque<Pair<String, String>>()

    // Tracks currently-active non-`android` namespace bindings so that section roots can
    // emit their xmlns:* declarations and stay well-formed XML. The `android` binding is
    // implicit in qualified attribute names emitted by [BinaryXmlFormatting.attributeName],
    // so it's intentionally excluded here.
    private val activeNonAndroidNamespaces = linkedMapOf<String, String>()

    private var depth = 0
    private var inApplication = false

    private var currentSection: SectionType? = null
    private var sectionRootDepth = 0
    private val current = StringBuilder()
    private var pendingOpen = false

    fun result(): List<ManifestSection> = SECTION_ORDER
        .filter { sections.containsKey(it) }
        .map { type ->
            val elements = sections.getValue(type)
            ManifestSection(
                type = type,
                elementCount = elements.size,
                prettyXml = elements.joinToString("\n"),
                isFlagged = false,
            )
        }

    override fun onStartNamespace(prefix: String, uri: String) {
        pendingNamespaces.addLast(prefix to uri)
        if (uri != ANDROID_NS_URI) activeNonAndroidNamespaces[prefix] = uri
    }

    override fun onEndNamespace(prefix: String, uri: String) {
        if (uri != ANDROID_NS_URI) activeNonAndroidNamespaces.remove(prefix)
    }

    override fun onStartElement(
        namespace: String?,
        prefix: String?,
        name: String,
        attributes: List<BinaryXmlAttribute>,
        lineNumber: Int,
    ) {
        depth++

        if (depth == 1) {
            // <manifest> root. Consume any pending namespace declarations so they don't bleed
            // into the first section element.
            pendingNamespaces.clear()
            return
        }

        if (currentSection != null) {
            // Inside an active section — emit the open tag indented relative to the section root.
            val relIndent = depth - sectionRootDepth
            renderStart(namespace, prefix, name, attributes, relIndent)
            return
        }

        // Not inside an active section: decide what this element starts.
        if (depth == 2 && name == APPLICATION_TAG && !inApplication) {
            inApplication = true
            // Application's own attributes go to OTHER as a self-closing fragment.
            if (attributes.isNotEmpty()) {
                val sb = StringBuilder()
                sb.append('<').append(BinaryXmlFormatting.qualifiedName(namespace, prefix, name))
                for (attr in attributes) {
                    sb.append('\n').append(INDENT)
                    sb.append(BinaryXmlFormatting.attributeName(attr))
                    sb.append("=\"")
                    sb.append(BinaryXmlFormatting.escapeXml(BinaryXmlFormatting.formatAttributeValue(attr, resolver)))
                    sb.append('"')
                }
                sb.append(" />")
                sections.getOrPut(SectionType.OTHER) { mutableListOf() }.add(sb.toString())
            }
            return
        }

        val sectionType: SectionType? = when {
            depth == 2 -> mapTopLevelTag(name)
            depth == 3 && inApplication -> mapApplicationChildTag(name)
            else -> null
        }
        if (sectionType != null) {
            currentSection = sectionType
            sectionRootDepth = depth
            current.clear()
            pendingOpen = false
            renderStart(namespace, prefix, name, attributes, relIndent = 0)
        }
    }

    override fun onEndElement(namespace: String?, prefix: String?, name: String, lineNumber: Int) {
        val active = currentSection
        if (active == null) {
            if (depth == 2 && inApplication && name == APPLICATION_TAG) {
                inApplication = false
            }
            depth--
            return
        }

        // relIndent matches the depth that the corresponding start tag was emitted at
        // (relIndent for an element at depth N is N - sectionRootDepth).
        val relIndent = depth - sectionRootDepth
        if (pendingOpen) {
            current.append(" />\n")
            pendingOpen = false
        } else {
            current.append(INDENT.repeat(relIndent))
            current.append("</").append(BinaryXmlFormatting.qualifiedName(namespace, prefix, name)).append(">\n")
        }
        if (depth == sectionRootDepth) {
            sections.getOrPut(active) { mutableListOf() }.add(current.toString().trimEnd('\n'))
            currentSection = null
        }
        depth--
    }

    override fun onCdata(text: String, lineNumber: Int) {
        if (currentSection == null) return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        flushPendingOpen()
        val relIndent = depth - sectionRootDepth
        current.append(INDENT.repeat(relIndent)).append(BinaryXmlFormatting.escapeXml(trimmed)).append('\n')
    }

    private fun renderStart(
        namespace: String?,
        prefix: String?,
        name: String,
        attributes: List<BinaryXmlAttribute>,
        relIndent: Int,
    ) {
        flushPendingOpen()
        current.append(INDENT.repeat(relIndent))
        current.append('<').append(BinaryXmlFormatting.qualifiedName(namespace, prefix, name))
        if (relIndent == 0) {
            // Section root — emit any active non-`android` namespace declarations so prefixed
            // content (e.g. <dist:module>) renders as well-formed XML. Empty-prefix bindings
            // map to bare `xmlns="…"`.
            for ((p, u) in activeNonAndroidNamespaces) {
                current.append('\n').append(INDENT.repeat(relIndent + 1))
                if (p.isEmpty()) current.append("xmlns") else current.append("xmlns:").append(p)
                current.append("=\"")
                current.append(BinaryXmlFormatting.escapeXml(u)).append('"')
            }
        }
        for (attr in attributes) {
            current.append('\n').append(INDENT.repeat(relIndent + 1))
            current.append(BinaryXmlFormatting.attributeName(attr))
            current.append("=\"")
            current.append(BinaryXmlFormatting.escapeXml(BinaryXmlFormatting.formatAttributeValue(attr, resolver)))
            current.append('"')
        }
        pendingOpen = true
    }

    private fun flushPendingOpen() {
        if (pendingOpen) {
            current.append(">\n")
            pendingOpen = false
        }
    }

    companion object {
        private const val INDENT = "  "
        private const val APPLICATION_TAG = "application"
        private const val ANDROID_NS_URI = "http://schemas.android.com/apk/res/android"

        private val SECTION_ORDER = listOf(
            SectionType.OTHER,
            SectionType.USES_PERMISSION,
            SectionType.PERMISSION,
            SectionType.QUERIES,
            SectionType.ACTIVITIES,
            SectionType.SERVICES,
            SectionType.RECEIVERS,
            SectionType.PROVIDERS,
            SectionType.META_DATA,
        )

        private fun mapTopLevelTag(name: String): SectionType = when (name) {
            "uses-permission", "uses-permission-sdk-23" -> SectionType.USES_PERMISSION
            "permission", "permission-group", "permission-tree" -> SectionType.PERMISSION
            "queries" -> SectionType.QUERIES
            else -> SectionType.OTHER
        }

        private fun mapApplicationChildTag(name: String): SectionType = when (name) {
            "activity", "activity-alias" -> SectionType.ACTIVITIES
            "service" -> SectionType.SERVICES
            "receiver" -> SectionType.RECEIVERS
            "provider" -> SectionType.PROVIDERS
            "meta-data" -> SectionType.META_DATA
            else -> SectionType.OTHER
        }
    }
}
