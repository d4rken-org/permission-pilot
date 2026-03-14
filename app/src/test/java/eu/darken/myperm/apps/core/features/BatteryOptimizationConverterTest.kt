package eu.darken.myperm.apps.core.features

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import testhelper.BaseTest

class BatteryOptimizationConverterTest : BaseTest() {

    private val converter = BatteryOptimization.Converter()

    @Test
    fun `round-trip all values`() {
        BatteryOptimization.entries.forEach { value ->
            converter.toEnum(converter.fromEnum(value)) shouldBe value
        }
    }

    @Test
    fun `fromEnum produces expected strings`() {
        converter.fromEnum(BatteryOptimization.IGNORED) shouldBe "IGNORED"
        converter.fromEnum(BatteryOptimization.OPTIMIZED) shouldBe "OPTIMIZED"
        converter.fromEnum(BatteryOptimization.MANAGED_BY_SYSTEM) shouldBe "MANAGED_BY_SYSTEM"
        converter.fromEnum(BatteryOptimization.UNKNOWN) shouldBe "UNKNOWN"
    }

    @Test
    fun `toEnum rejects unknown value`() {
        assertThrows<IllegalArgumentException> { converter.toEnum("BOGUS") }
    }
}
