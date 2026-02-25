package eu.darken.myperm.common.theming

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class ThemeStyle {
    @Json(name = "theme.style.default") DEFAULT,
    @Json(name = "theme.style.materialyou") MATERIAL_YOU,
}
