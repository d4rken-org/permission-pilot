package eu.darken.myperm.apps.core.manifest.binaryxml

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.binaryxml.AxmlFixtureBuilder
import testhelper.binaryxml.AxmlFixtureBuilder.Attr

class BinaryXmlStreamerTest : BaseTest() {

    private class Recorder : BinaryXmlVisitor {
        val events = mutableListOf<String>()
        val attrs = mutableMapOf<String, List<BinaryXmlAttribute>>()

        override fun onStartDocument() {
            events += "startDocument"
        }

        override fun onStartNamespace(prefix: String, uri: String) {
            events += "ns+ $prefix=$uri"
        }

        override fun onEndNamespace(prefix: String, uri: String) {
            events += "ns- $prefix=$uri"
        }

        override fun onStartElement(
            namespace: String?,
            prefix: String?,
            name: String,
            attributes: List<BinaryXmlAttribute>,
            lineNumber: Int,
        ) {
            events += "start $prefix:$name"
            attrs[name] = attributes
        }

        override fun onEndElement(namespace: String?, prefix: String?, name: String, lineNumber: Int) {
            events += "end $prefix:$name"
        }

        override fun onCdata(text: String, lineNumber: Int) {
            events += "cdata $text"
        }

        override fun onEndDocument() {
            events += "endDocument"
        }
    }

    @Test
    fun `simple element with namespace produces expected events`() {
        val bytes = AxmlFixtureBuilder()
            .startNamespace("android", ANDROID_NS)
            .startElement(null, "manifest")
            .endElement(null, "manifest")
            .endNamespace("android", ANDROID_NS)
            .build()

        val rec = Recorder()
        BinaryXmlStreamer().parse(bytes, rec)

        rec.events shouldContainExactly listOf(
            "startDocument",
            "ns+ android=$ANDROID_NS",
            "start null:manifest",
            "end null:manifest",
            "ns- android=$ANDROID_NS",
            "endDocument",
        )
    }

    @Test
    fun `android-namespaced attribute resolves prefix from namespace stack`() {
        val bytes = AxmlFixtureBuilder()
            .startNamespace("android", ANDROID_NS)
            .startElement(
                namespace = null,
                name = "manifest",
                attributes = listOf(
                    Attr(
                        namespace = ANDROID_NS,
                        name = "package",
                        type = AxmlFixtureBuilder.RES_TYPE_STRING,
                        data = 0,
                        rawValueString = "com.example",
                    )
                )
            )
            .endElement(null, "manifest")
            .endNamespace("android", ANDROID_NS)
            .build()

        val rec = Recorder()
        BinaryXmlStreamer().parse(bytes, rec)

        val manifestAttrs = rec.attrs.getValue("manifest")
        manifestAttrs.size shouldBe 1
        val attr = manifestAttrs[0]
        attr.namespace shouldBe ANDROID_NS
        attr.prefix shouldBe "android"
        attr.name shouldBe "package"
        (attr.typedValue as TypedValue.Str).value shouldBe "com.example"
    }

    @Test
    fun `unknown typed-value type surfaces as Unknown`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                namespace = null,
                name = "foo",
                attributes = listOf(Attr(null, "x", type = 0x42, data = 0xCAFEBABE.toInt()))
            )
            .endElement(null, "foo")
            .build()

        val rec = Recorder()
        BinaryXmlStreamer().parse(bytes, rec)

        val attr = rec.attrs.getValue("foo")[0]
        val tv = attr.typedValue as TypedValue.Unknown
        tv.type shouldBe 0x42
        tv.raw shouldBe 0xCAFEBABE.toInt()
    }

    @Test
    fun `Null typed value distinguishes undefined vs empty`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                namespace = null,
                name = "foo",
                attributes = listOf(
                    Attr(null, "undef", type = AxmlFixtureBuilder.RES_TYPE_NULL, data = 0),   // DATA_NULL_UNDEFINED
                    Attr(null, "empty", type = AxmlFixtureBuilder.RES_TYPE_NULL, data = 1),   // DATA_NULL_EMPTY
                )
            )
            .endElement(null, "foo")
            .build()

        val rec = Recorder()
        BinaryXmlStreamer().parse(bytes, rec)

        val attrs = rec.attrs.getValue("foo")
        (attrs[0].typedValue as TypedValue.Null).undefined shouldBe true
        (attrs[1].typedValue as TypedValue.Null).undefined shouldBe false
    }

    @Test
    fun `boolean encoded as zero or non-zero`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "t", type = AxmlFixtureBuilder.RES_TYPE_INT_BOOLEAN, data = 1),
                    Attr(null, "f", type = AxmlFixtureBuilder.RES_TYPE_INT_BOOLEAN, data = 0),
                )
            )
            .endElement(null, "foo")
            .build()

        val rec = Recorder()
        BinaryXmlStreamer().parse(bytes, rec)

        val attrs = rec.attrs.getValue("foo")
        (attrs[0].typedValue as TypedValue.Bool).value shouldBe true
        (attrs[1].typedValue as TypedValue.Bool).value shouldBe false
    }

    @Test
    fun `float decoded via Float fromBits`() {
        val pi = Math.PI.toFloat()
        val bits = pi.toRawBits()
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(Attr(null, "pi", type = AxmlFixtureBuilder.RES_TYPE_FLOAT, data = bits))
            )
            .endElement(null, "foo")
            .build()

        val rec = Recorder()
        BinaryXmlStreamer().parse(bytes, rec)
        val attrs = rec.attrs.getValue("foo")
        (attrs[0].typedValue as TypedValue.Flt).value shouldBe pi
    }

    @Test
    fun `non-xml root chunk is rejected`() {
        val bogus = ByteArray(8).also {
            it[0] = 0x01; it[1] = 0x00 // type = 0x0001 (string pool)
            it[2] = 0x08; it[3] = 0x00 // headerSize = 8
            it[4] = 0x08; it[5] = 0x00; it[6] = 0x00; it[7] = 0x00 // size = 8
        }
        val ex = shouldThrow<BinaryXmlException> { BinaryXmlStreamer().parse(bogus, Recorder()) }
        ex.message!!.shouldContain("RES_XML_TYPE")
    }

    @Test
    fun `negative chunk size is rejected`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(null, "foo")
            .endElement(null, "foo")
            .build()
        // Corrupt the first sub-chunk's size field (bytes 12..15) to negative.
        // Root header is 8 bytes, string pool chunk starts at offset 8.
        // String pool header: type(2), headerSize(2), size(4) — size field at 12..15.
        bytes[12] = 0xFF.toByte()
        bytes[13] = 0xFF.toByte()
        bytes[14] = 0xFF.toByte()
        bytes[15] = 0xFF.toByte()
        val ex = shouldThrow<BinaryXmlException> { BinaryXmlStreamer().parse(bytes, Recorder()) }
        ex.message!!.shouldContain("chunkSize")
    }

    @Test
    fun `chunk size exceeding buffer is rejected`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(null, "foo")
            .endElement(null, "foo")
            .build()
        // Set string pool chunkSize to a huge value.
        bytes[12] = 0x00
        bytes[13] = 0x00
        bytes[14] = 0x00
        bytes[15] = 0x7F
        val ex = shouldThrow<BinaryXmlException> { BinaryXmlStreamer().parse(bytes, Recorder()) }
        (ex.message!!.contains("chunkSize") || ex.message!!.contains("remaining")).shouldBeTrue()
    }

    @Test
    fun `utf16 string pool decodes correctly`() {
        val bytes = AxmlFixtureBuilder()
            .utf8(false)
            .startElement(null, "hello")
            .endElement(null, "hello")
            .build()

        val rec = Recorder()
        BinaryXmlStreamer().parse(bytes, rec)
        rec.events.any { it.contains("start null:hello") }.shouldBeTrue()
    }

    @Test
    fun `element with attributeSize larger than 20 skips extra bytes`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "a", type = AxmlFixtureBuilder.RES_TYPE_INT_DEC, data = 42),
                    Attr(null, "b", type = AxmlFixtureBuilder.RES_TYPE_INT_DEC, data = 43),
                ),
                attributeSize = 24, // forward-compat: extra 4 bytes per attr record
            )
            .endElement(null, "foo")
            .build()

        val rec = Recorder()
        BinaryXmlStreamer().parse(bytes, rec)
        val attrs = rec.attrs.getValue("foo")
        attrs.size shouldBe 2
        (attrs[0].typedValue as TypedValue.IntDec).value shouldBe 42
        (attrs[1].typedValue as TypedValue.IntDec).value shouldBe 43
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
