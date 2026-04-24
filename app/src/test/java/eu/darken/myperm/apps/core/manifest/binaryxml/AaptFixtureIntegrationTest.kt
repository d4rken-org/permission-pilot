package eu.darken.myperm.apps.core.manifest.binaryxml

import eu.darken.myperm.apps.core.manifest.ManifestTextRenderer
import eu.darken.myperm.apps.core.manifest.QueriesExtractor
import eu.darken.myperm.apps.core.manifest.ResourceRefResolver
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import testhelper.BaseTest

/**
 * Cross-validates the streaming parser against real binary AXML blobs produced by aapt2.
 * Protects against the risk that our synthetic [testhelper.binaryxml.AxmlFixtureBuilder]
 * and the streamer share the same mental model of the format.
 *
 * Fixtures live in `app/src/test/resources/manifest/` and are regenerated via
 * `app/src/test/fixtures-src/generate.sh`.
 */
class AaptFixtureIntegrationTest : BaseTest() {

    private val nullResolver = ResourceRefResolver { null }

    private fun loadFixture(name: String): ByteArray {
        val resource = javaClass.classLoader!!.getResource("manifest/$name")
            ?: error("Fixture missing: manifest/$name — run app/src/test/fixtures-src/generate.sh")
        return resource.openStream().use { it.readBytes() }
    }

    private fun renderXml(name: String): String {
        val renderer = ManifestTextRenderer(nullResolver)
        BinaryXmlStreamer().parse(loadFixture(name), renderer)
        return renderer.result()
    }

    private fun extractQueries(name: String): eu.darken.myperm.apps.core.manifest.QueriesInfo {
        val extractor = QueriesExtractor()
        BinaryXmlStreamer().parse(loadFixture(name), extractor)
        return extractor.result()
    }

    @Test
    fun `simple fixture parses cleanly via the streamer`() {
        val rendered = renderXml("simple.manifest.bin")
        rendered.shouldContain("<manifest")
        rendered.shouldContain("com.example.simple")
        rendered.shouldContain("android:name=\"android.permission.INTERNET\"")
        rendered.shouldContain("Simple Fixture")
    }

    @Test
    fun `refs fixture resolves framework references with android prefix`() {
        val rendered = renderXml("refs.manifest.bin")
        rendered.shouldContain("<manifest")
        rendered.shouldContain("android:icon=")
        // Unresolved references emit @0xHHHHHHHH fallback (we use nullResolver).
        // Verify the format, not the specific framework id.
        (rendered.contains("@0x0") || rendered.contains("@android:")).shouldBeTrue()
    }

    @Test
    fun `queries fixture produces the expected QueriesInfo projection`() {
        val info = extractQueries("queries.manifest.bin")

        info.packageQueries shouldContain "com.google.android.gms"
        info.packageQueries shouldContain "com.android.vending"
        info.providerQueries shouldContain "com.android.contacts"

        info.intentQueries.size shouldBe 2
        val allActions = info.intentQueries.flatMap { it.actions }
        allActions.shouldContainAnyOf("android.intent.action.VIEW", "android.intent.action.MAIN")

        val viewIntent = info.intentQueries.firstOrNull { it.actions.contains("android.intent.action.VIEW") }
        viewIntent shouldBe info.intentQueries.first { it.actions.contains("android.intent.action.VIEW") }
        viewIntent!!.dataSpecs.any { it.contains("scheme=https") }.shouldBeTrue()
        viewIntent.dataSpecs.any { it.contains("host=example.com") }.shouldBeTrue()

        val mainIntent = info.intentQueries.first { it.actions.contains("android.intent.action.MAIN") }
        mainIntent.categories shouldContain "android.intent.category.LAUNCHER"
    }

    @Test
    fun `enum_flags fixture renders symbolic flag and enum values`() {
        val rendered = renderXml("enum_flags.manifest.bin")

        // protectionLevel="signature|privileged" -> ManifestEnumFlagNames maps bits 0x2|0x10.
        rendered.shouldContain("android:protectionLevel=\"signature|privileged\"")

        // launchMode="singleTask" (enum value 2)
        rendered.shouldContain("android:launchMode=\"singleTask\"")

        // screenOrientation="portrait" (enum value 1)
        rendered.shouldContain("android:screenOrientation=\"portrait\"")

        // configChanges is a flag bitmask; renderer joins known bits with '|'.
        rendered.shouldContain("android:configChanges=")
        rendered.shouldContain("mcc")
        rendered.shouldContain("locale")
        rendered.shouldContain("orientation")

        // windowSoftInputMode="stateHidden|adjustResize" (0x2|0x10)
        rendered.shouldContain("android:windowSoftInputMode=")
        rendered.shouldContain("stateHidden")
        rendered.shouldContain("adjustResize")
    }

    @Test
    fun `nulls fixture parses without exception`() {
        // Primary goal: the streamer completes without throwing. The exact emission of
        // `taskAffinity=""` depends on whether aapt2 encoded it as TYPE_STRING-empty or
        // TYPE_NULL-empty; both should be handled gracefully.
        val rendered = renderXml("nulls.manifest.bin")
        rendered.shouldContain("<manifest")
        rendered.shouldContain("com.example.nulls")
    }

    @Test
    fun `all fixtures parse without BinaryXmlException`() {
        val fixtures = listOf(
            "simple.manifest.bin",
            "refs.manifest.bin",
            "queries.manifest.bin",
            "enum_flags.manifest.bin",
            "nulls.manifest.bin",
        )
        for (fixture in fixtures) {
            val bytes = loadFixture(fixture)
            // Use a visitor that just counts events — we only care that parsing completes.
            val counter = object : BinaryXmlVisitor {
                var startElements = 0
                override fun onStartElement(
                    namespace: String?,
                    prefix: String?,
                    name: String,
                    attributes: List<BinaryXmlAttribute>,
                    lineNumber: Int,
                ) {
                    startElements++
                }
            }
            BinaryXmlStreamer().parse(bytes, counter)
            (counter.startElements > 0).shouldBeTrue()
        }
    }
}
