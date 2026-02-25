package eu.darken.myperm.common.theming

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class ThemeMode {
    @Json(name = "theme.mode.system") SYSTEM,
    @Json(name = "theme.mode.dark") DARK,
    @Json(name = "theme.mode.light") LIGHT,
}
