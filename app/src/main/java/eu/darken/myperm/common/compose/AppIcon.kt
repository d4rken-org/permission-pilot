package eu.darken.myperm.common.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import eu.darken.myperm.apps.core.Pkg

@Composable
fun AppIcon(
    pkg: Pkg,
    isSystemApp: Boolean,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 10.dp,
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = pkg,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            error = rememberVectorPainter(Icons.TwoTone.Android),
            placeholder = rememberVectorPainter(Icons.TwoTone.Android),
        )
        if (isSystemApp) {
            Icon(
                imageVector = Icons.TwoTone.Shield,
                contentDescription = null,
                modifier = Modifier.size(badgeSize).align(Alignment.TopStart),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
