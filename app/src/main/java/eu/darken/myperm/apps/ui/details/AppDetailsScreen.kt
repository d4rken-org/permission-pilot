package eu.darken.myperm.apps.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.compose.AppIcon
import eu.darken.myperm.common.compose.LoadingContent
import eu.darken.myperm.common.compose.Pill
import eu.darken.myperm.common.compose.PermissionIcon
import eu.darken.myperm.common.compose.LabeledOption
import eu.darken.myperm.common.compose.MultiChoiceFilterDialog
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import androidx.compose.runtime.collectAsState
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationEventHandler
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AppDetailsScreenHost(
    route: Nav.Details.AppDetails,
    vm: AppDetailsViewModel = hiltViewModel(),
) {
    LaunchedEffect(route) { vm.init(route) }

    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()

    val effectiveState = state ?: AppDetailsViewModel.State(
        label = route.appLabel ?: route.pkgName,
        isLoading = true,
    )
    val manifestCardState by vm.manifestCardState.collectAsState()

    AppDetailsScreen(
        state = effectiveState,
        manifestCardState = manifestCardState,
        onBack = { vm.navUp() },
        onPermClicked = { vm.onPermissionClicked(it) },
        onTwinClicked = { vm.onTwinClicked(it) },
        onSiblingClicked = { vm.onSiblingClicked(it) },
        onGoSettings = { vm.onGoSettings() },
        onOpenApp = { vm.onOpenApp() },
        onFilter = { selected ->
            vm.updateFilterOptions { it.copy(filters = selected) }
        },
        onInstallerClicked = { vm.onInstallerClicked(it) },
        onManifestClicked = { vm.onManifestClicked() },
    )
}

@Composable
fun AppDetailsScreen(
    state: AppDetailsViewModel.State,
    manifestCardState: AppDetailsViewModel.ManifestCardState = AppDetailsViewModel.ManifestCardState.Queued(),
    onBack: () -> Unit,
    onPermClicked: (AppDetailsViewModel.PermItem) -> Unit,
    onTwinClicked: (AppDetailsViewModel.TwinItem) -> Unit,
    onSiblingClicked: (AppDetailsViewModel.SiblingItem) -> Unit,
    onGoSettings: () -> Unit,
    onOpenApp: () -> Unit,
    onFilter: (Set<AppDetailsFilterOptions.Filter>) -> Unit,
    onInstallerClicked: (String) -> Unit,
    onManifestClicked: () -> Unit = {},
) {
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showPermHelpDialog by rememberSaveable { mutableStateOf(false) }

    if (showPermHelpDialog) {
        PermissionHelpDialog(onDismiss = { showPermHelpDialog = false })
    }

    if (showFilterDialog) {
        MultiChoiceFilterDialog(
            title = stringResource(R.string.general_filter_action),
            options = AppDetailsFilterOptions.Filter.entries.map { LabeledOption(it, it.labelRes) },
            selected = state.filterOptions,
            onConfirm = { selected ->
                onFilter(selected)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.apps_app_details_label))
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
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.general_filter_action))
                    }
                    IconButton(onClick = onGoSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_page_label))
                    }
                },
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingContent(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                // Overview card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Identity row: icon | name + badge + package + version | open button
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                state.pkg?.let { pkg ->
                                    AppIcon(
                                        pkg = pkg,
                                        isSystemApp = state.isSystemApp,
                                        modifier = Modifier.size(56.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = state.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        if (state.isSystemApp) {
                                            Text(
                                                text = stringResource(R.string.apps_filter_systemapps_label),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.tertiaryContainer,
                                                        RoundedCornerShape(4.dp),
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = state.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (state.versionName != null || state.versionCode != null) {
                                        Text(
                                            text = listOfNotNull(state.versionName, state.versionCode?.let { "($it)" }).joinToString(" "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (state.canOpen) {
                                    IconButton(onClick = onOpenApp) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = stringResource(R.string.general_open_action),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            // Permission summary bar
                            if (state.totalPermCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                PermissionSummaryBar(
                                    grantedCount = state.grantedCount,
                                    totalCount = state.totalPermCount,
                                )
                            }

                            // Metadata grid
                            Spacer(modifier = Modifier.height(8.dp))
                            MetadataGrid(
                                state = state,
                                onInstallerClicked = onInstallerClicked,
                            )
                        }
                    }
                }

                // Manifest card
                item {
                    ManifestCard(
                        state = manifestCardState,
                        onClick = onManifestClicked,
                    )
                }

                // Twins
                if (state.twins.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.apps_details_twins_label),
                            count = state.twins.size,
                        )
                    }
                    items(state.twins) { twin ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTwinClicked(twin) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = twin.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = twin.pkgName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                    }
                }

                // Siblings
                if (state.siblings.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.apps_details_siblings_label),
                            count = state.siblings.size,
                        )
                    }
                    items(state.siblings) { sibling ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSiblingClicked(sibling) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Group,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sibling.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = sibling.pkgName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                    }
                }

                // Permissions header
                if (state.permissions.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.permissions_page_label),
                            count = state.permissions.size,
                            onHelpClicked = { showPermHelpDialog = true },
                        )
                    }
                }

                // Empty filter state
                if (state.permissions.isEmpty() && state.totalPermCount > 0) {
                    item {
                        Text(
                            text = stringResource(R.string.apps_details_empty_filter_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                        )
                    }
                }

                items(state.permissions, key = { it.permId.value }) { perm ->
                    PermissionRow(
                        item = perm,
                        onClick = { onPermClicked(perm) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ManifestCard(
    state: AppDetailsViewModel.ManifestCardState,
    onClick: () -> Unit,
) {
    val isClickable = state is AppDetailsViewModel.ManifestCardState.Loaded && state.canViewManifest
            || state is AppDetailsViewModel.ManifestCardState.Queued
            || state is AppDetailsViewModel.ManifestCardState.Analyzing

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .then(if (isClickable) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title row: icon + title + trailing indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.apps_details_manifest_label),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (state is AppDetailsViewModel.ManifestCardState.Loaded && state.hasWarning) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                when (state) {
                    is AppDetailsViewModel.ManifestCardState.Analyzing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    is AppDetailsViewModel.ManifestCardState.Loaded -> if (state.canViewManifest) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            // Content below title — full width
            when (state) {
                is AppDetailsViewModel.ManifestCardState.Queued -> {
                    val remaining = state.progress?.let { it.total - it.scanned } ?: 0
                    val progressText = if (state.progress != null && remaining > 0) {
                        pluralStringResource(R.plurals.apps_details_manifest_loading_progress, remaining, remaining)
                    } else {
                        stringResource(R.string.apps_details_manifest_queued)
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is AppDetailsViewModel.ManifestCardState.Analyzing -> {
                    Text(
                        text = stringResource(R.string.apps_details_manifest_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is AppDetailsViewModel.ManifestCardState.Loaded -> {
                    // Facts first: query counts
                    if (state.totalQueryCount > 0) {
                        val parts = buildList {
                            if (state.packageQueryCount > 0) {
                                add(
                                    pluralStringResource(
                                        R.plurals.apps_details_manifest_packages,
                                        state.packageQueryCount,
                                        state.packageQueryCount,
                                    )
                                )
                            }
                            if (state.intentQueryCount > 0) {
                                add(
                                    pluralStringResource(
                                        R.plurals.apps_details_manifest_intents,
                                        state.intentQueryCount,
                                        state.intentQueryCount,
                                    )
                                )
                            }
                            if (state.providerQueryCount > 0) {
                                add(
                                    pluralStringResource(
                                        R.plurals.apps_details_manifest_providers,
                                        state.providerQueryCount,
                                        state.providerQueryCount,
                                    )
                                )
                            }
                        }
                        Text(
                            text = parts.joinToString(" \u00B7 "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Hints below: bullet-pointed findings
                    if (state.hasActionMainQuery) {
                        Text(
                            text = "\u2022 ${stringResource(R.string.apps_details_manifest_hint_action_main)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (state.hasExcessiveQueries) {
                        Text(
                            text = "\u2022 ${stringResource(R.string.apps_details_manifest_hint_excessive)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (!state.hasWarning && state.totalQueryCount == 0) {
                        Text(
                            text = stringResource(R.string.apps_details_manifest_queries_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionSummaryBar(grantedCount: Int, totalCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = pluralStringResource(
                R.plurals.apps_details_description_primary_description,
                grantedCount,
                grantedCount,
                totalCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { if (totalCount > 0) grantedCount.toFloat() / totalCount else 0f },
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
private fun MetadataGrid(
    state: AppDetailsViewModel.State,
    onInstallerClicked: (String) -> Unit,
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Dates: 2-column
        val hasInstallDate = state.installedAt != null
        val hasUpdateDate = state.updatedAt != null
        if (hasInstallDate || hasUpdateDate) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.installedAt?.let { installedAt ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.apps_sort_install_date_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = dateFormatter.format(installedAt.atZone(ZoneId.systemDefault())),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } ?: Spacer(modifier = Modifier.weight(1f))
                state.updatedAt?.let { updatedAt ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.apps_sort_update_date_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = dateFormatter.format(updatedAt.atZone(ZoneId.systemDefault())),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } ?: Spacer(modifier = Modifier.weight(1f))
            }
        }

        // API levels: 2-column for target + minimum, third on own row
        val hasTarget = state.apiTargetDesc != null
        val hasMinimum = state.apiMinimumDesc != null
        if (hasTarget || hasMinimum) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = state.apiTargetDesc ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = state.apiMinimumDesc ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        state.apiCompileDesc?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Installer
        if (state.installerPkgNames.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.installerSourceLabel ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                val displayName = state.installerAppName ?: state.installerLabel ?: state.installerPkgNames.first()
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onInstallerClicked(state.installerPkgNames.first()) },
                )
            }
        } else if (state.installerLabel != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.installerSourceLabel ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = state.installerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int? = null, onHelpClicked: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        if (onHelpClicked != null) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onHelpClicked,
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
}


@Composable
private fun StatusIcon(status: UsesPermission.Status) {
    when (status) {
        UsesPermission.Status.GRANTED,
        UsesPermission.Status.GRANTED_IN_USE -> Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = stringResource(R.string.filter_granted_label),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )

        UsesPermission.Status.DENIED -> Icon(
            imageVector = Icons.Filled.Cancel,
            contentDescription = stringResource(R.string.filter_denied_label),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )

        UsesPermission.Status.UNKNOWN -> Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = status.name,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PermissionRow(
    item: AppDetailsViewModel.PermItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Status icon first for quick visual scanning
        StatusIcon(status = item.status)

        // Permission icon
        PermissionIcon(
            permissionId = item.permId,
            modifier = Modifier.size(20.dp),
            fallbackModel = null,
        )

        // Label, ID, type tags
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.permLabel ?: item.permId.value.split(".").lastOrNull() ?: item.permId.value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.permId.value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Type tags row
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.isRuntime) {
                    Pill(
                        text = stringResource(R.string.apps_details_perm_tag_runtime),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        compact = true,
                    )
                } else if (item.isSpecialAccess) {
                    Pill(
                        text = stringResource(R.string.apps_details_perm_tag_special),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        compact = true,
                    )
                } else {
                    Pill(
                        text = stringResource(R.string.apps_details_perm_tag_install),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        compact = true,
                    )
                }
                if (item.isDeclaredByApp) {
                    val declaredDesc = stringResource(R.string.permissions_app_type_declaring_description)
                    Icon(
                        imageVector = Icons.Filled.NewReleases,
                        contentDescription = declaredDesc,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .size(14.dp)
                            .semantics { contentDescription = declaredDesc },
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.apps_details_perm_help_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Pill(
                        text = stringResource(R.string.apps_details_perm_tag_runtime),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        compact = true,
                    )
                    Text(
                        text = stringResource(R.string.apps_details_perm_help_runtime),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Pill(
                        text = stringResource(R.string.apps_details_perm_tag_special),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        compact = true,
                    )
                    Text(
                        text = stringResource(R.string.apps_details_perm_help_special),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Pill(
                        text = stringResource(R.string.apps_details_perm_tag_install),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        compact = true,
                    )
                    Text(
                        text = stringResource(R.string.apps_details_perm_help_install),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.NewReleases,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.apps_details_perm_help_declared),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.general_done_action))
            }
        },
    )
}

@Preview2
@Composable
private fun AppDetailsScreenPreview() = PreviewWrapper {
    AppDetailsScreen(
        state = AppDetailsPreviewData.loadedState(),
        onBack = {},
        onPermClicked = {},
        onTwinClicked = {},
        onSiblingClicked = {},
        onGoSettings = {},
        onOpenApp = {},
        onFilter = {},
        onInstallerClicked = {},
        onManifestClicked = {},
    )
}

@Preview2
@Composable
private fun AppDetailsScreenLoadingPreview() = PreviewWrapper {
    AppDetailsScreen(
        state = AppDetailsPreviewData.loadingState(),
        onBack = {},
        onPermClicked = {},
        onTwinClicked = {},
        onSiblingClicked = {},
        onGoSettings = {},
        onOpenApp = {},
        onFilter = {},
        onInstallerClicked = {},
        onManifestClicked = {},
    )
}

@Preview2
@Composable
private fun AppDetailsScreenSystemAppPreview() = PreviewWrapper {
    AppDetailsScreen(
        state = AppDetailsPreviewData.systemAppState(),
        manifestCardState = AppDetailsPreviewData.manifestLoadedState(),
        onBack = {},
        onPermClicked = {},
        onTwinClicked = {},
        onSiblingClicked = {},
        onGoSettings = {},
        onOpenApp = {},
        onFilter = {},
        onInstallerClicked = {},
        onManifestClicked = {},
    )
}

@Preview2
@Composable
private fun AppDetailsScreenEmptyFilterPreview() = PreviewWrapper {
    AppDetailsScreen(
        state = AppDetailsPreviewData.emptyFilterState(),
        onBack = {},
        onPermClicked = {},
        onTwinClicked = {},
        onSiblingClicked = {},
        onGoSettings = {},
        onOpenApp = {},
        onFilter = {},
        onInstallerClicked = {},
        onManifestClicked = {},
    )
}

@Preview2
@Composable
private fun PermissionHelpDialogPreview() = PreviewWrapper {
    PermissionHelpDialog(onDismiss = {})
}
