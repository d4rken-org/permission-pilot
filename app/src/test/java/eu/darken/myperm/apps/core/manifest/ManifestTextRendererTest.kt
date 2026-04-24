package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlStreamer
import eu.darken.myperm.apps.core.manifest.binaryxml.TypedValue
import io.kotest.matchers.booleans.shouldBeTrue
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

    @Test
    fun `boolean false attribute renders as false`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "application",
                attributes = listOf(
                    Attr(ANDROID_NS, "exported", type = AxmlFixtureBuilder.RES_TYPE_INT_BOOLEAN, data = 0)
                )
            ).endElement(null, "application")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("android:exported=\"false\"")
    }

    @Test
    fun `attribute reference resolved via resolver renders with question prefix`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "style",
                attributes = listOf(
                    Attr(ANDROID_NS, "theme", type = AxmlFixtureBuilder.RES_TYPE_ATTRIBUTE, data = 0x01010001)
                )
            ).endElement(null, "style")
            .build()

        val rendered = render(bytes) { id ->
            if (id == 0x01010001) "@android:attr/theme" else null
        }

        rendered.shouldContain("android:theme=\"?android:attr/theme\"")
    }

    @Test
    fun `unresolved attribute reference falls back to hex question form`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "style",
                attributes = listOf(
                    Attr(null, "theme", type = AxmlFixtureBuilder.RES_TYPE_ATTRIBUTE, data = 0x01010001)
                )
            ).endElement(null, "style")
            .build()

        val rendered = render(bytes) // resolver always returns null
        rendered.shouldContain("theme=\"?0x01010001\"")
    }

    @Test
    fun `attribute reference falls back to rawValueString when it starts with question mark`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "style",
                attributes = listOf(
                    Attr(
                        namespace = null,
                        name = "theme",
                        type = AxmlFixtureBuilder.RES_TYPE_ATTRIBUTE,
                        data = 0x01010001,
                        rawValueString = "?attr/customTheme",
                    )
                )
            ).endElement(null, "style")
            .build()

        val rendered = render(bytes) // resolver returns null -> fall back to raw
        rendered.shouldContain("theme=\"?attr/customTheme\"")
        rendered.shouldNotContain("0x01010001")
    }

    @Test
    fun `DynamicAttributeRef uses question prefix when resolved`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "x", type = AxmlFixtureBuilder.RES_TYPE_DYNAMIC_ATTRIBUTE, data = 0x7F010001)
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes) { id ->
            if (id == 0x7F010001) "@attr/custom" else null
        }

        rendered.shouldContain("x=\"?attr/custom\"")
    }

    @Test
    fun `color ARGB8 renders as eight-digit hash`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "c", type = AxmlFixtureBuilder.RES_TYPE_INT_COLOR_ARGB8, data = 0x80FF0000.toInt())
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("c=\"#80FF0000\"")
    }

    @Test
    fun `color RGB8 renders as six-digit hash without alpha`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "c", type = AxmlFixtureBuilder.RES_TYPE_INT_COLOR_RGB8, data = 0xFF00FF)
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("c=\"#FF00FF\"")
    }

    @Test
    fun `color ARGB4 extracts high nibbles of each byte`() {
        // AOSP stores #ARGB as the 32-bit pattern 0xAARRGGBB with each byte being a nibble
        // replicated to itself. Data 0x11223344 should collapse back to #1234.
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "c", type = AxmlFixtureBuilder.RES_TYPE_INT_COLOR_ARGB4, data = 0x11223344)
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("c=\"#1234\"")
    }

    @Test
    fun `color RGB4 drops alpha and extracts high nibbles of RGB bytes`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "c", type = AxmlFixtureBuilder.RES_TYPE_INT_COLOR_RGB4, data = 0xFF223344.toInt())
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("c=\"#234\"")
    }

    @Test
    fun `dimension in dp with integer mantissa renders with dp suffix`() {
        // mantissa = 16 (bits 8-31), radix = 0 (bits 4-5 = 23.0 integer scale), unit = 1 (dp).
        val raw = (16 shl 8) or (0 shl 4) or 1
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "d", type = AxmlFixtureBuilder.RES_TYPE_DIMENSION, data = raw)
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("d=\"16dp\"")
    }

    @Test
    fun `dimension unit mapping covers px sp pt in mm`() {
        val units = mapOf(0 to "px", 2 to "sp", 3 to "pt", 4 to "in", 5 to "mm")
        for ((unit, suffix) in units) {
            val raw = (8 shl 8) or (0 shl 4) or unit
            val bytes = AxmlFixtureBuilder()
                .startElement(
                    null, "foo",
                    attributes = listOf(
                        Attr(null, "d", type = AxmlFixtureBuilder.RES_TYPE_DIMENSION, data = raw)
                    )
                ).endElement(null, "foo")
                .build()
            val rendered = render(bytes)
            rendered.shouldContain("d=\"8$suffix\"")
        }
    }

    @Test
    fun `fraction unit 0 emits percent suffix`() {
        // Focus on the suffix mapping (fractionSuffix): 0 -> "%". The exact numeric output is
        // intentionally unasserted — the renderer's fraction formatting doesn't apply Android's
        // `* 100` display convention, so asserting a specific number here would either hide that
        // discrepancy or lock us in to a non-standard output.
        val raw = (1 shl 8) or (0 shl 4) or 0
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "f", type = AxmlFixtureBuilder.RES_TYPE_FRACTION, data = raw)
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        Regex("""f="[^"]*%"""").containsMatchIn(rendered).shouldBeTrue()
    }

    @Test
    fun `fraction unit 1 emits percent-p suffix`() {
        val raw = (1 shl 8) or (0 shl 4) or 1
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "f", type = AxmlFixtureBuilder.RES_TYPE_FRACTION, data = raw)
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        Regex("""f="[^"]*%p"""").containsMatchIn(rendered).shouldBeTrue()
    }

    @Test
    fun `float renders via toString`() {
        val pi = Math.PI.toFloat()
        val bytes = AxmlFixtureBuilder()
            .startElement(
                null, "foo",
                attributes = listOf(
                    Attr(null, "pi", type = AxmlFixtureBuilder.RES_TYPE_FLOAT, data = pi.toRawBits())
                )
            ).endElement(null, "foo")
            .build()

        val rendered = render(bytes)
        rendered.shouldContain("pi=\"$pi\"")
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
