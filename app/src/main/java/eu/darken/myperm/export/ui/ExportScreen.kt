package eu.darken.myperm.export.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.Pill
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.export.core.AppExportConfig
import eu.darken.myperm.export.core.AppExportConfig.PermissionDetailLevel
import eu.darken.myperm.export.core.ExportFormat
import eu.darken.myperm.export.core.PermissionExportConfig

@Composable
fun ExportScreenHost(
    route: Nav.Export.Config,
    vm: ExportViewModel = hiltViewModel(),
) {
    LaunchedEffect(route) { vm.init(route) }

    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()
    val isPro by vm.isPro.collectAsState()

    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri -> vm.onSafResult(uri) }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is ExportViewModel.Event.LaunchSaf -> {
                    safLauncher.launch(event.fileName)
                }
            }
        }
    }

    state?.let { s ->
        ExportScreen(
            state = s,
            onBack = { vm.navUp() },
            onAppConfigChanged = { vm.updateAppConfig(it) },
            onPermConfigChanged = { vm.updatePermConfig(it) },
            onExport = { vm.startExport() },
            onUpgrade = { vm.onUpgrade() },
            onResetResult = { vm.resetExportResult() },
        )
    }
}

@Composable
fun ExportScreen(
    state: ExportViewModel.State,
    onBack: () -> Unit,
    onAppConfigChanged: ((AppExportConfig) -> AppExportConfig) -> Unit,
    onPermConfigChanged: ((PermissionExportConfig) -> PermissionExportConfig) -> Unit,
    onExport: () -> Unit,
    onUpgrade: () -> Unit,
    onResetResult: () -> Unit,
) {
    val isApps = state.mode is ExportViewModel.ExportMode.Apps
    val title = stringResource(
        if (isApps) R.string.export_app_info_title else R.string.export_permission_info_title
    )
    val subtitle = if (isApps) {
        pluralStringResource(R.plurals.export_title_apps, state.effectiveItemCount, state.effectiveItemCount)
    } else {
        pluralStringResource(R.plurals.export_title_permissions, state.effectiveItemCount, state.effectiveItemCount)
    }

    val density = LocalDensity.current
    val navBarBottomPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.general_close_action))
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.exportResult == null && state.effectiveItemCount > 0) {
                FloatingActionButton(
                    onClick = onExport,
                    modifier = Modifier.padding(bottom = navBarBottomPadding),
                ) {
                    Icon(Icons.Filled.Description, contentDescription = stringResource(R.string.export_action_label))
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val result = state.exportResult) {
                is ExportViewModel.ExportResult.InProgress -> ExportProgressContent()

                is ExportViewModel.ExportResult.Success -> ExportSuccessContent(
                    result = result,
                )

                is ExportViewModel.ExportResult.Error -> ExportErrorContent(
                    error = result.throwable,
                    onRetry = onResetResult,
                )

                null -> ExportConfigContent(
                    state = state,
                    onAppConfigChanged = onAppConfigChanged,
                    onPermConfigChanged = onPermConfigChanged,
                    onUpgrade = onUpgrade,
                )
            }
        }
    }
}

@Composable
private fun ExportConfigContent(
    state: ExportViewModel.State,
    onAppConfigChanged: ((AppExportConfig) -> AppExportConfig) -> Unit,
    onPermConfigChanged: ((PermissionExportConfig) -> PermissionExportConfig) -> Unit,
    onUpgrade: () -> Unit,
) {
    // Settings card: format + content toggles grouped
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Format section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.export_format_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val selectedFormat = state.appConfig?.format ?: state.permConfig?.format
                if (selectedFormat != null) {
                    Pill(
                        text = ".${selectedFormat.extension}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                ExportFormat.entries.forEach { format ->
                    val isSelected = when {
                        state.appConfig != null -> state.appConfig.format == format
                        state.permConfig != null -> state.permConfig.format == format
                        else -> false
                    }
                    val isLocked = !state.isPro && format != ExportFormat.MARKDOWN
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isLocked) {
                                onUpgrade()
                            } else {
                                state.appConfig?.let { onAppConfigChanged { it.copy(format = format) } }
                                state.permConfig?.let { onPermConfigChanged { it.copy(format = format) } }
                            }
                        },
                        label = { Text(stringResource(format.labelRes)) },
                        trailingIcon = if (isLocked) {
                            { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // Content toggles
            state.appConfig?.let { cfg -> AppExportToggles(cfg, onAppConfigChanged) }
            state.permConfig?.let { cfg -> PermExportToggles(cfg, onPermConfigChanged) }
        }
    }

    // Freemium banner
    if (state.isFreeLimited) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.export_free_limit_message, FREE_EXPORT_LIMIT),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                TextButton(
                    onClick = onUpgrade,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.export_upgrade_action))
                }
            }
        }
    }

    // Preview
    Card {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.export_preview_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.effectiveItemCount > 1) {
                    Text(
                        text = "1 / ${state.effectiveItemCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.isPreviewLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else if (state.preview != null) {
                Text(
                    text = state.preview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // Bottom spacer for FAB clearance
    Spacer(modifier = Modifier.height(56.dp))
}

@Composable
private fun AppExportToggles(
    config: AppExportConfig,
    onChanged: ((AppExportConfig) -> AppExportConfig) -> Unit,
) {
    val includePerms = config.permissionDetailLevel != PermissionDetailLevel.NONE
    val includeType = config.permissionDetailLevel >= PermissionDetailLevel.NAME_STATUS_TYPE
    val includeFull = config.permissionDetailLevel >= PermissionDetailLevel.FULL

    SwitchRow(
        label = stringResource(R.string.export_app_include_meta_label),
        description = stringResource(R.string.export_app_include_meta_description),
        checked = config.includeMetaInfo,
        onCheckedChange = { onChanged { it.copy(includeMetaInfo = !it.includeMetaInfo) } },
    )
    SwitchRow(
        label = stringResource(R.string.export_app_include_permissions_label),
        description = stringResource(R.string.export_app_include_permissions_description),
        checked = includePerms,
        onCheckedChange = {
            onChanged {
                it.copy(
                    permissionDetailLevel = if (includePerms) PermissionDetailLevel.NONE
                    else PermissionDetailLevel.NAME_AND_STATUS
                )
            }
        },
    )
    if (includePerms) {
        SwitchRow(
            label = stringResource(R.string.export_app_include_perm_type_label),
            checked = includeType,
            indent = true,
            onCheckedChange = {
                onChanged {
                    it.copy(
                        permissionDetailLevel = if (includeType) PermissionDetailLevel.NAME_AND_STATUS
                        else PermissionDetailLevel.NAME_STATUS_TYPE
                    )
                }
            },
        )
    }
    if (includeType) {
        SwitchRow(
            label = stringResource(R.string.export_app_include_perm_full_label),
            checked = includeFull,
            indent = true,
            onCheckedChange = {
                onChanged {
                    it.copy(
                        permissionDetailLevel = if (includeFull) PermissionDetailLevel.NAME_STATUS_TYPE
                        else PermissionDetailLevel.FULL
                    )
                }
            },
        )
    }
}

@Composable
private fun PermExportToggles(
    config: PermissionExportConfig,
    onChanged: ((PermissionExportConfig) -> PermissionExportConfig) -> Unit,
) {
    SwitchRow(
        label = stringResource(R.string.export_perm_include_apps_label),
        description = stringResource(R.string.export_perm_include_apps_description),
        checked = config.includeRequestingApps,
        onCheckedChange = { onChanged { it.copy(includeRequestingApps = !it.includeRequestingApps) } },
    )
    if (config.includeRequestingApps) {
        SwitchRow(
            label = stringResource(R.string.export_perm_granted_only_label),
            checked = config.grantedOnly,
            indent = true,
            onCheckedChange = { onChanged { it.copy(grantedOnly = !it.grantedOnly) } },
        )
    }
    SwitchRow(
        label = stringResource(R.string.export_perm_include_summary_label),
        checked = config.includeSummaryCounts,
        onCheckedChange = { onChanged { it.copy(includeSummaryCounts = !it.includeSummaryCounts) } },
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    indent: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 24.dp else 0.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ExportProgressContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.export_progress_label),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ExportSuccessContent(
    result: ExportViewModel.ExportResult.Success,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.export_success_message),
            style = MaterialTheme.typography.headlineSmall,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryRow(
                    label = stringResource(R.string.export_summary_filename),
                    value = result.fileName,
                )
                if (result.fileSize >= 0) {
                    SummaryRow(
                        label = stringResource(R.string.export_summary_size),
                        value = formatFileSize(result.fileSize),
                    )
                }
                SummaryRow(
                    label = stringResource(R.string.export_summary_duration),
                    value = formatDuration(result.duration),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_STREAM, result.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                },
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.export_share_action))
            }

            OutlinedButton(
                onClick = {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(result.uri, "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(viewIntent) }
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.export_open_action))
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

private fun formatDuration(duration: kotlin.time.Duration): String = when {
    duration.inWholeSeconds < 1 -> "${duration.inWholeMilliseconds} ms"
    else -> "%.1f s".format(duration.inWholeMilliseconds / 1000.0)
}

@Composable
private fun ExportErrorContent(
    error: Throwable,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(R.string.export_error_message),
            style = MaterialTheme.typography.headlineSmall,
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Text(
                text = error.localizedMessage ?: error.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp),
            )
        }
        Button(onClick = onRetry) {
            Text(stringResource(R.string.export_retry_action))
        }
    }
}

private const val FREE_EXPORT_LIMIT = 5
