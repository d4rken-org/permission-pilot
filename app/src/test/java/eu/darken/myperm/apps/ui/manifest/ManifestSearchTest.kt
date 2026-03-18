package eu.darken.myperm.apps.ui.manifest

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class ManifestSearchTest : BaseTest() {

    @Test
    fun `empty query returns no matches`() {
        ManifestViewerViewModel.findMatches("<manifest/>", "") shouldBe emptyList()
    }

    @Test
    fun `single match found`() {
        val xml = "<uses-permission android:name=\"android.permission.INTERNET\"/>"
        val matches = ManifestViewerViewModel.findMatches(xml, "INTERNET")

        matches.size shouldBe 1
        matches[0].start shouldBe xml.indexOf("INTERNET")
        matches[0].endExclusive shouldBe xml.indexOf("INTERNET") + "INTERNET".length
    }

    @Test
    fun `multiple matches found`() {
        val xml = "<permission/><permission/><permission/>"
        val matches = ManifestViewerViewModel.findMatches(xml, "permission")

        matches.size shouldBe 3
    }

    @Test
    fun `search is case insensitive`() {
        val xml = "<Permission android:name=\"CAMERA\"/>"
        val matches = ManifestViewerViewModel.findMatches(xml, "permission")

        matches.size shouldBe 1
    }

    @Test
    fun `no match returns empty list`() {
        ManifestViewerViewModel.findMatches("<manifest/>", "nonexistent") shouldBe emptyList()
    }

    @Test
    fun `overlapping pattern positions handled`() {
        val xml = "aaa"
        val matches = ManifestViewerViewModel.findMatches(xml, "aa")

        matches.size shouldBe 2
        matches[0] shouldBe ManifestViewerViewModel.MatchRange(0, 2)
        matches[1] shouldBe ManifestViewerViewModel.MatchRange(1, 3)
    }

    @Test
    fun `special regex characters treated as literal`() {
        val xml = "value=[test]"
        val matches = ManifestViewerViewModel.findMatches(xml, "[test]")

        matches.size shouldBe 1
    }

    @Test
    fun `parentheses treated as literal`() {
        val xml = "name=(value)"
        val matches = ManifestViewerViewModel.findMatches(xml, "(value)")

        matches.size shouldBe 1
    }

    @Test
    fun `match ranges point to correct positions`() {
        val xml = "abc permission def permission ghi"
        val matches = ManifestViewerViewModel.findMatches(xml, "permission")

        matches.size shouldBe 2
        xml.substring(matches[0].start, matches[0].endExclusive) shouldBe "permission"
        xml.substring(matches[1].start, matches[1].endExclusive) shouldBe "permission"
    }
}
