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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.compose.LabeledOption
import eu.darken.myperm.common.compose.MultiChoiceFilterDialog
import eu.darken.myperm.common.compose.waitForState
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

    val state by waitForState(vm.state)

    state?.let {
        AppDetailsScreen(
            state = it,
            onBack = { vm.navUp() },
            onPermClicked = { vm.onPermissionClicked(it) },
            onTwinClicked = { vm.onTwinClicked(it) },
            onSiblingClicked = { vm.onSiblingClicked(it) },
            onGoSettings = { vm.onGoSettings() },
            onOpenApp = { vm.onOpenApp() },
            onFilter = { selected ->
                vm.updateFilterOptions { it.copy(keys = selected) }
            },
            onInstallerClicked = { vm.onInstallerClicked(it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    state: AppDetailsViewModel.State,
    onBack: () -> Unit,
    onPermClicked: (AppDetailsViewModel.PermItem) -> Unit,
    onTwinClicked: (AppDetailsViewModel.TwinItem) -> Unit,
    onSiblingClicked: (AppDetailsViewModel.SiblingItem) -> Unit,
    onGoSettings: () -> Unit,
    onOpenApp: () -> Unit,
    onFilter: (Set<AppDetailsFilterOptions.Filter>) -> Unit,
    onInstallerClicked: (String) -> Unit,
) {
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }

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
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
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
                                    AsyncImage(
                                        model = pkg,
                                        contentDescription = state.label,
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
                                    text = twin.label ?: twin.pkgId.pkgName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (twin.label != null) {
                                    Text(
                                        text = twin.pkgId.pkgName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
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
                                    text = sibling.label ?: sibling.pkgId.pkgName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (sibling.label != null) {
                                    Text(
                                        text = sibling.pkgId.pkgName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
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
private fun SectionHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
    }
}

@Composable
private fun PermTypeTag(text: String, containerColor: Color, contentColor: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = contentColor,
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
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
        AsyncImage(
            model = item.usesPermission,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
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
                    PermTypeTag(
                        text = "Runtime",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else if (item.isSpecialAccess) {
                    PermTypeTag(
                        text = "Special",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    PermTypeTag(
                        text = "Install",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
