package eu.darken.myperm.apps.core.manifest

import android.R as AndroidR
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class ManifestEnumFlagNamesTest : BaseTest() {

    // --- protectionLevel (enum + flags) ---

    @Test
    fun `protectionLevel base enum renders discrete values`() {
        ManifestEnumFlagNames.format(AndroidR.attr.protectionLevel, 0) shouldBe "normal"
        ManifestEnumFlagNames.format(AndroidR.attr.protectionLevel, 1) shouldBe "dangerous"
        ManifestEnumFlagNames.format(AndroidR.attr.protectionLevel, 2) shouldBe "signature"
        ManifestEnumFlagNames.format(AndroidR.attr.protectionLevel, 3) shouldBe "signatureOrSystem"
        ManifestEnumFlagNames.format(AndroidR.attr.protectionLevel, 4) shouldBe "internal"
    }

    @Test
    fun `protectionLevel signature plus privileged formats as pipe-separated`() {
        ManifestEnumFlagNames.format(AndroidR.attr.protectionLevel, 0x12) shouldBe "signature|privileged"
    }

    @Test
    fun `protectionLevel signatureOrSystem plus flag renders correctly`() {
        // data = 3 (signatureOrSystem) | 0x10 (privileged) = 0x13
        ManifestEnumFlagNames.format(AndroidR.attr.protectionLevel, 0x13) shouldBe "signatureOrSystem|privileged"
    }

    @Test
    fun `protectionLevel unknown flag bit surfaces as hex`() {
        // signature (2) + unknown bit 0x4000000 -> "signature|0x4000000"
        // (must pick a bit not present in the known flag list — the table covers up through 0x2000000)
        ManifestEnumFlagNames.format(AndroidR.attr.protectionLevel, 0x4000002) shouldBe "signature|0x4000000"
    }

    // --- launchMode (pure enum) ---

    @Test
    fun `launchMode enum maps discrete values`() {
        ManifestEnumFlagNames.format(AndroidR.attr.launchMode, 0) shouldBe "standard"
        ManifestEnumFlagNames.format(AndroidR.attr.launchMode, 1) shouldBe "singleTop"
        ManifestEnumFlagNames.format(AndroidR.attr.launchMode, 2) shouldBe "singleTask"
        ManifestEnumFlagNames.format(AndroidR.attr.launchMode, 3) shouldBe "singleInstance"
    }

    @Test
    fun `launchMode unknown value returns null`() {
        ManifestEnumFlagNames.format(AndroidR.attr.launchMode, 99) shouldBe null
    }

    // --- screenOrientation ---

    @Test
    fun `screenOrientation handles -1 unspecified`() {
        ManifestEnumFlagNames.format(AndroidR.attr.screenOrientation, -1) shouldBe "unspecified"
    }

    @Test
    fun `screenOrientation portrait and landscape`() {
        ManifestEnumFlagNames.format(AndroidR.attr.screenOrientation, 0) shouldBe "landscape"
        ManifestEnumFlagNames.format(AndroidR.attr.screenOrientation, 1) shouldBe "portrait"
    }

    // --- configChanges (pure flag) ---

    @Test
    fun `configChanges emits pipe-separated flags`() {
        ManifestEnumFlagNames.format(AndroidR.attr.configChanges, 0x7) shouldBe "mcc|mnc|locale"
    }

    @Test
    fun `configChanges uses high-bit flags like fontScale`() {
        ManifestEnumFlagNames.format(AndroidR.attr.configChanges, 0x20000000) shouldBe "fontScale"
    }

    // --- windowSoftInputMode (two nibble enums) ---

    @Test
    fun `windowSoftInputMode combined state and adjust`() {
        // stateHidden (0x2) | adjustResize (0x10) = 0x12
        ManifestEnumFlagNames.format(AndroidR.attr.windowSoftInputMode, 0x12) shouldBe "stateHidden|adjustResize"
    }

    @Test
    fun `windowSoftInputMode stateAlwaysHidden decodes as enum not as flag combo`() {
        // Regression guard: previously rendered as "stateUnchanged|stateHidden" under a pure-flag decoder.
        ManifestEnumFlagNames.format(AndroidR.attr.windowSoftInputMode, 0x3) shouldBe "stateAlwaysHidden|adjustUnspecified"
    }

    @Test
    fun `windowSoftInputMode unspecified state yields adjust only`() {
        ManifestEnumFlagNames.format(AndroidR.attr.windowSoftInputMode, 0x20) shouldBe "stateUnspecified|adjustPan"
    }

    // --- persistableMode ---

    @Test
    fun `persistableMode enum`() {
        ManifestEnumFlagNames.format(AndroidR.attr.persistableMode, 0) shouldBe "persistRootOnly"
        ManifestEnumFlagNames.format(AndroidR.attr.persistableMode, 1) shouldBe "persistAcrossReboots"
        ManifestEnumFlagNames.format(AndroidR.attr.persistableMode, 2) shouldBe "persistNever"
    }

    // --- documentLaunchMode ---

    @Test
    fun `documentLaunchMode enum`() {
        ManifestEnumFlagNames.format(AndroidR.attr.documentLaunchMode, 0) shouldBe "none"
        ManifestEnumFlagNames.format(AndroidR.attr.documentLaunchMode, 2) shouldBe "always"
    }

    // --- foregroundServiceType ---

    @Test
    fun `foregroundServiceType common combinations`() {
        ManifestEnumFlagNames.format(AndroidR.attr.foregroundServiceType, 0x40) shouldBe "camera"
        // camera | microphone = 0x40 | 0x80 = 0xC0
        ManifestEnumFlagNames.format(AndroidR.attr.foregroundServiceType, 0xC0) shouldBe "camera|microphone"
    }

    // --- importantForAccessibility ---

    @Test
    fun `importantForAccessibility enum`() {
        ManifestEnumFlagNames.format(AndroidR.attr.importantForAccessibility, 0) shouldBe "auto"
        ManifestEnumFlagNames.format(AndroidR.attr.importantForAccessibility, 1) shouldBe "yes"
        ManifestEnumFlagNames.format(AndroidR.attr.importantForAccessibility, 2) shouldBe "no"
        ManifestEnumFlagNames.format(AndroidR.attr.importantForAccessibility, 4) shouldBe "noHideDescendants"
    }

    // --- importantForAutofill ---

    @Test
    fun `importantForAutofill default and single bit`() {
        ManifestEnumFlagNames.format(AndroidR.attr.importantForAutofill, 0) shouldBe "auto"
        ManifestEnumFlagNames.format(AndroidR.attr.importantForAutofill, 0x4) shouldBe "yes"
    }

    // --- gwpAsanMode ---

    @Test
    fun `gwpAsanMode enum`() {
        ManifestEnumFlagNames.format(AndroidR.attr.gwpAsanMode, -1) shouldBe "default"
        ManifestEnumFlagNames.format(AndroidR.attr.gwpAsanMode, 0) shouldBe "never"
        ManifestEnumFlagNames.format(AndroidR.attr.gwpAsanMode, 1) shouldBe "always"
    }

    // --- usesPermissionFlags ---

    @Test
    fun `usesPermissionFlags neverForLocation uses 0x10000 bit`() {
        // Regression guard: was previously mapped as 0x1 (wrong).
        ManifestEnumFlagNames.format(AndroidR.attr.usesPermissionFlags, 0x00010000) shouldBe "neverForLocation"
    }

    // --- unknown attr / table coverage ---

    @Test
    fun `unknown attribute id returns null`() {
        ManifestEnumFlagNames.format(0x01990099, 42) shouldBe null
    }

    /**
     * Structural test: every attribute listed in the table must be resolvable via
     * `android.R.attr.<name>` when decoded with a plausible data value. This guards
     * against typo'd or stale numeric literals — the table now references `android.R.attr`
     * directly, so this test also ensures we don't silently regress to a hand-typed ID.
     */
    @Test
    fun `every table key corresponds to a framework android R attr`() {
        // Non-exhaustive but covers every entry in the production table via its expected canonical id.
        val expectedIds = listOf(
            AndroidR.attr.protectionLevel,
            AndroidR.attr.launchMode,
            AndroidR.attr.screenOrientation,
            AndroidR.attr.configChanges,
            AndroidR.attr.windowSoftInputMode,
            AndroidR.attr.persistableMode,
            AndroidR.attr.documentLaunchMode,
            AndroidR.attr.foregroundServiceType,
            AndroidR.attr.importantForAccessibility,
            AndroidR.attr.importantForAutofill,
            AndroidR.attr.gwpAsanMode,
            AndroidR.attr.usesPermissionFlags,
        )
        // Picking a plausible data value per id — all chosen to land on a defined enum/flag.
        val samples = mapOf(
            AndroidR.attr.protectionLevel to 2,          // signature
            AndroidR.attr.launchMode to 0,               // standard
            AndroidR.attr.screenOrientation to 1,        // portrait
            AndroidR.attr.configChanges to 0x1,          // mcc
            AndroidR.attr.windowSoftInputMode to 0x12,   // stateHidden|adjustResize
            AndroidR.attr.persistableMode to 0,          // persistRootOnly
            AndroidR.attr.documentLaunchMode to 0,       // none
            AndroidR.attr.foregroundServiceType to 0x40, // camera
            AndroidR.attr.importantForAccessibility to 0, // auto
            AndroidR.attr.importantForAutofill to 0,     // auto
            AndroidR.attr.gwpAsanMode to 0,              // never
            AndroidR.attr.usesPermissionFlags to 0x10000, // neverForLocation
        )
        for (id in expectedIds) {
            val data = samples.getValue(id)
            ManifestEnumFlagNames.format(id, data).shouldNotBeNull()
        }
    }
}
