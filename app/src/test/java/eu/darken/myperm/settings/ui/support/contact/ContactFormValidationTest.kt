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
        // 60 Chinese characters should pass minWords=20 because 60 >= 20*3
        val text = "\u8fd9\u662f\u4e00\u6bb5\u6d4b\u8bd5\u6587\u5b57".repeat(8) // 8 chars * 8 = 64 chars
        meetsMinimum(text, 20) shouldBe true
    }

    @Test
    fun `meetsMinimum CJK fallback fails for short CJK text`() {
        // 30 Chinese characters should fail minWords=20 because 30 < 60
        val text = "\u8fd9\u662f\u4e00\u6bb5\u6d4b\u8bd5\u6587\u5b57".repeat(3) // 8 * 3 = 24 chars
        // 24 < 20*3=60, so should fail
        meetsMinimum(text, 20) shouldBe false
    }

    @Test
    fun `meetsMinimum handles mixed English and CJK text`() {
        // 5 English words + some CJK: word count is 5, char count around 30+
        val text = "hello world foo bar baz \u8fd9\u662f\u4e00\u6bb5\u6d4b\u8bd5\u6587\u5b57"
        // countWords would be 6 (5 English + 1 CJK block counted as one "word")
        // 6 < 20, so word check fails.
        // char count is ~32, 32 < 60, so CJK fallback also fails
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
        // This is 1 word of 60 chars. Word check: 1 < 20 fails.
        // CJK fallback: 60 >= 60, passes.
        meetsMinimum(text, 20) shouldBe true
    }
}
