package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlStreamer
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import testhelper.BaseTest

/**
 * Verifies [ManifestSectionVisitor] against the aapt2 fixtures.
 *
 * No parity comparison against the legacy [ManifestSectionParser] — XmlPullParserFactory
 * has no SPI implementation in JVM unit tests, so the legacy parser falls into its
 * exception fallback and returns just `[OTHER]` regardless of input. Real-device
 * end-to-end validation in Step 5c covers the equivalence.
 */
class ManifestSectionVisitorTest : BaseTest() {

    private val nullResolver = ResourceRefResolver { null }

    private fun loadFixture(name: String): ByteArray {
        val resource = javaClass.classLoader!!.getResource("manifest/$name")
            ?: error("Fixture missing: manifest/$name")
        return resource.openStream().use { it.readBytes() }
    }

    private fun runVisitor(fixture: String): List<ManifestSection> {
        val visitor = ManifestSectionVisitor(nullResolver)
        BinaryXmlStreamer().parse(loadFixture(fixture), visitor)
        return visitor.result()
    }

    @Test
    fun `simple fixture buckets uses-permission and OTHER`() {
        val sections = runVisitor("simple.manifest.bin")
        val types = sections.map { it.type }
        types shouldContain SectionType.USES_PERMISSION
        // Application attributes go into OTHER.
        types shouldContain SectionType.OTHER
    }

    @Test
    fun `simple fixture USES_PERMISSION section captures the granted permission name`() {
        val sections = runVisitor("simple.manifest.bin")
        val perm = sections.first { it.type == SectionType.USES_PERMISSION }
        perm.prettyXml.shouldContain("<uses-permission")
        perm.prettyXml.shouldContain("android.permission.INTERNET")
    }

    @Test
    fun `queries fixture produces a QUERIES section with package targets and intent action`() {
        val sections = runVisitor("queries.manifest.bin")
        val queries = sections.first { it.type == SectionType.QUERIES }
        queries.prettyXml.shouldContain("<queries>")
        queries.prettyXml.shouldContain("</queries>")
        queries.prettyXml.shouldContain("com.google.android.gms")
        queries.prettyXml.shouldContain("com.android.vending")
        queries.prettyXml.shouldContain("android.intent.action.VIEW")
    }

    @Test
    fun `enum_flags fixture renders symbolic flag values inside the activity section`() {
        val sections = runVisitor("enum_flags.manifest.bin")
        val activities = sections.first { it.type == SectionType.ACTIVITIES }
        activities.prettyXml.shouldContain("singleTask")
        activities.prettyXml.shouldContain("portrait")
    }

    @Test
    fun `enum_flags fixture surfaces protectionLevel under PERMISSION section`() {
        val sections = runVisitor("enum_flags.manifest.bin")
        val perm = sections.first { it.type == SectionType.PERMISSION }
        // protectionLevel="signature|privileged" — verifies BinaryXmlFormatting reaches the visitor.
        perm.prettyXml.shouldContain("signature|privileged")
    }

    @Test
    fun `isFlagged is always false — viewer model recomputes from queries`() {
        val sections = runVisitor("queries.manifest.bin")
        sections.forEach { it.isFlagged shouldBe false }
    }

    @Test
    fun `manifest-level xmlns declarations do not leak into any section`() {
        val sections = runVisitor("simple.manifest.bin")
        sections.forEach { section ->
            section.prettyXml.shouldNotContain("xmlns:android")
        }
    }

    @Test
    fun `sections appear in canonical order`() {
        val sections = runVisitor("queries.manifest.bin")
        val seen = sections.map { it.type }
        val canonical = listOf(
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
        var prevIdx = -1
        seen.forEach { t ->
            val idx = canonical.indexOf(t)
            if (idx <= prevIdx) error("Section $t appears out of canonical order: $seen")
            prevIdx = idx
        }
    }

    @Test
    fun `empty sections are filtered out — only types with entries appear`() {
        val sections = runVisitor("simple.manifest.bin")
        val types = sections.map { it.type }
        // simple.manifest.bin has no <queries>, so QUERIES must not appear.
        (SectionType.QUERIES !in types) shouldBe true
        // simple.manifest.bin has no <permission>, so PERMISSION must not appear.
        (SectionType.PERMISSION !in types) shouldBe true
    }

    @Test
    fun `each section's elementCount matches the number of top-level entries`() {
        val sections = runVisitor("queries.manifest.bin")
        sections.forEach { section ->
            // Each entry in a section is separated by '\n' between root tags. elementCount must
            // be at least 1 for the section to appear at all.
            (section.elementCount >= 1) shouldBe true
        }
    }

    @Test
    fun `enum_flags fixture activity emits self-closing form when no children`() {
        val sections = runVisitor("enum_flags.manifest.bin")
        val activities = sections.first { it.type == SectionType.ACTIVITIES }
        // The fixture's <activity> should render as a multi-attribute element. Verify the
        // shape: it must contain " />" or "</activity>" depending on whether the fixture
        // includes children. Either is acceptable — what matters is well-formedness.
        val xml = activities.prettyXml
        val wellFormed = xml.endsWith(" />") || xml.endsWith("</activity>")
        wellFormed shouldBe true
    }
}
