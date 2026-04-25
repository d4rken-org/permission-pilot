package eu.darken.myperm.apps.core.manifest.binaryxml

import eu.darken.myperm.apps.core.manifest.ManifestSection
import eu.darken.myperm.apps.core.manifest.ManifestSectionVisitor
import eu.darken.myperm.apps.core.manifest.QueriesExtractor
import eu.darken.myperm.apps.core.manifest.ResourceRefResolver
import eu.darken.myperm.apps.core.manifest.SectionType
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import testhelper.BaseTest

/**
 * Cross-validates the streaming parser + section visitor against real binary AXML blobs
 * produced by aapt2. Protects against the risk that our synthetic
 * [testhelper.binaryxml.AxmlFixtureBuilder] and the streamer share the same mental model
 * of the format.
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

    private fun runVisitor(name: String): List<ManifestSection> {
        val visitor = ManifestSectionVisitor(nullResolver)
        BinaryXmlStreamer().parse(loadFixture(name), visitor)
        return visitor.result()
    }

    private fun extractQueries(name: String): eu.darken.myperm.apps.core.manifest.QueriesInfo {
        val extractor = QueriesExtractor()
        BinaryXmlStreamer().parse(loadFixture(name), extractor)
        return extractor.result()
    }

    @Test
    fun `simple fixture parses cleanly via the section visitor`() {
        val sections = runVisitor("simple.manifest.bin")
        val perm = sections.first { it.type == SectionType.USES_PERMISSION }
        perm.prettyXml.shouldContain("android:name=\"android.permission.INTERNET\"")

        val other = sections.first { it.type == SectionType.OTHER }
        // <application>'s label/icon attributes go to OTHER as a self-closing fragment.
        other.prettyXml.shouldContain("<application")
        other.prettyXml.shouldContain("Simple Fixture")
    }

    @Test
    fun `refs fixture resolves framework references in the application attributes`() {
        val sections = runVisitor("refs.manifest.bin")
        val other = sections.first { it.type == SectionType.OTHER }
        other.prettyXml.shouldContain("android:icon=")
        // Unresolved references emit @0xHHHHHHHH fallback (we use nullResolver).
        // Verify the format, not the specific framework id.
        (other.prettyXml.contains("@0x0") || other.prettyXml.contains("@android:")).shouldBeTrue()
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
    fun `enum_flags fixture renders symbolic flag and enum values via the formatter`() {
        val sections = runVisitor("enum_flags.manifest.bin")

        // protectionLevel="signature|privileged" -> ManifestEnumFlagNames maps bits 0x2|0x10.
        // Lives on a <permission> declaration, so the PERMISSION section.
        val perm = sections.first { it.type == SectionType.PERMISSION }
        perm.prettyXml.shouldContain("android:protectionLevel=\"signature|privileged\"")

        // launchMode + screenOrientation + configChanges + windowSoftInputMode all live on
        // the <activity>, which buckets to ACTIVITIES.
        val activities = sections.first { it.type == SectionType.ACTIVITIES }
        activities.prettyXml.shouldContain("android:launchMode=\"singleTask\"")
        activities.prettyXml.shouldContain("android:screenOrientation=\"portrait\"")
        activities.prettyXml.shouldContain("android:configChanges=")
        activities.prettyXml.shouldContain("mcc")
        activities.prettyXml.shouldContain("locale")
        activities.prettyXml.shouldContain("orientation")
        activities.prettyXml.shouldContain("android:windowSoftInputMode=")
        activities.prettyXml.shouldContain("stateHidden")
        activities.prettyXml.shouldContain("adjustResize")
    }

    @Test
    fun `nulls fixture parses without exception and produces some sections`() {
        val sections = runVisitor("nulls.manifest.bin")
        sections.isNotEmpty().shouldBeTrue()
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
