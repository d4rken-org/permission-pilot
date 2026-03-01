package eu.darken.myperm.common.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R
import eu.darken.myperm.common.theming.PermPilotColorsAmber
import eu.darken.myperm.common.theming.PermPilotColorsBlue
import eu.darken.myperm.common.theming.PermPilotColorsGreen
import eu.darken.myperm.common.theming.ThemeColor

@Composable
fun ThemeColorSelectorDialog(
    selectedColor: ThemeColor,
    onColorSelected: (ThemeColor) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.ui_theme_color_label)) },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeColor.entries.forEach { color ->
                    val isSelected = color == selectedColor
                    val (lightPreview, darkPreview) = colorPreviewPair(color)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { onColorSelected(color) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                        Text(
                            text = stringResource(color.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(lightPreview)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(darkPreview)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

private fun colorPreviewPair(color: ThemeColor): Pair<Color, Color> = when (color) {
    ThemeColor.BLUE -> PermPilotColorsBlue.LightDefault.primary to PermPilotColorsBlue.DarkDefault.primary
    ThemeColor.GREEN -> PermPilotColorsGreen.LightDefault.primary to PermPilotColorsGreen.DarkDefault.primary
    ThemeColor.AMBER -> PermPilotColorsAmber.LightDefault.primary to PermPilotColorsAmber.DarkDefault.primary
}
