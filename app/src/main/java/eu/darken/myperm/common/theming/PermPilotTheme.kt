package eu.darken.myperm.common.theming

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether dynamic colors (Material You) are currently active.
 * Exposed so settings UI can disable the accent color picker when true.
 */
val LocalIsDynamicColorActive = compositionLocalOf { false }

@Composable
fun PermPilotTheme(
    state: ThemeState = ThemeState(),
    content: @Composable () -> Unit,
) {
    val darkTheme = when (state.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val isDynamicColorActive = state.style == ThemeStyle.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val context = LocalContext.current
    val colorScheme = remember(state, darkTheme, isDynamicColorActive, context) {
        if (isDynamicColorActive) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) {
                ThemeColorProvider.getDarkColorScheme(state.color, state.style)
            } else {
                ThemeColorProvider.getLightColorScheme(state.color, state.style)
            }
        }
    }

    CompositionLocalProvider(LocalIsDynamicColorActive provides isDynamicColorActive) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
