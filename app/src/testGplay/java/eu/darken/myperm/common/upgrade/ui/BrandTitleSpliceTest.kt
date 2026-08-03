package eu.darken.myperm.common.upgrade.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest

/**
 * The brand is spliced into the already-formatted translation, so the styled suffix has to land on
 * the right offsets no matter where the pattern put the placeholder.
 */
class BrandTitleSpliceTest : BaseTest() {

    private val brandColor = Color.Red

    // "Permission Pilot Pro" with the suffix (17..20) colored, like upgradeScreenTitle(upgraded = true).
    private val brand: AnnotatedString = buildAnnotatedString {
        append("Permission Pilot ")
        pushStyle(SpanStyle(color = brandColor))
        append("Pro")
        pop()
    }

    @Test fun `marker in the middle shifts the styled suffix by the prefix`() {
        val result = spliceBrandTitle("Get $BRAND_TITLE_MARKER", brand)

        result.text shouldBe "Get Permission Pilot Pro"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().item.color shouldBe brandColor
        result.spanStyles.single().start shouldBe 21
        result.spanStyles.single().end shouldBe 24
        result.text.substring(21, 24) shouldBe "Pro"
    }

    @Test fun `marker at the start keeps the suffix offsets inside the brand`() {
        val result = spliceBrandTitle("$BRAND_TITLE_MARKER holen", brand)

        result.text shouldBe "Permission Pilot Pro holen"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().start shouldBe 17
        result.spanStyles.single().end shouldBe 20
        result.text.substring(17, 20) shouldBe "Pro"
    }

    @Test fun `a duplicated marker renders the brand twice`() {
        val result = spliceBrandTitle("$BRAND_TITLE_MARKER und $BRAND_TITLE_MARKER", brand)

        result.text shouldBe "Permission Pilot Pro und Permission Pilot Pro"
        result.spanStyles.size shouldBe 2
        result.spanStyles[0].start shouldBe 17
        result.spanStyles[0].end shouldBe 20
        result.spanStyles[1].start shouldBe 42
        result.spanStyles[1].end shouldBe 45
        result.text.substring(42, 45) shouldBe "Pro"
    }

    @Test fun `a translation that lost the placeholder still shows the brand`() {
        val result = spliceBrandTitle("Get Pro", brand)

        result.text shouldBe "Get Pro Permission Pilot Pro"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().item.color shouldBe brandColor
        result.spanStyles.single().start shouldBe 25
        result.spanStyles.single().end shouldBe 28
    }
}
