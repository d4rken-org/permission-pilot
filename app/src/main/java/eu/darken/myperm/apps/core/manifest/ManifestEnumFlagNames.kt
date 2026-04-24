package eu.darken.myperm.apps.core.manifest

import android.R as AndroidR

/**
 * Curated decoder for high-frequency enum/flag-typed manifest attributes.
 *
 * The `apk-parser` library we're replacing resolved these via the framework's `<attr>` metadata.
 * We don't load that metadata, so we ship a small hand-maintained table covering the attrs that
 * users actually read in the manifest viewer. Attrs outside the table render as plain numbers.
 *
 * Framework resource IDs are pulled from `android.R.attr.*` — compile-time constants sourced from
 * the platform, eliminating the risk of hand-typed numeric errors. Each entry is covered by a
 * unit test (`ManifestEnumFlagNamesTest`).
 */
internal object ManifestEnumFlagNames {

    private val table: Map<Int, Decoder> = mapOf(
        // Base enum (0..4) + privilege/grant flag bits above 0x0F. Flags may OR with any base.
        AndroidR.attr.protectionLevel to Decoder.EnumWithFlags(
            baseMask = 0x0F,
            baseEnum = mapOf(
                0 to "normal",
                1 to "dangerous",
                2 to "signature",
                3 to "signatureOrSystem",
                4 to "internal",
            ),
            flags = listOf(
                FlagBit(0x10, "privileged"),
                FlagBit(0x20, "development"),
                FlagBit(0x40, "appop"),
                FlagBit(0x80, "pre23"),
                FlagBit(0x100, "installer"),
                FlagBit(0x200, "verifier"),
                FlagBit(0x400, "preinstalled"),
                FlagBit(0x800, "setup"),
                FlagBit(0x1000, "instant"),
                FlagBit(0x2000, "runtime"),
                FlagBit(0x4000, "oem"),
                FlagBit(0x8000, "vendorPrivileged"),
                FlagBit(0x10000, "textClassifier"),
                FlagBit(0x20000, "configurator"),
                FlagBit(0x40000, "incidentReportApprover"),
                FlagBit(0x80000, "appPredictor"),
                FlagBit(0x100000, "moduleInstaller"),
                FlagBit(0x200000, "companion"),
                FlagBit(0x400000, "retailDemo"),
                FlagBit(0x800000, "recents"),
                FlagBit(0x1000000, "role"),
                FlagBit(0x2000000, "knownSigner"),
            ),
        ),
        AndroidR.attr.launchMode to Decoder.Enum(
            0 to "standard",
            1 to "singleTop",
            2 to "singleTask",
            3 to "singleInstance",
            4 to "singleInstancePerTask",
        ),
        AndroidR.attr.screenOrientation to Decoder.Enum(
            -1 to "unspecified",
            0 to "landscape",
            1 to "portrait",
            2 to "user",
            3 to "behind",
            4 to "sensor",
            5 to "nosensor",
            6 to "sensorLandscape",
            7 to "sensorPortrait",
            8 to "reverseLandscape",
            9 to "reversePortrait",
            10 to "fullSensor",
            11 to "userLandscape",
            12 to "userPortrait",
            13 to "fullUser",
            14 to "locked",
        ),
        AndroidR.attr.configChanges to Decoder.Flag(
            FlagBit(0x0001, "mcc"),
            FlagBit(0x0002, "mnc"),
            FlagBit(0x0004, "locale"),
            FlagBit(0x0008, "touchscreen"),
            FlagBit(0x0010, "keyboard"),
            FlagBit(0x0020, "keyboardHidden"),
            FlagBit(0x0040, "navigation"),
            FlagBit(0x0080, "orientation"),
            FlagBit(0x0100, "screenLayout"),
            FlagBit(0x0200, "uiMode"),
            FlagBit(0x0400, "screenSize"),
            FlagBit(0x0800, "smallestScreenSize"),
            FlagBit(0x1000, "density"),
            FlagBit(0x2000, "layoutDirection"),
            FlagBit(0x4000, "colorMode"),
            FlagBit(0x20000000, "fontScale"),
            FlagBit(0x40000000.toInt(), "fontWeightAdjustment"),
            FlagBit(0x80000000.toInt(), "grammaticalGender"),
        ),
        // State nibble (low 4 bits, enum 0..5) + adjust nibble (bits 4..7, enum 0x00/0x10/0x20/0x30).
        AndroidR.attr.windowSoftInputMode to Decoder.TwoNibbleEnum(
            lowMask = 0x0F,
            lowEnum = mapOf(
                0 to "stateUnspecified",
                1 to "stateUnchanged",
                2 to "stateHidden",
                3 to "stateAlwaysHidden",
                4 to "stateVisible",
                5 to "stateAlwaysVisible",
            ),
            highMask = 0xF0,
            highEnum = mapOf(
                0x00 to "adjustUnspecified",
                0x10 to "adjustResize",
                0x20 to "adjustPan",
                0x30 to "adjustNothing",
            ),
        ),
        AndroidR.attr.persistableMode to Decoder.Enum(
            0 to "persistRootOnly",
            1 to "persistAcrossReboots",
            2 to "persistNever",
        ),
        AndroidR.attr.documentLaunchMode to Decoder.Enum(
            0 to "none",
            1 to "intoExisting",
            2 to "always",
            3 to "never",
        ),
        AndroidR.attr.foregroundServiceType to Decoder.Flag(
            FlagBit(0x00000001, "dataSync"),
            FlagBit(0x00000002, "mediaPlayback"),
            FlagBit(0x00000004, "phoneCall"),
            FlagBit(0x00000008, "location"),
            FlagBit(0x00000010, "connectedDevice"),
            FlagBit(0x00000020, "mediaProjection"),
            FlagBit(0x00000040, "camera"),
            FlagBit(0x00000080, "microphone"),
            FlagBit(0x00000100, "health"),
            FlagBit(0x00000200, "remoteMessaging"),
            FlagBit(0x00000400, "systemExempted"),
            FlagBit(0x00000800, "shortService"),
            FlagBit(0x00001000, "fileManagement"),
            FlagBit(0x00002000, "mediaProcessing"),
            FlagBit(0x00004000, "specialUse"),
        ),
        AndroidR.attr.importantForAccessibility to Decoder.Enum(
            0 to "auto",
            1 to "yes",
            2 to "no",
            4 to "noHideDescendants",
        ),
        AndroidR.attr.importantForAutofill to Decoder.Flag(
            FlagBit(0x1, "no"),
            FlagBit(0x2, "noExcludeDescendants"),
            FlagBit(0x4, "yes"),
            FlagBit(0x8, "yesExcludeDescendants"),
            defaultWhenZero = "auto",
        ),
        AndroidR.attr.gwpAsanMode to Decoder.Enum(
            -1 to "default",
            0 to "never",
            1 to "always",
        ),
        AndroidR.attr.usesPermissionFlags to Decoder.Flag(
            FlagBit(0x00010000, "neverForLocation"),
        ),
    )

    fun format(attributeResourceId: Int, data: Int): String? =
        table[attributeResourceId]?.format(data)

    internal data class FlagBit(val mask: Int, val name: String)

    internal sealed class Decoder {
        abstract fun format(data: Int): String?

        class Enum(vararg pairs: Pair<Int, String>) : Decoder() {
            private val mapping: Map<Int, String> = pairs.toMap()
            override fun format(data: Int): String? = mapping[data]
        }

        class Flag(
            vararg val bits: FlagBit,
            private val defaultWhenZero: String? = null,
        ) : Decoder() {
            override fun format(data: Int): String? {
                if (data == 0) return defaultWhenZero
                val out = StringBuilder()
                var remaining = data
                for (bit in bits) {
                    if ((remaining and bit.mask) == bit.mask) {
                        if (out.isNotEmpty()) out.append('|')
                        out.append(bit.name)
                        remaining = remaining and bit.mask.inv()
                    }
                }
                if (remaining != 0) {
                    if (out.isNotEmpty()) out.append('|')
                    out.append("0x").append(Integer.toHexString(remaining))
                }
                return if (out.isEmpty()) null else out.toString()
            }
        }

        /**
         * Enum in the low nibble (lower bits) combined with flag-like privilege bits in the upper
         * bits. Used by `protectionLevel`, where the low nibble is a discrete protection level
         * (normal/dangerous/signature/...) and the upper bits are optional grant flags.
         */
        class EnumWithFlags(
            private val baseMask: Int,
            private val baseEnum: Map<Int, String>,
            private val flags: List<FlagBit>,
        ) : Decoder() {
            override fun format(data: Int): String? {
                val baseValue = data and baseMask
                val baseName = baseEnum[baseValue] ?: return null
                val rest = data and baseMask.inv()
                if (rest == 0) return baseName
                val out = StringBuilder(baseName)
                var remaining = rest
                for (bit in flags) {
                    if ((remaining and bit.mask) == bit.mask) {
                        out.append('|').append(bit.name)
                        remaining = remaining and bit.mask.inv()
                    }
                }
                if (remaining != 0) {
                    out.append("|0x").append(Integer.toHexString(remaining))
                }
                return out.toString()
            }
        }

        /**
         * Two independent enum fields packed into low and high nibbles. Used by
         * `windowSoftInputMode`: `state` in bits 0..3 (enum 0..5), `adjust` in bits 4..7
         * (enum 0x00/0x10/0x20/0x30). A `0x00` high nibble still decodes (unspecified).
         */
        class TwoNibbleEnum(
            private val lowMask: Int,
            private val lowEnum: Map<Int, String>,
            private val highMask: Int,
            private val highEnum: Map<Int, String>,
        ) : Decoder() {
            override fun format(data: Int): String? {
                val low = data and lowMask
                val high = data and highMask
                val leftover = data and (lowMask or highMask).inv()
                val lowName = lowEnum[low]
                val highName = highEnum[high]
                if (lowName == null && highName == null && leftover == 0) return null
                val parts = mutableListOf<String>()
                lowName?.let { parts.add(it) }
                highName?.let { parts.add(it) }
                if (leftover != 0) parts.add("0x${Integer.toHexString(leftover)}")
                return parts.joinToString("|")
            }
        }
    }
}
