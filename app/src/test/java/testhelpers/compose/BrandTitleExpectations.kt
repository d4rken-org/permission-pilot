package testhelpers.compose

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import eu.darken.myperm.R
import io.kotest.matchers.shouldBe

/**
 * Arrangement of the branded title belongs to the translated template, so tests must not spell it
 * as `"$name $qualifier"`: a locale that legitimately reorders or repunctuates would then fail CI
 * on intended behaviour. These helpers resolve the real template for the locale under test and
 * assert only the invariants — the qualifier appears exactly once, and the highlight covers
 * exactly it, wherever the template put it.
 */
fun Context.brandTitleFor(nameRes: Int): String = getString(
    R.string.app_name_upgraded_template,
    getString(nameRes),
    getString(R.string.upgrade_title_suffix),
)

/** The flavor's tier qualifier — "Pro" on gplay, "FOSS" on the FOSS build. */
val Context.brandQualifier: String
    get() = getString(R.string.upgrade_title_suffix)

/**
 * Asserts the styled run is exactly the qualifier: one span, the expected color, covering the
 * qualifier's only occurrence. Pins the span *boundary*, not just the concatenated text — the
 * failure worth guarding against renders the right characters with the highlight on the wrong word.
 */
fun AnnotatedString.shouldHighlightOnlyQualifier(qualifier: String, color: Color) {
    spanStyles.size shouldBe 1
    val span = spanStyles.single()
    span.item.color shouldBe color
    text.substring(span.start, span.end) shouldBe qualifier
    // Only one candidate position may exist, else the boundary check above proves nothing.
    text.indexOf(qualifier) shouldBe span.start
    text.lastIndexOf(qualifier) shouldBe span.start
}
