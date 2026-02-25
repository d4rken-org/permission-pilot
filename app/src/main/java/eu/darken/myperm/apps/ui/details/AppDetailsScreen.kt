package eu.darken.myperm.apps.ui.details

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                state.pkg?.let { pkg ->
                                    AsyncImage(
                                        model = pkg,
                                        contentDescription = state.label,
                                        modifier = Modifier.size(64.dp),
                                    )
                                }
                                Column {
                                    Text(text = state.label, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = state.packageName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (state.versionName != null || state.versionCode != null) {
                                        Text(
                                            text = listOfNotNull(state.versionName, state.versionCode?.let { "($it)" }).joinToString(" "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.isSystemApp) {
                                Text(
                                    text = stringResource(R.string.apps_filter_systemapps_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            if (state.totalPermCount > 0) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.apps_details_description_primary_description,
                                        state.grantedCount,
                                        state.grantedCount,
                                        state.totalPermCount,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            state.updatedAt?.let { updatedAt ->
                                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                Text(
                                    text = "${stringResource(R.string.apps_sort_update_date_label)}: ${formatter.format(updatedAt.atZone(ZoneId.systemDefault()))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            state.installedAt?.let { installedAt ->
                                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                Text(
                                    text = "${stringResource(R.string.apps_sort_install_date_label)}: ${formatter.format(installedAt.atZone(ZoneId.systemDefault()))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            state.apiTargetDesc?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            state.apiMinimumDesc?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            state.apiCompileDesc?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.installerPkgNames.isNotEmpty()) {
                                Text(
                                    text = state.installerSourceLabel ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                val displayName = state.installerAppName ?: state.installerLabel ?: state.installerPkgNames.first()
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onInstallerClicked(state.installerPkgNames.first()) },
                                )
                            } else if (state.installerLabel != null) {
                                Text(
                                    text = state.installerSourceLabel ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = state.installerLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.canOpen) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = onOpenApp) {
                                    Text(text = stringResource(R.string.general_open_action))
                                }
                            }
                        }
                    }
                }

                // Twins
                if (state.twins.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.apps_details_twins_label),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(state.twins) { twin ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTwinClicked(twin) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(text = twin.label ?: twin.pkgId.pkgName)
                        }
                        HorizontalDivider()
                    }
                }

                // Siblings
                if (state.siblings.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.apps_details_siblings_label),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(state.siblings) { sibling ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSiblingClicked(sibling) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(text = sibling.label ?: sibling.pkgId.pkgName)
                        }
                        HorizontalDivider()
                    }
                }

                // Permissions header
                if (state.permissions.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.permissions_page_label),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                items(state.permissions, key = { it.permId.value }) { perm ->
                    PermissionRow(
                        item = perm,
                        onClick = { onPermClicked(perm) },
                    )
                    HorizontalDivider()
                }
            }
        }
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = item.usesPermission,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.permId.value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.permLabel ?: item.permId.value.split(".").lastOrNull() ?: item.permId.value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.isDeclaredByApp) {
                Text(
                    text = stringResource(R.string.permissions_app_type_declaring_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (item.status) {
            UsesPermission.Status.GRANTED -> Icon(
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
            else -> Text(
                text = item.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
