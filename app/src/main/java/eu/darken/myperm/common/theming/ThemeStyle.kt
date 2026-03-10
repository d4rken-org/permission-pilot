package eu.darken.myperm.common.theming

import androidx.annotation.StringRes
import eu.darken.myperm.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeStyle(
    @StringRes val labelRes: Int,
) {
    @SerialName("theme.style.default") DEFAULT(R.string.ui_theme_style_default_label),
    @SerialName("theme.style.materialyou") MATERIAL_YOU(R.string.ui_theme_style_materialyou_label),
    @SerialName("theme.style.mediumcontrast") MEDIUM_CONTRAST(R.string.ui_theme_style_mediumcontrast_label),
    @SerialName("theme.style.highcontrast") HIGH_CONTRAST(R.string.ui_theme_style_highcontrast_label),
}
