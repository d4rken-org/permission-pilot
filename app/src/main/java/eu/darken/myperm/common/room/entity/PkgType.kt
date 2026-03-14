package eu.darken.myperm.common.room.entity

import androidx.room.TypeConverter

enum class PkgType {
    PRIMARY,
    SECONDARY_PROFILE,
    SECONDARY_USER,
    UNINSTALLED;

    class Converter {
        @TypeConverter
        fun fromEnum(value: PkgType): String = value.name

        @TypeConverter
        fun toEnum(value: String): PkgType = valueOf(value)
    }
}
