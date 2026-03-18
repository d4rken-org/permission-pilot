package eu.darken.myperm.common.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsUpgradeIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.TwoTone.Stars,
        contentDescription = null,
        modifier = modifier.padding(start = 16.dp).size(24.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}
