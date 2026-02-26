package eu.darken.myperm.common.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import eu.darken.myperm.permissions.core.Permission

@Composable
fun PermissionIcon(
    permissionId: Permission.Id,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    fallbackModel: Any? = null,
) {
    val knownIcon = remember(permissionId) { permissionId.toIcon() }
    if (knownIcon != null) {
        Icon(imageVector = knownIcon, contentDescription = null, modifier = modifier, tint = tint)
    } else if (fallbackModel != null) {
        AsyncImage(model = fallbackModel, contentDescription = null, modifier = modifier)
    } else {
        Icon(imageVector = Icons.TwoTone.Security, contentDescription = null, modifier = modifier, tint = tint)
    }
}
