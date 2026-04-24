package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlStreamer
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.binaryxml.AxmlFixtureBuilder
import testhelper.binaryxml.AxmlFixtureBuilder.Attr

class QueriesExtractorTest : BaseTest() {

    private fun parse(bytes: ByteArray): QueriesInfo {
        val extractor = QueriesExtractor()
        BinaryXmlStreamer().parse(bytes, extractor)
        return extractor.result()
    }

    private fun AxmlFixtureBuilder.strAttr(ns: String?, name: String, value: String) =
        Attr(ns, name, type = AxmlFixtureBuilder.RES_TYPE_STRING, data = 0, rawValueString = value)

    @Test
    fun `empty manifest has empty queries`() {
        val bytes = AxmlFixtureBuilder()
            .startElement(null, "manifest")
            .endElement(null, "manifest")
            .build()

        val info = parse(bytes)
        info.packageQueries shouldBe emptyList()
        info.intentQueries shouldBe emptyList()
        info.providerQueries shouldBe emptyList()
    }

    @Test
    fun `package and provider children of queries are captured`() {
        val b = AxmlFixtureBuilder()
        val bytes = b
            .startElement(null, "manifest")
            .startElement(null, "queries")
            .startElement(
                null,
                "package",
                attributes = listOf(b.strAttr(ANDROID_NS, "name", "com.target"))
            ).endElement(null, "package")
            .startElement(
                null,
                "provider",
                attributes = listOf(b.strAttr(ANDROID_NS, "authorities", "com.foo.provider"))
            ).endElement(null, "provider")
            .endElement(null, "queries")
            .endElement(null, "manifest")
            .build()

        val info = parse(bytes)
        info.packageQueries shouldContainExactly listOf("com.target")
        info.providerQueries shouldContainExactly listOf("com.foo.provider")
    }

    @Test
    fun `intent with action data and category maintains correct order`() {
        val b = AxmlFixtureBuilder()
        val bytes = b
            .startElement(null, "manifest")
            .startElement(null, "queries")
            .startElement(null, "intent")
            .startElement(
                null,
                "action",
                attributes = listOf(b.strAttr(ANDROID_NS, "name", "android.intent.action.VIEW"))
            ).endElement(null, "action")
            .startElement(
                null,
                "data",
                attributes = listOf(
                    b.strAttr(ANDROID_NS, "scheme", "https"),
                    b.strAttr(ANDROID_NS, "host", "example.com"),
                )
            ).endElement(null, "data")
            .startElement(
                null,
                "category",
                attributes = listOf(b.strAttr(ANDROID_NS, "name", "android.intent.category.BROWSABLE"))
            ).endElement(null, "category")
            .endElement(null, "intent")
            .endElement(null, "queries")
            .endElement(null, "manifest")
            .build()

        val info = parse(bytes)
        info.intentQueries.size shouldBe 1
        val intent = info.intentQueries[0]
        intent.actions shouldContainExactly listOf("android.intent.action.VIEW")
        intent.dataSpecs shouldContainExactly listOf("scheme=https, host=example.com")
        intent.categories shouldContainExactly listOf("android.intent.category.BROWSABLE")
    }

    @Test
    fun `nested package inside intent is ignored`() {
        val b = AxmlFixtureBuilder()
        val bytes = b
            .startElement(null, "manifest")
            .startElement(null, "queries")
            .startElement(null, "intent")
            .startElement(
                null, "package",
                attributes = listOf(b.strAttr(ANDROID_NS, "name", "com.nested"))
            ).endElement(null, "package")
            .endElement(null, "intent")
            .endElement(null, "queries")
            .endElement(null, "manifest")
            .build()

        val info = parse(bytes)
        info.packageQueries shouldBe emptyList()
    }

    @Test
    fun `two queries blocks are merged`() {
        val b = AxmlFixtureBuilder()
        val bytes = b
            .startElement(null, "manifest")
            .startElement(null, "queries")
            .startElement(null, "package",
                attributes = listOf(b.strAttr(ANDROID_NS, "name", "com.a"))
            ).endElement(null, "package")
            .endElement(null, "queries")
            .startElement(null, "queries")
            .startElement(null, "package",
                attributes = listOf(b.strAttr(ANDROID_NS, "name", "com.b"))
            ).endElement(null, "package")
            .endElement(null, "queries")
            .endElement(null, "manifest")
            .build()

        val info = parse(bytes)
        info.packageQueries shouldContainExactly listOf("com.a", "com.b")
    }

    @Test
    fun `action MAIN in intent flows through hint scanner flags`() {
        val b = AxmlFixtureBuilder()
        val bytes = b
            .startElement(null, "manifest")
            .startElement(null, "queries")
            .startElement(null, "intent")
            .startElement(null, "action",
                attributes = listOf(b.strAttr(ANDROID_NS, "name", "android.intent.action.MAIN"))
            ).endElement(null, "action")
            .endElement(null, "intent")
            .endElement(null, "queries")
            .endElement(null, "manifest")
            .build()

        val info = parse(bytes)
        val flags = ManifestHintScanner().evaluate(info)
        flags.hasActionMainQuery shouldBe true
    }

    @Test
    fun `package with non-string name attribute is silently skipped`() {
        val b = AxmlFixtureBuilder()
        val bytes = b
            .startElement(null, "manifest")
            .startElement(null, "queries")
            .startElement(
                null, "package",
                attributes = listOf(
                    Attr(ANDROID_NS, "name", type = AxmlFixtureBuilder.RES_TYPE_REFERENCE, data = 0x7F110005)
                )
            ).endElement(null, "package")
            .endElement(null, "queries")
            .endElement(null, "manifest")
            .build()

        val info = parse(bytes)
        info.packageQueries shouldBe emptyList()
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
