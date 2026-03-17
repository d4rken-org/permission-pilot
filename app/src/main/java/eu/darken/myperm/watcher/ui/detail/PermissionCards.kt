package eu.darken.myperm.watcher.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.compose.PermissionIcon
import eu.darken.myperm.common.compose.Pill
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.watcher.core.PermissionDiff
import eu.darken.myperm.watcher.core.WatcherEventType

@Composable
internal fun PermissionCards(
    diff: PermissionDiff,
    eventType: WatcherEventType,
    enrichedMap: Map<String, EnrichedPermission>,
) {
    when (eventType) {
        WatcherEventType.INSTALL -> {
            val perms = diff.addedPermissions + diff.addedDeclared
            if (perms.isNotEmpty()) {
                PermissionCategoryCard(
                    title = stringResource(R.string.watcher_diff_added_permissions),
                    subtitle = stringResource(R.string.watcher_detail_new_perms_subtitle),
                    titleColor = MaterialTheme.colorScheme.primary,
                    permissions = perms,
                    enrichedMap = enrichedMap,
                    showGrantType = true,
                )
            }
        }
        WatcherEventType.UPDATE -> {
            val added = diff.addedPermissions + diff.addedDeclared
            if (added.isNotEmpty()) {
                PermissionCategoryCard(
                    title = stringResource(R.string.watcher_diff_added_permissions),
                    subtitle = stringResource(R.string.watcher_detail_new_perms_subtitle),
                    titleColor = MaterialTheme.colorScheme.primary,
                    permissions = added,
                    enrichedMap = enrichedMap,
                    showGrantType = true,
                )
            }
            val removed = diff.removedPermissions + diff.removedDeclared
            if (removed.isNotEmpty()) {
                PermissionCategoryCard(
                    title = stringResource(R.string.watcher_diff_removed_permissions),
                    subtitle = stringResource(R.string.watcher_detail_removed_perms_subtitle),
                    titleColor = MaterialTheme.colorScheme.error,
                    permissions = removed,
                    enrichedMap = enrichedMap,
                    showGrantType = false,
                )
            }
            if (diff.grantChanges.isNotEmpty()) {
                GrantChangesCategoryCard(
                    title = stringResource(R.string.watcher_diff_grant_changes),
                    subtitle = stringResource(R.string.watcher_detail_grant_changes_subtitle),
                    grantChanges = diff.grantChanges,
                    enrichedMap = enrichedMap,
                )
            }
        }
        WatcherEventType.REMOVED -> {
            val perms = diff.addedPermissions + diff.addedDeclared
            if (perms.isNotEmpty()) {
                PermissionCategoryCard(
                    title = pluralStringResource(R.plurals.watcher_detail_last_permissions_header, perms.size, perms.size),
                    subtitle = stringResource(R.string.watcher_detail_last_known_perms_subtitle),
                    titleColor = MaterialTheme.colorScheme.primary,
                    permissions = perms,
                    enrichedMap = enrichedMap,
                    showGrantType = false,
                )
            }
        }
        WatcherEventType.GRANT_CHANGE -> {
            if (diff.grantChanges.isNotEmpty()) {
                GrantChangesCategoryCard(
                    title = stringResource(R.string.watcher_diff_grant_changes),
                    subtitle = stringResource(R.string.watcher_detail_grant_changes_subtitle),
                    grantChanges = diff.grantChanges,
                    enrichedMap = enrichedMap,
                )
            }
        }
    }
}

@Composable
internal fun PermissionCategoryCard(
    title: String,
    subtitle: String,
    titleColor: Color,
    permissions: List<String>,
    enrichedMap: Map<String, EnrichedPermission>,
    showGrantType: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            permissions.forEachIndexed { index, permId ->
                EnrichedPermissionEntry(
                    permissionId = permId,
                    enriched = enrichedMap[permId],
                    showGrantType = showGrantType,
                )
                if (index < permissions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 28.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnrichedPermissionEntry(
    permissionId: String,
    enriched: EnrichedPermission?,
    showGrantType: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val hasDescription = enriched?.description != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasDescription) Modifier.clickable(
                    onClickLabel = stringResource(R.string.watcher_detail_toggle_description),
                ) { expanded = !expanded }
                else Modifier
            )
            .padding(vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PermissionIcon(
                permissionId = Permission.Id(permissionId),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = enriched?.label ?: permissionId.substringAfterLast('.'),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = permissionId,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (hasDescription) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.watcher_detail_toggle_description),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(visible = expanded && hasDescription) {
            Text(
                text = enriched?.description.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 28.dp, top = 4.dp),
            )
        }
        if (showGrantType && enriched != null && enriched.grantType != GrantType.UNKNOWN) {
            Spacer(modifier = Modifier.height(4.dp))
            Pill(
                text = when (enriched.grantType) {
                    GrantType.RUNTIME, GrantType.SPECIAL_ACCESS -> stringResource(R.string.watcher_detail_grant_type_approval)
                    else -> stringResource(R.string.watcher_detail_grant_type_automatic)
                },
                containerColor = when (enriched.grantType) {
                    GrantType.RUNTIME, GrantType.SPECIAL_ACCESS -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = when (enriched.grantType) {
                    GrantType.RUNTIME, GrantType.SPECIAL_ACCESS -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                compact = true,
                modifier = Modifier.padding(start = 28.dp),
            )
        }
    }
}

@Composable
internal fun GrantChangesCategoryCard(
    title: String,
    subtitle: String,
    grantChanges: List<PermissionDiff.GrantChange>,
    enrichedMap: Map<String, EnrichedPermission>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            grantChanges.forEachIndexed { index, change ->
                GrantChangeEntry(
                    change = change,
                    enriched = enrichedMap[change.permissionId],
                )
                if (index < grantChanges.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 28.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GrantChangeEntry(
    change: PermissionDiff.GrantChange,
    enriched: EnrichedPermission?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PermissionIcon(
                permissionId = Permission.Id(change.permissionId),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = enriched?.label ?: change.permissionId.substringAfterLast('.'),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = change.permissionId,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 28.dp, top = 4.dp),
        ) {
            GrantStatusIcon(change.oldStatus, Modifier.size(16.dp))
            Text(
                text = grantStatusLabel(change.oldStatus),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GrantStatusIcon(change.newStatus, Modifier.size(16.dp))
            Text(
                text = grantStatusLabel(change.newStatus),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun grantStatusLabel(status: UsesPermission.Status): String = when (status) {
    UsesPermission.Status.GRANTED -> stringResource(R.string.filter_granted_label)
    UsesPermission.Status.GRANTED_IN_USE -> stringResource(R.string.permissions_status_granted_in_use_label)
    UsesPermission.Status.DENIED -> stringResource(R.string.filter_denied_label)
    UsesPermission.Status.UNKNOWN -> stringResource(R.string.watcher_detail_unknown_permission)
}

@Composable
private fun GrantStatusIcon(status: UsesPermission.Status, modifier: Modifier = Modifier) {
    when (status) {
        UsesPermission.Status.GRANTED,
        UsesPermission.Status.GRANTED_IN_USE -> Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier,
        )
        UsesPermission.Status.DENIED -> Icon(
            Icons.Filled.Cancel,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        UsesPermission.Status.UNKNOWN -> Icon(
            Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

@Preview
@Composable
private fun PermissionCategoryCardPreview() {
    MaterialTheme {
        PermissionCategoryCard(
            title = "Added Permissions",
            subtitle = "These permissions were added",
            titleColor = MaterialTheme.colorScheme.primary,
            permissions = listOf(
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION",
            ),
            enrichedMap = mapOf(
                "android.permission.CAMERA" to EnrichedPermission(
                    id = "android.permission.CAMERA",
                    label = "Camera",
                    description = "Allows the app to take pictures and videos",
                    grantType = GrantType.RUNTIME,
                ),
                "android.permission.RECORD_AUDIO" to EnrichedPermission(
                    id = "android.permission.RECORD_AUDIO",
                    label = "Microphone",
                    description = null,
                    grantType = GrantType.RUNTIME,
                ),
            ),
            showGrantType = true,
        )
    }
}

@Preview
@Composable
private fun GrantChangesCategoryCardPreview() {
    MaterialTheme {
        GrantChangesCategoryCard(
            title = "Grant Changes",
            subtitle = "These permissions changed grant status",
            grantChanges = listOf(
                PermissionDiff.GrantChange(
                    "android.permission.CAMERA",
                    UsesPermission.Status.DENIED,
                    UsesPermission.Status.GRANTED,
                ),
                PermissionDiff.GrantChange(
                    "android.permission.LOCATION",
                    UsesPermission.Status.GRANTED,
                    UsesPermission.Status.DENIED,
                ),
            ),
            enrichedMap = mapOf(
                "android.permission.CAMERA" to EnrichedPermission(
                    id = "android.permission.CAMERA",
                    label = "Camera",
                    description = null,
                    grantType = GrantType.RUNTIME,
                ),
            ),
        )
    }
}
