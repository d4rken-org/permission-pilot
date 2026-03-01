package eu.darken.myperm.common.theming

import androidx.compose.material3.ColorScheme

object ThemeColorProvider {

    fun getLightColorScheme(color: ThemeColor, style: ThemeStyle): ColorScheme = when (color) {
        ThemeColor.BLUE -> PermPilotColorsBlue.lightScheme(style)
        ThemeColor.GREEN -> PermPilotColorsGreen.lightScheme(style)
        ThemeColor.AMBER -> PermPilotColorsAmber.lightScheme(style)
    }

    fun getDarkColorScheme(color: ThemeColor, style: ThemeStyle): ColorScheme = when (color) {
        ThemeColor.BLUE -> PermPilotColorsBlue.darkScheme(style)
        ThemeColor.GREEN -> PermPilotColorsGreen.darkScheme(style)
        ThemeColor.AMBER -> PermPilotColorsAmber.darkScheme(style)
    }
}
