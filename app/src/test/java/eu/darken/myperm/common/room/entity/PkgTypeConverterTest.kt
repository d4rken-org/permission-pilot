package eu.darken.myperm.common.room.entity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import testhelper.BaseTest

class PkgTypeConverterTest : BaseTest() {

    private val converter = PkgType.Converter()

    @Test
    fun `round-trip all values`() {
        PkgType.entries.forEach { value ->
            converter.toEnum(converter.fromEnum(value)) shouldBe value
        }
    }

    @Test
    fun `fromEnum produces expected strings`() {
        converter.fromEnum(PkgType.PRIMARY) shouldBe "PRIMARY"
        converter.fromEnum(PkgType.SECONDARY_PROFILE) shouldBe "SECONDARY_PROFILE"
        converter.fromEnum(PkgType.SECONDARY_USER) shouldBe "SECONDARY_USER"
        converter.fromEnum(PkgType.UNINSTALLED) shouldBe "UNINSTALLED"
    }

    @Test
    fun `toEnum rejects unknown value`() {
        assertThrows<IllegalArgumentException> { converter.toEnum("BOGUS") }
    }
}
