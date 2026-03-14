package eu.darken.myperm.apps.core.features

import androidx.room.TypeConverter

enum class InternetAccess {
    DIRECT,
    INDIRECT,
    NONE,
    UNKNOWN;

    class Converter {
        @TypeConverter
        fun fromEnum(value: InternetAccess): String = value.name

        @TypeConverter
        fun toEnum(value: String): InternetAccess = valueOf(value)
    }
}