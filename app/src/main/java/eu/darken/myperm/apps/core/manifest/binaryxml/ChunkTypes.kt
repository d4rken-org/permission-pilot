package eu.darken.myperm.apps.core.manifest.binaryxml

internal object ChunkTypes {
    const val RES_NULL_TYPE: Int = 0x0000
    const val RES_STRING_POOL_TYPE: Int = 0x0001
    const val RES_XML_TYPE: Int = 0x0003
    const val RES_XML_RESOURCE_MAP_TYPE: Int = 0x0180

    const val RES_XML_START_NAMESPACE_TYPE: Int = 0x0100
    const val RES_XML_END_NAMESPACE_TYPE: Int = 0x0101
    const val RES_XML_START_ELEMENT_TYPE: Int = 0x0102
    const val RES_XML_END_ELEMENT_TYPE: Int = 0x0103
    const val RES_XML_CDATA_TYPE: Int = 0x0104

    const val STRING_POOL_UTF8_FLAG: Int = 1 shl 8
    const val STRING_POOL_SORTED_FLAG: Int = 1 shl 0

    const val NULL_INDEX: Int = -1 // 0xFFFFFFFF when read as unsigned
}

internal object ResTypes {
    const val TYPE_NULL: Int = 0x00
    const val TYPE_REFERENCE: Int = 0x01
    const val TYPE_ATTRIBUTE: Int = 0x02
    const val TYPE_STRING: Int = 0x03
    const val TYPE_FLOAT: Int = 0x04
    const val TYPE_DIMENSION: Int = 0x05
    const val TYPE_FRACTION: Int = 0x06
    const val TYPE_DYNAMIC_REFERENCE: Int = 0x07
    const val TYPE_DYNAMIC_ATTRIBUTE: Int = 0x08

    const val TYPE_FIRST_INT: Int = 0x10
    const val TYPE_INT_DEC: Int = 0x10
    const val TYPE_INT_HEX: Int = 0x11
    const val TYPE_INT_BOOLEAN: Int = 0x12

    const val TYPE_FIRST_COLOR_INT: Int = 0x1C
    const val TYPE_INT_COLOR_ARGB8: Int = 0x1C
    const val TYPE_INT_COLOR_RGB8: Int = 0x1D
    const val TYPE_INT_COLOR_ARGB4: Int = 0x1E
    const val TYPE_INT_COLOR_RGB4: Int = 0x1F
    const val TYPE_LAST_COLOR_INT: Int = 0x1F
    const val TYPE_LAST_INT: Int = 0x1F

    const val DATA_NULL_UNDEFINED: Int = 0
    const val DATA_NULL_EMPTY: Int = 1
}
