package eu.darken.myperm.common.upgrade.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest

/**
 * The title template lets translators own word order and punctuation, so the styled qualifier has
 * to land on the right offsets wherever they put it. Assertions are on span boundaries, not just on
 * the concatenated text: the failure this guards against renders the correct characters with the
 * highlight sitting on the wrong word.
 */
class TitleTemplateSpliceTest : BaseTest() {

    private val qualifierColor = Color.Red

    private val name = AnnotatedString("Permission Pilot")

    private val qualifier: AnnotatedString = buildAnnotatedString {
        pushStyle(SpanStyle(color = qualifierColor))
        append("Pro")
        pop()
    }

    private fun template(pattern: String) = pattern
        .replace("%1\$s", BRAND_TITLE_MARKER)
        .replace("%2\$s", BRAND_QUALIFIER_MARKER)

    @Test
    fun `the default order highlights the trailing qualifier`() {
        val result = spliceTitleTemplate(template("%1\$s %2\$s"), name, qualifier)

        result.text shouldBe "Permission Pilot Pro"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().item.color shouldBe qualifierColor
        result.spanStyles.single().start shouldBe 17
        result.spanStyles.single().end shouldBe 20
        result.text.substring(17, 20) shouldBe "Pro"
    }

    @Test
    fun `a reordered template highlights the leading qualifier`() {
        val result = spliceTitleTemplate(template("%2\$s %1\$s"), name, qualifier)

        result.text shouldBe "Pro Permission Pilot"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().start shouldBe 0
        result.spanStyles.single().end shouldBe 3
        result.text.substring(0, 3) shouldBe "Pro"
    }

    @Test
    fun `a custom separator shifts the qualifier without entering the span`() {
        val result = spliceTitleTemplate(template("%1\$s – %2\$s"), name, qualifier)

        result.text shouldBe "Permission Pilot – Pro"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().start shouldBe 19
        result.spanStyles.single().end shouldBe 22
        result.text.substring(19, 22) shouldBe "Pro"
    }

    @Test
    fun `a multi-word qualifier is highlighted whole`() {
        val multiWord = buildAnnotatedString {
            pushStyle(SpanStyle(color = qualifierColor))
            append("النسخة الاحترافية")
            pop()
        }

        val result = spliceTitleTemplate(template("%1\$s – %2\$s"), AnnotatedString("بايلوت الأذونات"), multiWord)

        result.text shouldBe "بايلوت الأذونات – النسخة الاحترافية"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().start shouldBe 18
        result.spanStyles.single().end shouldBe 35
        result.text.substring(18, 35) shouldBe "النسخة الاحترافية"
    }

    // Span offsets are UTF-16 indices, so a supplementary character ahead of a slot shifts it by
    // two. Pins that the splice arithmetic counts code units and not code points.
    @Test
    fun `a supplementary character before the slots shifts the span by two`() {
        val result = spliceTitleTemplate(template("🛡 %1\$s %2\$s"), name, qualifier)

        result.text shouldBe "🛡 Permission Pilot Pro"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().start shouldBe 20
        result.spanStyles.single().end shouldBe 23
        result.text.substring(20, 23) shouldBe "Pro"
    }

    @Test
    fun `a duplicated name marker falls back to the complete default title`() {
        val result = spliceTitleTemplate(
            "$BRAND_TITLE_MARKER $BRAND_TITLE_MARKER $BRAND_QUALIFIER_MARKER",
            name,
            qualifier,
        )

        result.text shouldBe "Permission Pilot Pro"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().start shouldBe 17
        result.spanStyles.single().end shouldBe 20
    }

    @Test
    fun `a duplicated qualifier marker falls back to the complete default title`() {
        val result = spliceTitleTemplate(
            "$BRAND_TITLE_MARKER $BRAND_QUALIFIER_MARKER $BRAND_QUALIFIER_MARKER",
            name,
            qualifier,
        )

        result.text shouldBe "Permission Pilot Pro"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().start shouldBe 17
        result.spanStyles.single().end shouldBe 20
    }

    @Test
    fun `a missing name marker falls back rather than rendering the qualifier alone`() {
        val result = spliceTitleTemplate("Get $BRAND_QUALIFIER_MARKER", name, qualifier)

        result.text shouldBe "Permission Pilot Pro"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().start shouldBe 17
        result.spanStyles.single().end shouldBe 20
    }

    @Test
    fun `a missing qualifier marker falls back rather than dropping the tier`() {
        val result = spliceTitleTemplate("Get $BRAND_TITLE_MARKER", name, qualifier)

        result.text shouldBe "Permission Pilot Pro"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().start shouldBe 17
        result.spanStyles.single().end shouldBe 20
    }

    @Test
    fun `a template with neither marker falls back to the complete default title`() {
        val result = spliceTitleTemplate("Permission Pilot Pro", name, qualifier)

        result.text shouldBe "Permission Pilot Pro"
        result.spanStyles.size shouldBe 1
        result.spanStyles.single().item.color shouldBe qualifierColor
        result.spanStyles.single().start shouldBe 17
        result.spanStyles.single().end shouldBe 20
    }
}
