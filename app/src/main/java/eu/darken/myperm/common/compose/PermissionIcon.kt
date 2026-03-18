package eu.darken.myperm.common.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.permissions.core.Permission

@Composable
fun PermissionIcon(
    permissionId: Permission.Id,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    fallbackModel: Any? = null,
    status: UsesPermission.Status? = null,
) {
    val knownIcon = remember(permissionId) { permissionId.toIcon() }

    if (status != null) {
        Box(modifier = modifier) {
            val iconModifier = Modifier.fillMaxSize()
            if (knownIcon != null) {
                Icon(imageVector = knownIcon, contentDescription = null, modifier = iconModifier, tint = tint)
            } else if (fallbackModel != null) {
                AsyncImage(model = fallbackModel, contentDescription = null, modifier = iconModifier)
            } else {
                Icon(imageVector = Icons.TwoTone.Security, contentDescription = null, modifier = iconModifier, tint = tint)
            }
            StatusDot(
                status = status,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    } else {
        if (knownIcon != null) {
            Icon(imageVector = knownIcon, contentDescription = null, modifier = modifier, tint = tint)
        } else if (fallbackModel != null) {
            AsyncImage(model = fallbackModel, contentDescription = null, modifier = modifier)
        } else {
            Icon(imageVector = Icons.TwoTone.Security, contentDescription = null, modifier = modifier, tint = tint)
        }
    }
}

@Composable
private fun StatusDot(
    status: UsesPermission.Status,
    modifier: Modifier = Modifier,
) {
    val dotColor = when (status) {
        UsesPermission.Status.GRANTED -> MaterialTheme.colorScheme.primary
        UsesPermission.Status.GRANTED_IN_USE -> MaterialTheme.colorScheme.secondary
        UsesPermission.Status.DENIED -> MaterialTheme.colorScheme.error
        UsesPermission.Status.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .size(8.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .background(dotColor, CircleShape),
    )
}
