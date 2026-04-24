package eu.darken.myperm.apps.core.manifest

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class ManifestEnumFlagNamesTest : BaseTest() {

    @Test
    fun `protectionLevel signature and privileged formats as pipe-separated flags`() {
        ManifestEnumFlagNames.format(0x01010009, 0x12) shouldBe "signature|privileged"
    }

    @Test
    fun `protectionLevel zero falls back to normal default`() {
        ManifestEnumFlagNames.format(0x01010009, 0) shouldBe "normal"
    }

    @Test
    fun `launchMode enum maps discrete values`() {
        ManifestEnumFlagNames.format(0x0101001d, 0) shouldBe "standard"
        ManifestEnumFlagNames.format(0x0101001d, 1) shouldBe "singleTop"
        ManifestEnumFlagNames.format(0x0101001d, 2) shouldBe "singleTask"
        ManifestEnumFlagNames.format(0x0101001d, 3) shouldBe "singleInstance"
    }

    @Test
    fun `screenOrientation handles -1 unspecified`() {
        ManifestEnumFlagNames.format(0x0101001e, -1) shouldBe "unspecified"
    }

    @Test
    fun `configChanges emits pipe-separated flags`() {
        // mcc (1) | mnc (2) | locale (4) = 0x7
        ManifestEnumFlagNames.format(0x0101001f, 0x7) shouldBe "mcc|mnc|locale"
    }

    @Test
    fun `unknown attribute id returns null`() {
        ManifestEnumFlagNames.format(0x01990099, 42) shouldBe null
    }

    @Test
    fun `unknown enum data returns null`() {
        // launchMode 99 — not a defined enum value
        ManifestEnumFlagNames.format(0x0101001d, 99) shouldBe null
    }

    @Test
    fun `leftover bits get hex fallback`() {
        // protectionLevel: signature (0x2) | unknown bit 0x80000 → "signature|0x80000"
        val result = ManifestEnumFlagNames.format(0x01010009, 0x80002)
        result shouldBe "signature|0x80000"
    }
}
