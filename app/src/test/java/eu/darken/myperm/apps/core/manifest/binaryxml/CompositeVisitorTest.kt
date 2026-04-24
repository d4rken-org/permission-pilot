package eu.darken.myperm.apps.core.manifest.binaryxml

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.binaryxml.AxmlFixtureBuilder
import testhelper.binaryxml.AxmlFixtureBuilder.Attr

class CompositeVisitorTest : BaseTest() {

    private class Recorder(private val tag: String) : BinaryXmlVisitor {
        val events = mutableListOf<String>()
        override fun onStartDocument() {
            events += "$tag:startDoc"
        }

        override fun onEndDocument() {
            events += "$tag:endDoc"
        }

        override fun onStartNamespace(prefix: String, uri: String) {
            events += "$tag:ns+ $prefix"
        }

        override fun onEndNamespace(prefix: String, uri: String) {
            events += "$tag:ns- $prefix"
        }

        override fun onStartElement(
            namespace: String?,
            prefix: String?,
            name: String,
            attributes: List<BinaryXmlAttribute>,
            lineNumber: Int,
        ) {
            events += "$tag:start $name(${attributes.size})"
        }

        override fun onEndElement(namespace: String?, prefix: String?, name: String, lineNumber: Int) {
            events += "$tag:end $name"
        }

        override fun onCdata(text: String, lineNumber: Int) {
            events += "$tag:cdata ${text.trim()}"
        }
    }

    @Test
    fun `each event is forwarded to every visitor in registration order`() {
        val a = Recorder("A")
        val b = Recorder("B")
        val c = Recorder("C")

        val composite = CompositeVisitor(listOf(a, b, c))
        composite.onStartDocument()
        composite.onStartNamespace("android", "uri")
        composite.onStartElement(null, null, "manifest", emptyList(), 1)
        composite.onCdata("hello", 2)
        composite.onEndElement(null, null, "manifest", 3)
        composite.onEndNamespace("android", "uri")
        composite.onEndDocument()

        // Every visitor must receive the full event sequence — not just a matching count, since
        // a dispatch bug could easily preserve counts while dropping payloads for later visitors.
        fun expected(tag: String) = listOf(
            "$tag:startDoc",
            "$tag:ns+ android",
            "$tag:start manifest(0)",
            "$tag:cdata hello",
            "$tag:end manifest",
            "$tag:ns- android",
            "$tag:endDoc",
        )

        a.events shouldContainExactly expected("A")
        b.events shouldContainExactly expected("B")
        c.events shouldContainExactly expected("C")
    }

    @Test
    fun `integrates with streamer over a real fixture`() {
        // End-to-end: stream a tiny manifest through CompositeVisitor(left, right) and confirm
        // both visitors see the full event stream (no cross-talk, no dropped events).
        val b = AxmlFixtureBuilder()
        val bytes = b
            .startNamespace("android", ANDROID_NS)
            .startElement(
                null, "manifest",
                attributes = listOf(
                    Attr(ANDROID_NS, "package", type = AxmlFixtureBuilder.RES_TYPE_STRING, data = 0, rawValueString = "com.ex")
                )
            )
            .startElement(null, "queries")
            .startElement(
                null, "package",
                attributes = listOf(
                    Attr(ANDROID_NS, "name", type = AxmlFixtureBuilder.RES_TYPE_STRING, data = 0, rawValueString = "com.target")
                )
            )
            .endElement(null, "package")
            .endElement(null, "queries")
            .endElement(null, "manifest")
            .endNamespace("android", ANDROID_NS)
            .build()

        // Both recorders share the same tag so the captured event sequences can be compared directly.
        val left = Recorder("X")
        val right = Recorder("X")
        BinaryXmlStreamer().parse(bytes, CompositeVisitor(listOf(left, right)))

        // Both visitors saw the same sequence of events.
        left.events shouldBe right.events
        // Sequence is non-trivial (would be empty on a fan-out bug): manifest/queries/package
        // produces start+end pairs plus namespace and document brackets.
        (left.events.size >= 10) shouldBe true
    }

    @Test
    fun `empty visitor list is a no-op safe fallback`() {
        val composite = CompositeVisitor(emptyList())
        // Must not throw regardless of event. (Defensive: ApkManifestReader could in theory pass
        // an empty composite — still valid behavior.)
        composite.onStartDocument()
        composite.onStartElement(null, null, "x", emptyList(), 1)
        composite.onEndElement(null, null, "x", 2)
        composite.onEndDocument()
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
