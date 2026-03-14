package eu.darken.myperm.watcher.core

import androidx.room.TypeConverter

enum class WatcherEventType {
    INSTALL,
    UPDATE,
    REMOVED,
    GRANT_CHANGE,
    ;

    class Converter {
        @TypeConverter
        fun fromEnum(value: WatcherEventType): String = value.name

        @TypeConverter
        fun toEnum(value: String): WatcherEventType = valueOf(value)
    }
}
