package eu.darken.myperm.common.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.features.Highlighted
import eu.darken.myperm.permissions.core.features.InstallTimeGrant
import eu.darken.myperm.permissions.core.features.ManifestDoc
import eu.darken.myperm.permissions.core.features.NotNormalPerm
import eu.darken.myperm.permissions.core.features.PermissionTag
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess

@Composable
fun Pill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (compact) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = contentColor,
            modifier = modifier
                .background(containerColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = modifier
                .background(containerColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun PermissionTagPill(tag: PermissionTag, compact: Boolean = false) {
    val (text, containerColor, contentColor) = when (tag) {
        is RuntimeGrant -> Triple(
            stringResource(R.string.permissions_tag_runtime_label),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        is SpecialAccess -> Triple(
            stringResource(R.string.permissions_tag_special_access_label),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        is InstallTimeGrant -> Triple(
            stringResource(R.string.permissions_tag_install_time_label),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        is ManifestDoc -> Triple(
            stringResource(R.string.permissions_tag_documented_label),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is Highlighted -> Triple(
            stringResource(R.string.permissions_tag_notable_label),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is NotNormalPerm -> Triple(
            stringResource(R.string.permissions_tag_non_standard_label),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Pill(text = text, containerColor = containerColor, contentColor = contentColor, compact = compact)
}
