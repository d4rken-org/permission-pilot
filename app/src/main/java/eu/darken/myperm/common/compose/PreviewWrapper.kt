package eu.darken.myperm.common.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import eu.darken.myperm.common.theming.PermPilotTheme

@Composable
fun PreviewWrapper(content: @Composable () -> Unit) {
    PermPilotTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
