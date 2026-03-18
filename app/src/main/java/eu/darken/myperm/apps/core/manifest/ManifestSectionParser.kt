package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManifestSectionParser @Inject constructor() {

    fun parse(xml: String, flags: ManifestHintScanner.Flags): List<ManifestSection> {
        val collectors = mutableMapOf<SectionType, SectionCollector>()

        try {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var depth = 0
            var inApplication = false
            var currentSectionType: SectionType? = null
            var sectionDepth = 0
            val sectionBuilder = StringBuilder()

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        depth++
                        when {
                            // Depth 1: direct children of <manifest>
                            depth == 2 && !inApplication -> {
                                if (parser.name == "application") {
                                    inApplication = true
                                    // Application tag attributes go to OTHER
                                    val appAttrs = buildApplicationAttrsXml(parser)
                                    if (appAttrs.isNotEmpty()) {
                                        val collector = collectors.getOrPut(SectionType.OTHER) { SectionCollector() }
                                        collector.elements.add(appAttrs)
                                    }
                                } else {
                                    currentSectionType = mapTopLevelTag(parser.name)
                                    sectionDepth = depth
                                    sectionBuilder.clear()
                                    appendStartTag(sectionBuilder, parser, indent = 0)
                                }
                            }
                            // Depth 2: children of <application>
                            depth == 3 && inApplication -> {
                                currentSectionType = mapApplicationChildTag(parser.name)
                                sectionDepth = depth
                                sectionBuilder.clear()
                                appendStartTag(sectionBuilder, parser, indent = 0)
                            }
                            // Deeper elements within a section
                            currentSectionType != null -> {
                                val indent = depth - sectionDepth
                                appendStartTag(sectionBuilder, parser, indent)
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        when {
                            // Closing a top-level element (not application)
                            depth == 2 && !inApplication && currentSectionType != null -> {
                                appendEndTag(sectionBuilder, parser, indent = 0)
                                val collector = collectors.getOrPut(currentSectionType!!) { SectionCollector() }
                                collector.elements.add(sectionBuilder.toString())
                                currentSectionType = null
                            }
                            // Closing <application>
                            depth == 2 && inApplication && parser.name == "application" -> {
                                inApplication = false
                            }
                            // Closing an application child element
                            depth == 3 && inApplication && currentSectionType != null -> {
                                appendEndTag(sectionBuilder, parser, indent = 0)
                                val collector = collectors.getOrPut(currentSectionType!!) { SectionCollector() }
                                collector.elements.add(sectionBuilder.toString())
                                currentSectionType = null
                            }
                            // Closing deeper elements within a section
                            currentSectionType != null -> {
                                val indent = depth - sectionDepth - 1
                                appendEndTag(sectionBuilder, parser, indent)
                            }
                        }
                        depth--
                    }

                    XmlPullParser.TEXT -> {
                        if (currentSectionType != null) {
                            val text = parser.text?.trim()
                            if (!text.isNullOrEmpty()) {
                                val indent = depth - sectionDepth
                                sectionBuilder.append(INDENT.repeat(indent))
                                sectionBuilder.append(text)
                                sectionBuilder.append('\n')
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            log(TAG) { "Failed to parse manifest sections: $e" }
            // Fallback: return the entire XML as a single OTHER section
            return listOf(
                ManifestSection(
                    type = SectionType.OTHER,
                    elementCount = 1,
                    prettyXml = xml,
                    isFlagged = false,
                )
            )
        }

        return SECTION_ORDER
            .filter { collectors.containsKey(it) }
            .map { type ->
                val collector = collectors.getValue(type)
                ManifestSection(
                    type = type,
                    elementCount = collector.elements.size,
                    prettyXml = collector.elements.joinToString("\n"),
                    isFlagged = isFlagged(type, flags),
                )
            }
    }

    private fun buildApplicationAttrsXml(parser: XmlPullParser): String {
        if (parser.attributeCount == 0) return ""
        val sb = StringBuilder()
        sb.append("<application")
        for (i in 0 until parser.attributeCount) {
            sb.append('\n')
            sb.append(INDENT)
            val prefix = parser.getAttributePrefix(i)
            val name = if (!prefix.isNullOrEmpty()) "$prefix:${parser.getAttributeName(i)}" else parser.getAttributeName(i)
            sb.append("$name=\"${escapeXmlAttr(parser.getAttributeValue(i))}\"")
        }
        sb.append(" />")
        return sb.toString()
    }

    private fun appendStartTag(sb: StringBuilder, parser: XmlPullParser, indent: Int) {
        sb.append(INDENT.repeat(indent))
        sb.append('<')
        sb.append(parser.name)
        for (i in 0 until parser.attributeCount) {
            if (parser.attributeCount > 1) {
                sb.append('\n')
                sb.append(INDENT.repeat(indent + 1))
            } else {
                sb.append(' ')
            }
            val prefix = parser.getAttributePrefix(i)
            val name = if (!prefix.isNullOrEmpty()) "$prefix:${parser.getAttributeName(i)}" else parser.getAttributeName(i)
            sb.append("$name=\"${escapeXmlAttr(parser.getAttributeValue(i))}\"")
        }
        sb.append('>')
        sb.append('\n')
    }

    private fun appendEndTag(sb: StringBuilder, parser: XmlPullParser, indent: Int) {
        sb.append(INDENT.repeat(indent))
        sb.append("</")
        sb.append(parser.name)
        sb.append('>')
        sb.append('\n')
    }

    private fun escapeXmlAttr(value: String): String = value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun isFlagged(type: SectionType, flags: ManifestHintScanner.Flags): Boolean = when (type) {
        SectionType.QUERIES -> flags.hasActionMainQuery || flags.packageQueryCount > ManifestHintScanner.EXCESSIVE_THRESHOLD
        else -> false
    }

    private class SectionCollector {
        val elements = mutableListOf<String>()
    }

    companion object {
        private val TAG = logTag("Apps", "Manifest", "SectionParser")
        private const val INDENT = "  "

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
