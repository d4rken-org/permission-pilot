package eu.darken.myperm.apps.core.manifest.binaryxml

sealed class TypedValue {
    data class Str(val value: String) : TypedValue()
    data class Reference(val resourceId: Int) : TypedValue()
    data class AttributeRef(val resourceId: Int) : TypedValue()
    data class DynamicReference(val resourceId: Int) : TypedValue()
    data class DynamicAttributeRef(val resourceId: Int) : TypedValue()
    data class IntDec(val value: Int) : TypedValue()
    data class IntHex(val value: Int) : TypedValue()
    data class Bool(val value: Boolean) : TypedValue()
    data class Flt(val value: Float) : TypedValue()
    data class Dimension(val raw: Int) : TypedValue()
    data class Fraction(val raw: Int) : TypedValue()
    data class Color(val argb: Int, val width: Width) : TypedValue() {
        enum class Width { RGB4, ARGB4, RGB8, ARGB8 }
    }

    /** true = `DATA_NULL_UNDEFINED` (the attribute has no value), false = `DATA_NULL_EMPTY` (explicit empty). */
    data class Null(val undefined: Boolean) : TypedValue()
    data class Unknown(val type: Int, val raw: Int) : TypedValue()
}

data class BinaryXmlAttribute(
    val namespace: String?,
    val prefix: String?,
    val name: String,
    val resourceId: Int,
    val rawValueString: String?,
    val typedValue: TypedValue,
)
