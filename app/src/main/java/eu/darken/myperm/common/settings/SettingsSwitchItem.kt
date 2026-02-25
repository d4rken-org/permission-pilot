package eu.darken.myperm.common.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SettingsBaseItem(
        icon = icon,
        title = title,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        subtitle = subtitle,
        enabled = enabled,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    )
}

@Preview2
@Composable
private fun SettingsSwitchItemPreview() = PreviewWrapper {
    SettingsSwitchItem(
        icon = Icons.TwoTone.Notifications,
        title = "Show notifications",
        subtitle = "Display connection notifications",
        checked = true,
        onCheckedChange = {},
    )
}
