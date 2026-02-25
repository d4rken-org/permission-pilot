package eu.darken.myperm.permissions.ui.details

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import eu.darken.myperm.common.compose.LabeledOption
import eu.darken.myperm.common.compose.MultiChoiceFilterDialog
import eu.darken.myperm.common.compose.waitForState
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationEventHandler

@Composable
fun PermissionDetailsScreenHost(
    route: Nav.Details.PermissionDetails,
    vm: PermissionDetailsViewModel = hiltViewModel(),
) {
    LaunchedEffect(route) { vm.init(route) }

    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by waitForState(vm.state)

    var showFilterDialog by rememberSaveable { mutableStateOf(false) }

    state?.let {
        PermissionDetailsScreen(
            state = it,
            onBack = { vm.navUp() },
            onAppClicked = { pkgName, userHandle -> vm.onAppClicked(pkgName, userHandle) },
            onFilterClicked = { showFilterDialog = true },
        )
    }

    if (showFilterDialog && state != null) {
        MultiChoiceFilterDialog(
            title = stringResource(R.string.general_filter_action),
            options = PermissionDetailsFilterOptions.Filter.entries.map { LabeledOption(it, it.labelRes) },
            selected = state!!.filterOptions.keys,
            onConfirm = { selected ->
                vm.updateFilterOptions { it.copy(keys = selected) }
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDetailsScreen(
    state: PermissionDetailsViewModel.State,
    onBack: () -> Unit,
    onAppClicked: (String, Int) -> Unit,
    onFilterClicked: () -> Unit,
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
                                state.permission?.let { perm ->
                                    AsyncImage(
                                        model = perm,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                    )
                                }
                                Column {
                                    state.description?.let {
                                        Text(text = it, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Text(
                                        text = state.permissionId,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            state.fullDescription?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            state.protectionLevel?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.permissions_details_protection_label),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.totalUserCount > 0 || state.totalSystemCount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                if (state.totalUserCount > 0) {
                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.permissions_details_count_user_apps,
                                            state.totalUserCount,
                                            state.grantedUserCount,
                                            state.totalUserCount,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (state.totalSystemCount > 0) {
                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.permissions_details_count_system_apps,
                                            state.totalSystemCount,
                                            state.grantedSystemCount,
                                            state.totalSystemCount,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }

                // Declaring apps
                if (state.declaringApps.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.permissions_details_declaring_apps_label),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(state.declaringApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClicked(app.pkgName, app.userHandle) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AsyncImage(
                                model = app.pkg,
                                contentDescription = app.label,
                                modifier = Modifier.size(32.dp),
                            )
                            Column {
                                Text(
                                    text = app.label ?: app.pkgName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = app.pkgName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(R.string.permissions_app_type_declaring_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }

                // Requesting apps
                if (state.requestingApps.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.permissions_details_requesting_apps_label),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(state.requestingApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClicked(app.pkgName, app.userHandle) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AsyncImage(
                                model = app.pkg,
                                contentDescription = app.label,
                                modifier = Modifier.size(32.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label ?: app.pkgName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = app.pkgName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (app.isGranted) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = stringResource(R.string.filter_granted_label),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = stringResource(R.string.filter_denied_label),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
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
