package eu.darken.myperm.watcher.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.compose.AppIcon
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.watcher.core.PermissionDiff
import java.text.DateFormat
import java.util.Date

@Composable
fun ReportDetailScreenHost(
    route: Nav.Watcher.ReportDetail,
    vm: ReportDetailViewModel = hiltViewModel(
        key = "report_${route.reportId}",
        creationCallback = { factory: ReportDetailViewModel.Factory -> factory.create(route.reportId) },
    ),
) {
    val navCtrl = LocalNavigationController.current
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()

    ReportDetailScreen(
        state = state,
        onBack = { navCtrl?.up() },
        onViewApp = { vm.onViewApp() },
    )
}

@Composable
fun ReportDetailScreen(
    state: ReportDetailViewModel.State,
    onBack: () -> Unit,
    onViewApp: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.watcher_report_detail_label)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!state.isLoading && state.packageName.isNotEmpty()) {
                        IconButton(onClick = onViewApp) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = stringResource(R.string.apps_app_details_label),
                            )
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isLoading) return@Column

            AppHeaderCard(state)
            EventDetailsCard(state)
            PermissionCards(state)
        }
    }
}

// Section 1: App Header

@Composable
private fun AppHeaderCard(state: ReportDetailViewModel.State) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                pkg = Pkg.Container(Pkg.Id(state.packageName)),
                isSystemApp = state.isSystemApp,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.appLabel ?: state.packageName,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = state.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EventTypeChip(state.eventType)
                    Text(
                        text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(state.detectedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventTypeChip(eventType: String) {
    val (containerColor, contentColor, label) = when (eventType) {
        "INSTALL" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            stringResource(R.string.watcher_event_install),
        )
        "UPDATE" -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            stringResource(R.string.watcher_event_update),
        )
        "REMOVED" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            stringResource(R.string.watcher_event_removed),
        )
        "GRANT_CHANGE" -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            stringResource(R.string.watcher_event_grant_change),
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            eventType,
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// Section 2: Event Details

@Composable
private fun EventDetailsCard(state: ReportDetailViewModel.State) {
    val hasContent = when (state.eventType) {
        "INSTALL", "UPDATE", "REMOVED" -> true
        "GRANT_CHANGE" -> true
        else -> false
    }
    if (!hasContent) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (state.eventType) {
                "INSTALL" -> {
                    val versionText = buildVersionText(state.versionName, state.versionCode)
                    if (versionText != null) {
                        DetailRow(stringResource(R.string.watcher_detail_version_label), versionText)
                    }
                    DetailRow(
                        stringResource(R.string.watcher_detail_installer_label),
                        state.installerLabel ?: stringResource(R.string.watcher_detail_installer_unknown),
                    )
                }
                "UPDATE" -> {
                    if (state.previousVersionName != null || state.versionName != null) {
                        val fromVersion = state.previousVersionName ?: "?"
                        val toVersion = state.versionName ?: "?"
                        DetailRow(stringResource(R.string.watcher_detail_version_label), "$fromVersion \u2192 $toVersion")
                    }
                    if (state.previousVersionCode != null || state.versionCode != null) {
                        val fromCode = state.previousVersionCode?.toString() ?: "?"
                        val toCode = state.versionCode?.toString() ?: "?"
                        DetailRow("Code", "$fromCode \u2192 $toCode")
                    }
                }
                "REMOVED" -> {
                    val versionText = buildVersionText(state.versionName, state.versionCode)
                    if (versionText != null) {
                        DetailRow(stringResource(R.string.watcher_detail_version_label), versionText)
                    }
                    Text(
                        text = stringResource(R.string.watcher_detail_app_removed_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                "GRANT_CHANGE" -> {
                    Text(
                        text = stringResource(R.string.watcher_detail_grant_change_desc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun buildVersionText(versionName: String?, versionCode: Long?): String? {
    if (versionName == null && versionCode == null) return null
    return when {
        versionName != null && versionCode != null -> "$versionName ($versionCode)"
        versionName != null -> versionName
        else -> "($versionCode)"
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// Section 3: Permissions

@Composable
private fun PermissionCards(state: ReportDetailViewModel.State) {
    when (state.eventType) {
        "INSTALL" -> {
            val perms = state.diff.addedPermissions + state.diff.addedDeclared
            if (perms.isNotEmpty()) {
                PermissionCategoryCard(
                    title = stringResource(R.string.watcher_diff_added_permissions),
                    subtitle = stringResource(R.string.watcher_detail_new_perms_subtitle),
                    titleColor = MaterialTheme.colorScheme.primary,
                    permissions = perms,
                    enrichedMap = state.permissionInfoMap,
                    showGrantType = true,
                )
            }
        }
        "UPDATE" -> {
            val added = state.diff.addedPermissions + state.diff.addedDeclared
            if (added.isNotEmpty()) {
                PermissionCategoryCard(
                    title = stringResource(R.string.watcher_diff_added_permissions),
                    subtitle = stringResource(R.string.watcher_detail_new_perms_subtitle),
                    titleColor = MaterialTheme.colorScheme.primary,
                    permissions = added,
                    enrichedMap = state.permissionInfoMap,
                    showGrantType = true,
                )
            }
            val removed = state.diff.removedPermissions + state.diff.removedDeclared
            if (removed.isNotEmpty()) {
                PermissionCategoryCard(
                    title = stringResource(R.string.watcher_diff_removed_permissions),
                    subtitle = stringResource(R.string.watcher_detail_removed_perms_subtitle),
                    titleColor = MaterialTheme.colorScheme.error,
                    permissions = removed,
                    enrichedMap = state.permissionInfoMap,
                    showGrantType = false,
                )
            }
            if (state.diff.grantChanges.isNotEmpty()) {
                GrantChangesCategoryCard(
                    title = stringResource(R.string.watcher_diff_grant_changes),
                    subtitle = stringResource(R.string.watcher_detail_grant_changes_subtitle),
                    grantChanges = state.diff.grantChanges,
                    enrichedMap = state.permissionInfoMap,
                )
            }
        }
        "REMOVED" -> {
            val perms = state.diff.addedPermissions + state.diff.addedDeclared
            if (perms.isNotEmpty()) {
                PermissionCategoryCard(
                    title = stringResource(R.string.watcher_detail_last_permissions_header, perms.size),
                    subtitle = stringResource(R.string.watcher_detail_last_known_perms_subtitle),
                    titleColor = MaterialTheme.colorScheme.primary,
                    permissions = perms,
                    enrichedMap = state.permissionInfoMap,
                    showGrantType = false,
                )
            }
        }
        "GRANT_CHANGE" -> {
            if (state.diff.grantChanges.isNotEmpty()) {
                GrantChangesCategoryCard(
                    title = stringResource(R.string.watcher_diff_grant_changes),
                    subtitle = stringResource(R.string.watcher_detail_grant_changes_subtitle),
                    grantChanges = state.diff.grantChanges,
                    enrichedMap = state.permissionInfoMap,
                )
            }
        }
    }
}

@Composable
private fun PermissionCategoryCard(
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
            permissions.forEach { permId ->
                EnrichedPermissionEntry(
                    permissionId = permId,
                    enriched = enrichedMap[permId],
                    showGrantType = showGrantType,
                )
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
            .padding(vertical = 6.dp),
    ) {
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
        AnimatedVisibility(visible = expanded && hasDescription) {
            Text(
                text = enriched?.description.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (showGrantType && enriched != null && enriched.grantType != GrantType.UNKNOWN) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (enriched.grantType) {
                    GrantType.RUNTIME, GrantType.SPECIAL_ACCESS -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    text = when (enriched.grantType) {
                        GrantType.RUNTIME, GrantType.SPECIAL_ACCESS -> stringResource(R.string.watcher_detail_grant_type_approval)
                        else -> stringResource(R.string.watcher_detail_grant_type_automatic)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (enriched.grantType) {
                        GrantType.RUNTIME, GrantType.SPECIAL_ACCESS -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun GrantChangesCategoryCard(
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
            grantChanges.forEach { change ->
                GrantChangeEntry(
                    change = change,
                    enriched = enrichedMap[change.permissionId],
                )
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
            .padding(vertical = 6.dp),
    ) {
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
        Text(
            text = "${change.oldStatus} \u2192 ${change.newStatus}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Preview
@Composable
private fun ReportDetailUpdatePreview() {
    MaterialTheme {
        ReportDetailScreen(
            state = ReportDetailViewModel.State(
                isLoading = false,
                packageName = "com.example.app",
                appLabel = "Example App",
                eventType = "UPDATE",
                versionName = "2.1.0",
                previousVersionName = "2.0.3",
                versionCode = 210,
                previousVersionCode = 203,
                detectedAt = System.currentTimeMillis(),
                diff = PermissionDiff(
                    addedPermissions = listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO"),
                    removedPermissions = listOf("android.permission.READ_CONTACTS"),
                    grantChanges = listOf(
                        PermissionDiff.GrantChange("android.permission.LOCATION", "denied", "granted"),
                    ),
                ),
            ),
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ReportDetailInstallPreview() {
    MaterialTheme {
        ReportDetailScreen(
            state = ReportDetailViewModel.State(
                isLoading = false,
                packageName = "com.example.newapp",
                appLabel = "New App",
                eventType = "INSTALL",
                versionName = "1.0.0",
                versionCode = 100,
                installerLabel = "Google Play Store",
                detectedAt = System.currentTimeMillis(),
                diff = PermissionDiff(
                    addedPermissions = listOf("android.permission.INTERNET", "android.permission.CAMERA"),
                ),
            ),
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ReportDetailRemovedPreview() {
    MaterialTheme {
        ReportDetailScreen(
            state = ReportDetailViewModel.State(
                isLoading = false,
                packageName = "com.example.oldapp",
                appLabel = "Old App",
                eventType = "REMOVED",
                versionName = "3.2.1",
                versionCode = 321,
                detectedAt = System.currentTimeMillis(),
                diff = PermissionDiff(
                    addedPermissions = listOf("android.permission.INTERNET"),
                ),
            ),
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ReportDetailGrantChangePreview() {
    MaterialTheme {
        ReportDetailScreen(
            state = ReportDetailViewModel.State(
                isLoading = false,
                packageName = "com.example.app",
                appLabel = "Some App",
                eventType = "GRANT_CHANGE",
                detectedAt = System.currentTimeMillis(),
                diff = PermissionDiff(
                    grantChanges = listOf(
                        PermissionDiff.GrantChange("android.permission.CAMERA", "denied", "granted"),
                        PermissionDiff.GrantChange("android.permission.LOCATION", "granted", "denied"),
                    ),
                ),
            ),
            onBack = {},
        )
    }
}
