package eu.darken.myperm.apps.core.manifest

import eu.darken.myperm.apps.core.manifest.binaryxml.BinaryXmlAttribute
import eu.darken.myperm.apps.core.manifest.binaryxml.TypedValue

/**
 * Resolves a resource id to its symbolic name (e.g. `@android:style/Theme.Material`).
 * Returned without an `?` prefix even for attribute references — the caller decides whether
 * to emit `@…` (resource ref) or `?…` (attribute ref).
 */
fun interface ResourceRefResolver {
    fun resolve(resourceId: Int): String?
}

/**
 * Pure formatting helpers shared by visitors that emit human-readable XML — currently the
 * full-document [ManifestTextRenderer] and the per-section [ManifestSectionVisitor].
 *
 * Stateless: no buffer, no element/depth bookkeeping. Each function takes everything it
 * needs as parameters so callers can format an attribute or value in any context.
 */
internal object BinaryXmlFormatting {

    const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    const val ANDROID_PREFIX = "android"

    fun qualifiedName(namespace: String?, prefix: String?, name: String): String = when {
        namespace == ANDROID_NS -> "$ANDROID_PREFIX:$name"
        !prefix.isNullOrEmpty() -> "$prefix:$name"
        else -> name
    }

    fun attributeName(attr: BinaryXmlAttribute): String = when {
        attr.namespace == ANDROID_NS -> "$ANDROID_PREFIX:${attr.name}"
        !attr.prefix.isNullOrEmpty() -> "${attr.prefix}:${attr.name}"
        else -> attr.name
    }

    fun formatAttributeValue(attr: BinaryXmlAttribute, resolver: ResourceRefResolver): String {
        val value = attr.typedValue
        // Enum/flag mapping for framework attributes with an int data value.
        if (attr.resourceId != 0) {
            val data = when (value) {
                is TypedValue.IntDec -> value.value
                is TypedValue.IntHex -> value.value
                else -> null
            }
            if (data != null) {
                ManifestEnumFlagNames.format(attr.resourceId, data)?.let { return it }
            }
        }
        return when (value) {
            is TypedValue.Str -> value.value
            is TypedValue.Reference -> formatReference(value.resourceId, attr.rawValueString, isAttr = false, resolver)
            is TypedValue.DynamicReference -> formatReference(value.resourceId, attr.rawValueString, isAttr = false, resolver)
            is TypedValue.AttributeRef -> formatReference(value.resourceId, attr.rawValueString, isAttr = true, resolver)
            is TypedValue.DynamicAttributeRef -> formatReference(value.resourceId, attr.rawValueString, isAttr = true, resolver)
            is TypedValue.IntDec -> value.value.toString()
            is TypedValue.IntHex -> "0x${Integer.toHexString(value.value).uppercase()}"
            is TypedValue.Bool -> value.value.toString()
            is TypedValue.Flt -> value.value.toString()
            is TypedValue.Color -> formatColor(value)
            is TypedValue.Dimension -> formatComplex(value.raw, isDimension = true)
            is TypedValue.Fraction -> formatComplex(value.raw, isDimension = false)
            is TypedValue.Null -> if (value.undefined) "@null" else ""
            is TypedValue.Unknown -> "(type=0x${"%02X".format(value.type)} data=0x${"%08X".format(value.raw)})"
        }
    }

    fun formatReference(
        resourceId: Int,
        rawValueString: String?,
        isAttr: Boolean,
        resolver: ResourceRefResolver,
    ): String {
        val resolved = resolver.resolve(resourceId)
        if (resolved != null) {
            return if (isAttr) "?${resolved.removePrefix("@")}" else resolved
        }
        // Fallback to the raw textual reference if aapt2 preserved one.
        if (rawValueString != null) {
            val prefix = if (isAttr) "?" else "@"
            if (rawValueString.startsWith(prefix)) return rawValueString
        }
        val hex = "0x%08X".format(resourceId)
        return if (isAttr) "?$hex" else "@$hex"
    }

    fun formatColor(color: TypedValue.Color): String = when (color.width) {
        TypedValue.Color.Width.ARGB8 -> "#%08X".format(color.argb)
        TypedValue.Color.Width.RGB8 -> "#%06X".format(color.argb and 0xFFFFFF)
        TypedValue.Color.Width.ARGB4 -> "#%04X".format(
            ((color.argb ushr 28) and 0xF shl 12) or
                ((color.argb ushr 20) and 0xF shl 8) or
                ((color.argb ushr 12) and 0xF shl 4) or
                ((color.argb ushr 4) and 0xF)
        )
        TypedValue.Color.Width.RGB4 -> "#%03X".format(
            ((color.argb ushr 20) and 0xF shl 8) or
                ((color.argb ushr 12) and 0xF shl 4) or
                ((color.argb ushr 4) and 0xF)
        )
    }

    fun formatComplex(raw: Int, isDimension: Boolean): String {
        val mantissa = raw shr COMPLEX_MANTISSA_SHIFT
        val radix = (raw shr COMPLEX_RADIX_SHIFT) and COMPLEX_RADIX_MASK
        val scaleFactor = when (radix) {
            RADIX_23p0 -> 1f
            RADIX_16p7 -> 1f / (1 shl 7)
            RADIX_8p15 -> 1f / (1 shl 15)
            RADIX_0p23 -> 1f / (1 shl 23)
            else -> 1f
        }
        val value = mantissa.toFloat() * scaleFactor
        val unit = raw and COMPLEX_UNIT_MASK
        val suffix = if (isDimension) dimensionSuffix(unit) else fractionSuffix(unit)
        return if (value == value.toInt().toFloat()) {
            "${value.toInt()}$suffix"
        } else {
            "$value$suffix"
        }
    }

    fun escapeXml(value: String): String {
        if (value.isEmpty()) return value
        val sb = StringBuilder(value.length + 8)
        for (ch in value) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun dimensionSuffix(unit: Int): String = when (unit) {
        0 -> "px"
        1 -> "dp"
        2 -> "sp"
        3 -> "pt"
        4 -> "in"
        5 -> "mm"
        else -> ""
    }

    private fun fractionSuffix(unit: Int): String = when (unit) {
        0 -> "%"
        1 -> "%p"
        else -> ""
    }

    // TypedValue complex encoding constants (mirrors android.util.TypedValue).
    private const val COMPLEX_UNIT_MASK = 0xF
    private const val COMPLEX_RADIX_SHIFT = 4
    private const val COMPLEX_RADIX_MASK = 0x3
    private const val COMPLEX_MANTISSA_SHIFT = 8
    private const val RADIX_23p0 = 0
    private const val RADIX_16p7 = 1
    private const val RADIX_8p15 = 2
    private const val RADIX_0p23 = 3
}
