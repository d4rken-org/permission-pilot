package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlStreamer
import eu.darken.myperm.apps.core.manifest.binaryxml.TypedValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.binaryxml.AxmlFixtureBuilder
import testhelper.binaryxml.AxmlFixtureBuilder.Attr

class ManifestTextRendererTest : BaseTest() {

    private fun render(
        bytes: ByteArray,
        resolver: ResourceRefResolver = ResourceRefResolver { null },
    ): String {
        val renderer = ManifestTextRenderer(resolver)
        BinaryXmlStreamer().parse(bytes, renderer)
        return renderer.result()
    }

    private fun AxmlFixtureBuilder.strAttr(ns: String?, name: String, value: String) =
        Attr(ns, name, type = AxmlFixtureBuilder.RES_TYPE_STRING, data = 0, rawValueString = value)

    @Test
    fun `renders element with android namespace prefix even when stack does not resolve`() {
        val bytes = AxmlFixtureBuilder()
            .startNamespace("a", ANDROID_NS) // bound prefix is "a" — viewer must still render "android:"
            .startElement(
                null, "manifest",
                attributes = listOf(
                    AxmlFixtureBuilder().strAttr(ANDROID_NS, "package", "com.example")
                )
            )
            .endElement(null, "manifest")
            .endNamespace("a", ANDROID_NS)
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("android:package=\"com.example\"")
        rendered.shouldContain("xmlns:android=\"http://schemas.android.com/apk/res/android\"")
    }

    @Test
    fun `reference resolved via resolver becomes symbolic`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "manifest",
                attributes = listOf(
                    Attr(
                        namespace = ANDROID_NS,
                        name = "icon",
                        type = AxmlFixtureBuilder.RES_TYPE_REFERENCE,
                        data = 0x7F020001,
                    )
                )
            ).endElement(null, "manifest")
            .build()

        val rendered = render(bytes) { id ->
            if (id == 0x7F020001) "@drawable/ic_launcher" else null
        }

        rendered.shouldContain("android:icon=\"@drawable/ic_launcher\"")
    }

    @Test
    fun `unresolved reference falls back to hex`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "manifest",
                attributes = listOf(
                    Attr(null, "x", type = AxmlFixtureBuilder.RES_TYPE_REFERENCE, data = 0x7F020001)
                )
            ).endElement(null, "manifest")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("x=\"@0x7F020001\"")
    }

    @Test
    fun `reference falls back to raw value string when resolver fails but raw is symbolic`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "manifest",
                attributes = listOf(
                    Attr(
                        namespace = null,
                        name = "icon",
                        type = AxmlFixtureBuilder.RES_TYPE_REFERENCE,
                        data = 0x7F020001,
                        rawValueString = "@drawable/ic_oem",
                    )
                )
            ).endElement(null, "manifest")
            .build()

        val rendered = render(bytes) // resolver always returns null
        rendered.shouldContain("icon=\"@drawable/ic_oem\"")
        rendered.shouldNotContain("0x7F020001")
    }

    @Test
    fun `boolean attribute renders as true or false`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "application",
                attributes = listOf(
                    Attr(ANDROID_NS, "exported", type = AxmlFixtureBuilder.RES_TYPE_INT_BOOLEAN, data = 1)
                )
            ).endElement(null, "application")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("android:exported=\"true\"")
    }

    @Test
    fun `null undefined renders as at-null`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "x", type = AxmlFixtureBuilder.RES_TYPE_NULL, data = 0),
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("x=\"@null\"")
    }

    @Test
    fun `null empty renders as empty string`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "x", type = AxmlFixtureBuilder.RES_TYPE_NULL, data = 1),
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("x=\"\"")
    }

    @Test
    fun `string values are xml escaped`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    AxmlFixtureBuilder().strAttr(null, "x", "a & b < c > \"d\"")
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("a &amp; b &lt; c &gt; &quot;d&quot;")
    }

    @Test
    fun `enum flag mapping formats protectionLevel`() {
        val protectionLevelResId = 0x01010009
        // Pre-seed the string pool so "protectionLevel" sits at index 0; resourceMap[0] then
        // carries its framework resource ID. Other strings added by the builder get indices >= 1
        // and lack resource IDs (resourceMap is size 1).
        val b = AxmlFixtureBuilder()
        b.str("protectionLevel")
        b.resourceMap(IntArray(1) { protectionLevelResId })
        val bytes = b
            .startElement(
                null, "permission",
                attributes = listOf(
                    Attr(
                        namespace = ANDROID_NS,
                        name = "protectionLevel",
                        type = AxmlFixtureBuilder.RES_TYPE_INT_HEX,
                        data = 0x12,  // signature | privileged = 0x2 | 0x10
                    )
                )
            ).endElement(null, "permission")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("signature|privileged")
    }

    @Test
    fun `self-closing empty element is emitted with slash-greater`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(null, "manifest")
            .startElement(null, "uses-sdk")
            .endElement(null, "uses-sdk")
            .endElement(null, "manifest")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("<uses-sdk />")
    }

    @Test
    fun `int hex and dec distinguish format`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "dec", type = AxmlFixtureBuilder.RES_TYPE_INT_DEC, data = 42),
                    Attr(null, "hex", type = AxmlFixtureBuilder.RES_TYPE_INT_HEX, data = 0xABCD),
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("dec=\"42\"")
        rendered.shouldContain("hex=\"0xABCD\"")
    }

    @Test
    fun `namespaced attribute without stack push still uses prefix for android uri`() {
        // Namespace never declared — but we still render android: for the android URI.
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    AxmlFixtureBuilder().strAttr(ANDROID_NS, "name", "x")
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("android:name=\"x\"")
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
