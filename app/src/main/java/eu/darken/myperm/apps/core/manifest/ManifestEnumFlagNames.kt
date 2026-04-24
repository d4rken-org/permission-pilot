package eu.darken.myperm.apps.core.manifest

/**
 * Curated decoder for high-frequency enum/flag-typed manifest attributes.
 *
 * The `apk-parser` library we're replacing resolved these via the framework's `<attr>` metadata.
 * We don't load that metadata, so we ship a small hand-maintained table covering the attrs that
 * users actually read in the manifest viewer. Attrs outside the table render as plain numbers.
 *
 * Framework resource IDs are stable public contract — declared on `android.R.attr` and safe to
 * reference numerically. The values here are verified against the platform sources and every
 * entry is covered by unit tests.
 */
internal object ManifestEnumFlagNames {

    private val table: Map<Int, Decoder> = mapOf(
        // android.R.attr.protectionLevel (0x01010009)
        0x01010009 to Decoder.Flag(
            FlagBit(0x1, "dangerous"),
            FlagBit(0x2, "signature"),
            FlagBit(0x4, "signatureOrSystem"),
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
            defaultWhenZero = "normal",
        ),
        // android.R.attr.launchMode (0x0101001d)
        0x0101001d to Decoder.Enum(
            0 to "standard",
            1 to "singleTop",
            2 to "singleTask",
            3 to "singleInstance",
            4 to "singleInstancePerTask",
        ),
        // android.R.attr.screenOrientation (0x0101001e)
        0x0101001e to Decoder.Enum(
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
        // android.R.attr.configChanges (0x0101001f)
        0x0101001f to Decoder.Flag(
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
        // android.R.attr.windowSoftInputMode (0x0101022b)
        0x0101022b to Decoder.Flag(
            FlagBit(0x1, "stateUnchanged"),
            FlagBit(0x2, "stateHidden"),
            FlagBit(0x3, "stateAlwaysHidden"),
            FlagBit(0x4, "stateVisible"),
            FlagBit(0x5, "stateAlwaysVisible"),
            FlagBit(0x10, "adjustResize"),
            FlagBit(0x20, "adjustPan"),
            FlagBit(0x30, "adjustNothing"),
            FlagBit(0x100, "adjustUnspecified"),
            defaultWhenZero = "stateUnspecified",
        ),
        // android.R.attr.persistableMode (0x010103c9)
        0x010103c9 to Decoder.Enum(
            0 to "persistRootOnly",
            1 to "persistAcrossReboots",
            2 to "persistNever",
        ),
        // android.R.attr.documentLaunchMode (0x01010445)
        0x01010445 to Decoder.Enum(
            0 to "none",
            1 to "intoExisting",
            2 to "always",
            3 to "never",
        ),
        // android.R.attr.foregroundServiceType (0x01010586)
        0x01010586 to Decoder.Flag(
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
        // android.R.attr.importantForAccessibility (0x010103aa)
        0x010103aa to Decoder.Enum(
            0 to "auto",
            1 to "yes",
            2 to "no",
            4 to "noHideDescendants",
        ),
        // android.R.attr.importantForAutofill (0x01010524)
        0x01010524 to Decoder.Flag(
            FlagBit(0x1, "no"),
            FlagBit(0x2, "noExcludeDescendants"),
            FlagBit(0x4, "yes"),
            FlagBit(0x8, "yesExcludeDescendants"),
            defaultWhenZero = "auto",
        ),
        // android.R.attr.gwpAsanMode (0x0101063d)
        0x0101063d to Decoder.Enum(
            -1 to "default",
            0 to "never",
            1 to "always",
        ),
        // android.R.attr.networkSecurityConfig is a reference type — no enum.
        // android.R.attr.usesPermissionFlags (0x01010643)
        0x01010643 to Decoder.Flag(
            FlagBit(0x1, "neverForLocation"),
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
    }
}
