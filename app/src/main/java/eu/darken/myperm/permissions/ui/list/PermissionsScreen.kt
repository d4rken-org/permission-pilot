package eu.darken.myperm.permissions.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.LabeledOption
import eu.darken.myperm.common.compose.MultiChoiceFilterDialog
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.compose.SingleChoiceSortDialog
import eu.darken.myperm.common.compose.waitForState
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.known.APermGrp

@Composable
fun PermissionsScreenHost(vm: PermissionsViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by waitForState(vm.state)

    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }

    state?.let {
        PermissionsScreen(
            state = it,
            onSearchChanged = { vm.onSearchInputChanged(it) },
            onGroupClicked = { vm.toggleGroup(it) },
            onPermClicked = { vm.onPermissionClicked(it) },
            onExpandAll = { vm.expandAll() },
            onCollapseAll = { vm.collapseAll() },
            onRefresh = { vm.onRefresh() },
            onSettings = { vm.goToSettings() },
            onFilterClicked = { showFilterDialog = true },
            onSortClicked = { showSortDialog = true },
        )
    }

    val readyState = state as? PermissionsViewModel.State.Ready

    if (showFilterDialog && readyState != null) {
        MultiChoiceFilterDialog(
            title = stringResource(R.string.general_filter_action),
            options = PermsFilterOptions.Filter.entries.map { LabeledOption(it, it.labelRes) },
            selected = readyState.filterOptions.keys,
            onConfirm = { selected ->
                vm.updateFilterOptions { it.copy(keys = selected) }
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
        )
    }

    if (showSortDialog && readyState != null) {
        SingleChoiceSortDialog(
            title = stringResource(R.string.general_sort_action),
            options = PermsSortOptions.Sort.entries.map { LabeledOption(it, it.labelRes) },
            selected = readyState.sortOptions.mainSort,
            onSelect = { selected ->
                vm.updateSortOptions { it.copy(mainSort = selected) }
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    state: PermissionsViewModel.State,
    onSearchChanged: (String?) -> Unit,
    onGroupClicked: (PermissionGroup.Id) -> Unit,
    onPermClicked: (PermissionsViewModel.PermItem) -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onFilterClicked: () -> Unit,
    onSortClicked: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.permissions_page_label))
                        if (state is PermissionsViewModel.State.Ready) {
                            val ready = state as PermissionsViewModel.State.Ready
                            Text(
                                text = "${pluralStringResource(R.plurals.generic_x_groups_label, ready.countGroups, ready.countGroups)}, ${pluralStringResource(R.plurals.generic_x_items_label, ready.countPermissions, ready.countPermissions)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onFilterClicked) {
                        Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.general_filter_action))
                    }
                    IconButton(onClick = onSortClicked) {
                        Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.general_sort_action))
                    }
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.permissions_search_list_hint))
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.general_more_options_action))
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.general_expand_all_action)) },
                                onClick = { showOverflowMenu = false; onExpandAll() },
                                leadingIcon = { Icon(Icons.Filled.UnfoldMore, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.general_collapse_all_action)) },
                                onClick = { showOverflowMenu = false; onCollapseAll() },
                                leadingIcon = { Icon(Icons.Filled.UnfoldLess, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.general_refresh_action)) },
                                onClick = { showOverflowMenu = false; onRefresh() },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_page_label)) },
                                onClick = { showOverflowMenu = false; onSettings() },
                                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onSearchChanged(it.ifBlank { null })
                    },
                    placeholder = { Text(stringResource(R.string.permissions_search_list_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                )
            }

            when (state) {
                is PermissionsViewModel.State.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is PermissionsViewModel.State.Ready -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.listData) { listItem ->
                            when (listItem) {
                                is PermissionsViewModel.ListItem.Group -> PermissionGroupHeader(
                                    item = listItem.item,
                                    onClick = { onGroupClicked(listItem.item.group.id) },
                                )

                                is PermissionsViewModel.ListItem.Perm -> PermissionListItem(
                                    item = listItem.item,
                                    onClick = { onPermClicked(listItem.item) },
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionGroupHeader(
    item: PermissionsViewModel.GroupItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.isExpanded) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(item.group.iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.group.labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(item.group.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(R.plurals.generic_x_items_label, item.permCount, item.permCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (item.isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionListItem(
    item: PermissionsViewModel.PermItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = item.permission,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.id.value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Granted to ${item.grantedCount} out of ${item.requestingCount} apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val label = item.label
            if (label != null && label.lowercase() != item.id.value.lowercase()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview2
@Composable
private fun PermissionsScreenReadyPreview() = PreviewWrapper {
    PermissionsScreen(
        state = PermissionsPreviewData.readyState(),
        onSearchChanged = {},
        onGroupClicked = {},
        onPermClicked = {},
        onExpandAll = {},
        onCollapseAll = {},
        onRefresh = {},
        onSettings = {},
        onFilterClicked = {},
        onSortClicked = {},
    )
}

@Preview2
@Composable
private fun PermissionsScreenEmptyPreview() = PreviewWrapper {
    PermissionsScreen(
        state = PermissionsPreviewData.emptyReadyState(),
        onSearchChanged = {},
        onGroupClicked = {},
        onPermClicked = {},
        onExpandAll = {},
        onCollapseAll = {},
        onRefresh = {},
        onSettings = {},
        onFilterClicked = {},
        onSortClicked = {},
    )
}

@Preview2
@Composable
private fun PermissionsScreenLoadingPreview() = PreviewWrapper {
    PermissionsScreen(
        state = PermissionsViewModel.State.Loading,
        onSearchChanged = {},
        onGroupClicked = {},
        onPermClicked = {},
        onExpandAll = {},
        onCollapseAll = {},
        onRefresh = {},
        onSettings = {},
        onFilterClicked = {},
        onSortClicked = {},
    )
}

@Preview2
@Composable
private fun PermissionGroupHeaderPreview() = PreviewWrapper {
    PermissionGroupHeader(
        item = PermissionsViewModel.GroupItem(group = APermGrp.Camera, permCount = 5, isExpanded = true),
        onClick = {},
    )
}
