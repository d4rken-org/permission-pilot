package eu.darken.myperm.settings.ui.support.contact

import eu.darken.myperm.settings.ui.support.contact.ContactFormViewModel.Companion.countWords
import eu.darken.myperm.settings.ui.support.contact.ContactFormViewModel.Companion.meetsMinimum
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ContactFormValidationTest {

    @Test
    fun `countWords returns correct count for English text`() {
        countWords("the quick brown fox jumps over the lazy dog") shouldBe 9
    }

    @Test
    fun `countWords returns 0 for empty string`() {
        countWords("") shouldBe 0
    }

    @Test
    fun `countWords returns 0 for whitespace only`() {
        countWords("   \t  \n  ") shouldBe 0
    }

    @Test
    fun `countWords returns 1 for single word`() {
        countWords("hello") shouldBe 1
    }

    @Test
    fun `countWords handles multiple spaces between words`() {
        countWords("hello   world   foo") shouldBe 3
    }

    @Test
    fun `countWords trims leading and trailing whitespace`() {
        countWords("  hello world  ") shouldBe 2
    }

    @Test
    fun `meetsMinimum passes for sufficient English words`() {
        val text = (1..20).joinToString(" ") { "word$it" }
        meetsMinimum(text, 20) shouldBe true
    }

    @Test
    fun `meetsMinimum fails for insufficient English words`() {
        val text = (1..5).joinToString(" ") { "word$it" }
        meetsMinimum(text, 20) shouldBe false
    }

    @Test
    fun `meetsMinimum CJK fallback triggers for Chinese text`() {
        // 64 Chinese characters should pass minWords=20 because 64 >= 20*3=60
        val text = "\u8fd9\u662f\u4e00\u6bb5\u6d4b\u8bd5\u6587\u5b57".repeat(8) // 8 chars * 8 = 64 chars
        meetsMinimum(text, 20) shouldBe true
    }

    @Test
    fun `meetsMinimum CJK fallback fails for short CJK text`() {
        // 24 Chinese characters should fail minWords=20 because 24 < 60
        val text = "\u8fd9\u662f\u4e00\u6bb5\u6d4b\u8bd5\u6587\u5b57".repeat(3) // 8 * 3 = 24 chars
        meetsMinimum(text, 20) shouldBe false
    }

    @Test
    fun `meetsMinimum handles mixed English and CJK text`() {
        val text = "hello world foo bar baz \u8fd9\u662f\u4e00\u6bb5\u6d4b\u8bd5\u6587\u5b57"
        meetsMinimum(text, 20) shouldBe false
    }

    @Test
    fun `meetsMinimum passes at exact boundary`() {
        val text = (1..20).joinToString(" ") { "w" }
        meetsMinimum(text, 20) shouldBe true
    }

    @Test
    fun `meetsMinimum CJK passes at exact boundary`() {
        // Exactly 60 chars for minWords=20: 60 == 20*3
        val text = "a".repeat(60)
        meetsMinimum(text, 20) shouldBe true
    }

    @Test
    fun `canSend is false when description is too short`() {
        val state = ContactFormViewModel.State(
            category = ContactFormViewModel.Category.QUESTION,
            description = "too short",
        )
        state.canSend shouldBe false
    }

    @Test
    fun `canSend is true for question with enough words`() {
        val text = (1..20).joinToString(" ") { "word$it" }
        val state = ContactFormViewModel.State(
            category = ContactFormViewModel.Category.QUESTION,
            description = text,
        )
        state.canSend shouldBe true
    }

    @Test
    fun `canSend requires expected behavior for bugs`() {
        val desc = (1..20).joinToString(" ") { "word$it" }
        val state = ContactFormViewModel.State(
            category = ContactFormViewModel.Category.BUG,
            description = desc,
            expectedBehavior = "short",
        )
        state.canSend shouldBe false
    }

    @Test
    fun `canSend is true for bug with both fields filled`() {
        val desc = (1..20).joinToString(" ") { "word$it" }
        val expected = (1..10).joinToString(" ") { "exp$it" }
        val state = ContactFormViewModel.State(
            category = ContactFormViewModel.Category.BUG,
            description = desc,
            expectedBehavior = expected,
        )
        state.canSend shouldBe true
    }

    @Test
    fun `canSend is false while sending`() {
        val desc = (1..20).joinToString(" ") { "word$it" }
        val state = ContactFormViewModel.State(
            category = ContactFormViewModel.Category.QUESTION,
            description = desc,
            isSending = true,
        )
        state.canSend shouldBe false
    }

    @Test
    fun `canSend is false while recording`() {
        val desc = (1..20).joinToString(" ") { "word$it" }
        val state = ContactFormViewModel.State(
            category = ContactFormViewModel.Category.QUESTION,
            description = desc,
            isRecording = true,
        )
        state.canSend shouldBe false
    }
}
