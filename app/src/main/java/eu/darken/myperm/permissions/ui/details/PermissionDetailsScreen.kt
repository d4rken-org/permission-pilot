package eu.darken.myperm.permissions.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.compose.AppIcon
import eu.darken.myperm.common.compose.LabeledOption
import eu.darken.myperm.common.compose.LoadingContent
import eu.darken.myperm.common.compose.MultiChoiceFilterDialog
import eu.darken.myperm.common.compose.GrantStatusPill
import eu.darken.myperm.common.compose.PermissionIcon
import eu.darken.myperm.common.compose.PermissionTagPill
import eu.darken.myperm.common.compose.Pill
import eu.darken.myperm.common.compose.SystemPill
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.permissions.core.ProtectionFlag
import eu.darken.myperm.permissions.core.ProtectionType

@Composable
fun PermissionDetailsScreenHost(
    route: Nav.Details.PermissionDetails,
    vm: PermissionDetailsViewModel = hiltViewModel(),
) {
    LaunchedEffect(route) { vm.init(route) }

    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()

    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showPermissionHelpDialog by rememberSaveable { mutableStateOf(false) }
    var showStatusHelpDialog by rememberSaveable { mutableStateOf(false) }

    state?.let {
        PermissionDetailsScreen(
            state = it,
            onBack = { vm.navUp() },
            onAppClicked = { pkgName, userHandle -> vm.onAppClicked(pkgName, userHandle) },
            onFilterClicked = { showFilterDialog = true },
            onPermissionHelpClicked = { showPermissionHelpDialog = true },
            onStatusHelpClicked = { showStatusHelpDialog = true },
        )
    }

    if (showPermissionHelpDialog) {
        PermissionInfoHelpDialog(onDismiss = { showPermissionHelpDialog = false })
    }

    if (showStatusHelpDialog) {
        GrantStatusHelpDialog(onDismiss = { showStatusHelpDialog = false })
    }

    if (showFilterDialog && state != null) {
        MultiChoiceFilterDialog(
            title = stringResource(R.string.general_filter_action),
            options = PermissionDetailsFilterOptions.Filter.entries.map { LabeledOption(it, it.labelRes) },
            selected = state!!.filterOptions.filters,
            onConfirm = { selected ->
                vm.updateFilterOptions { it.copy(filters = selected) }
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
        )
    }
}

@Composable
fun PermissionDetailsScreen(
    state: PermissionDetailsViewModel.State,
    onBack: () -> Unit,
    onAppClicked: (String, Int) -> Unit,
    onFilterClicked: () -> Unit,
    onPermissionHelpClicked: () -> Unit,
    onStatusHelpClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.permissions_permission_details_label))
                        Text(
                            text = state.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onFilterClicked) {
                        Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.general_filter_action))
                    }
                },
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingContent(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // Overview card
                item(key = "overview") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PermissionIcon(
                                    permissionId = state.permission?.id ?: eu.darken.myperm.permissions.core.Permission.Id(state.permissionId),
                                    modifier = Modifier.size(48.dp),
                                    fallbackModel = state.permission,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.description ?: state.label,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = state.permissionId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(
                                    onClick = onPermissionHelpClicked,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = stringResource(R.string.label_help),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (state.protectionType != null || state.protectionFlags.isNotEmpty() || state.tags.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    state.protectionType?.let { ProtectionTypePill(it) }
                                    state.protectionFlags.forEach { ProtectionFlagPill(it) }
                                    if (state.protectionFlagOverflow > 0) {
                                        Pill(
                                            text = pluralStringResource(R.plurals.permissions_details_flags_more_label, state.protectionFlagOverflow, state.protectionFlagOverflow),
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    state.tags.forEach { PermissionTagPill(it) }
                                }
                            }

                            state.fullDescription?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (state.totalUserCount > 0) {
                                GrantRatioRow(
                                    label = pluralStringResource(
                                        R.plurals.generic_x_apps_user_label,
                                        state.totalUserCount,
                                        state.totalUserCount,
                                    ),
                                    granted = state.grantedUserCount,
                                    total = state.totalUserCount,
                                )
                            }
                            if (state.totalSystemCount > 0) {
                                GrantRatioRow(
                                    label = pluralStringResource(
                                        R.plurals.generic_x_apps_system_label,
                                        state.totalSystemCount,
                                        state.totalSystemCount,
                                    ),
                                    granted = state.grantedSystemCount,
                                    total = state.totalSystemCount,
                                )
                            }
                            if (state.otherProfileCount > 0) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.permissions_details_count_other_profile_apps,
                                        state.otherProfileCount,
                                        state.otherProfileCount,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.uninstalledCount > 0) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.permissions_details_count_uninstalled_apps,
                                        state.uninstalledCount,
                                        state.uninstalledCount,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.otherProfileCount > 0 || state.uninstalledCount > 0) {
                                Text(
                                    text = stringResource(R.string.permissions_details_description_restrictions_caveat_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Declaring apps
                if (state.declaringApps.isNotEmpty()) {
                    item(key = "decl_header") {
                        Text(
                            text = stringResource(R.string.permissions_details_declaring_apps_label),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(state.declaringApps, key = { "decl:${it.pkgName}:${it.userHandle}" }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClicked(app.pkgName, app.userHandle) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AppIcon(
                                pkg = app.pkg,
                                isSystemApp = app.isSystemApp,
                                modifier = Modifier.size(32.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = app.label ?: app.pkgName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (app.isSystemApp) {
                                        SystemPill()
                                    }
                                }
                                Text(
                                    text = app.pkgName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }

                // Requesting apps
                if (state.requestingApps.isNotEmpty()) {
                    item(key = "req_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.permissions_details_requesting_apps_label),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = onStatusHelpClicked,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = stringResource(R.string.label_help),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    items(state.requestingApps, key = { "req:${it.pkgName}:${it.userHandle}" }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClicked(app.pkgName, app.userHandle) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AppIcon(
                                pkg = app.pkg,
                                isSystemApp = app.isSystemApp,
                                modifier = Modifier.size(32.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = app.label ?: app.pkgName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (app.isSystemApp) {
                                        SystemPill()
                                    }
                                    GrantStatusPill(app.status)
                                }
                                Text(
                                    text = app.pkgName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun GrantRatioRow(label: String, granted: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$granted / $total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { if (total > 0) granted.toFloat() / total else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun ProtectionTypePill(type: ProtectionType) {
    val colors = when (type) {
        ProtectionType.DANGEROUS -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ProtectionType.SIGNATURE,
        ProtectionType.SIGNATURE_OR_SYSTEM,
        ProtectionType.INTERNAL -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        ProtectionType.NORMAL -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ProtectionType.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Pill(
        text = stringResource(type.labelRes),
        containerColor = colors.first,
        contentColor = colors.second,
    )
}

@Composable
private fun ProtectionFlagPill(flag: ProtectionFlag) {
    Pill(
        text = flag.name,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PermissionInfoHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permissions_details_help_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Protection type section
                HelpSectionHeader(stringResource(R.string.permissions_details_help_section_protection))
                HelpRow(
                    pill = stringResource(R.string.permissions_protection_type_dangerous_label),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    description = stringResource(R.string.permissions_details_help_dangerous),
                )
                HelpRow(
                    pill = stringResource(R.string.permissions_protection_type_normal_label),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    description = stringResource(R.string.permissions_details_help_normal),
                )
                HelpRow(
                    pill = stringResource(R.string.permissions_protection_type_signature_label),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    description = stringResource(R.string.permissions_details_help_signature),
                )
                HelpRow(
                    pill = stringResource(R.string.permissions_protection_type_internal_label),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    description = stringResource(R.string.permissions_details_help_internal),
                )

                // Tags section
                HelpSectionHeader(stringResource(R.string.permissions_details_help_section_tags))
                HelpRow(
                    pill = stringResource(R.string.permissions_tag_runtime_label),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    description = stringResource(R.string.permissions_details_help_tag_runtime),
                )
                HelpRow(
                    pill = stringResource(R.string.permissions_tag_install_time_label),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    description = stringResource(R.string.permissions_details_help_tag_install_time),
                )
                HelpRow(
                    pill = stringResource(R.string.permissions_tag_special_access_label),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    description = stringResource(R.string.permissions_details_help_tag_special_access),
                )
                HelpRow(
                    pill = stringResource(R.string.permissions_tag_documented_label),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    description = stringResource(R.string.permissions_details_help_tag_documented),
                )
                HelpRow(
                    pill = stringResource(R.string.permissions_tag_notable_label),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    description = stringResource(R.string.permissions_details_help_tag_notable),
                )
                HelpRow(
                    pill = stringResource(R.string.permissions_tag_non_standard_label),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    description = stringResource(R.string.permissions_details_help_tag_non_standard),
                )

                // Badges section
                HelpSectionHeader(stringResource(R.string.permissions_details_help_section_badges))
                HelpRow(
                    pill = stringResource(R.string.permissions_details_system_badge_label),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    description = stringResource(R.string.permissions_details_help_system_badge),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.general_done_action))
            }
        },
    )
}

@Composable
private fun GrantStatusHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permissions_details_status_help_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HelpRow(
                    pill = stringResource(R.string.filter_granted_label),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    description = stringResource(R.string.permissions_details_help_status_granted),
                )
                HelpRow(
                    pill = stringResource(R.string.permissions_status_granted_in_use_label),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    description = stringResource(R.string.permissions_details_help_status_in_use),
                )
                HelpRow(
                    pill = stringResource(R.string.filter_denied_label),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    description = stringResource(R.string.permissions_details_help_status_denied),
                )
                HelpRow(
                    pill = stringResource(R.string.permissions_status_unknown_label),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    description = stringResource(R.string.permissions_details_help_status_unknown),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.general_done_action))
            }
        },
    )
}

@Composable
private fun HelpSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun HelpRow(
    pill: String,
    containerColor: Color,
    contentColor: Color,
    description: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Pill(text = pill, containerColor = containerColor, contentColor = contentColor)
        Text(text = description, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview2
@Composable
private fun PermissionDetailsScreenPreview() = PreviewWrapper {
    PermissionDetailsScreen(
        state = PermissionDetailsPreviewData.loadedState(),
        onBack = {},
        onAppClicked = { _, _ -> },
        onFilterClicked = {},
        onPermissionHelpClicked = {},
        onStatusHelpClicked = {},
    )
}

@Preview2
@Composable
private fun PermissionDetailsScreenLoadingPreview() = PreviewWrapper {
    PermissionDetailsScreen(
        state = PermissionDetailsPreviewData.loadingState(),
        onBack = {},
        onAppClicked = { _, _ -> },
        onFilterClicked = {},
        onPermissionHelpClicked = {},
        onStatusHelpClicked = {},
    )
}
