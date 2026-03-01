package eu.darken.myperm.common.theming

import androidx.annotation.StringRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import eu.darken.myperm.R

@JsonClass(generateAdapter = false)
enum class ThemeStyle(
    @StringRes val labelRes: Int,
) {
    @Json(name = "theme.style.default") DEFAULT(R.string.ui_theme_style_default_label),
    @Json(name = "theme.style.materialyou") MATERIAL_YOU(R.string.ui_theme_style_materialyou_label),
    @Json(name = "theme.style.mediumcontrast") MEDIUM_CONTRAST(R.string.ui_theme_style_mediumcontrast_label),
    @Json(name = "theme.style.highcontrast") HIGH_CONTRAST(R.string.ui_theme_style_highcontrast_label),
}
