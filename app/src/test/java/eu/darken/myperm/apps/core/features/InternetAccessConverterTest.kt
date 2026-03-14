package eu.darken.myperm.apps.core.features

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import testhelper.BaseTest

class InternetAccessConverterTest : BaseTest() {

    private val converter = InternetAccess.Converter()

    @Test
    fun `round-trip all values`() {
        InternetAccess.entries.forEach { value ->
            converter.toEnum(converter.fromEnum(value)) shouldBe value
        }
    }

    @Test
    fun `fromEnum produces expected strings`() {
        converter.fromEnum(InternetAccess.DIRECT) shouldBe "DIRECT"
        converter.fromEnum(InternetAccess.INDIRECT) shouldBe "INDIRECT"
        converter.fromEnum(InternetAccess.NONE) shouldBe "NONE"
        converter.fromEnum(InternetAccess.UNKNOWN) shouldBe "UNKNOWN"
    }

    @Test
    fun `toEnum rejects unknown value`() {
        assertThrows<IllegalArgumentException> { converter.toEnum("BOGUS") }
    }
}
