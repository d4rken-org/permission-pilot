package eu.darken.myperm.apps.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
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

@Composable
fun AppsScreenHost(vm: AppsViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by waitForState(vm.state)
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }

    state?.let {
        AppsScreen(
            state = it,
            onSearchChanged = { vm.onSearchInputChanged(it) },
            onAppClicked = { vm.onAppClicked(it) },
            onFilter = { showFilterDialog = true },
            onSort = { showSortDialog = true },
            onRefresh = { vm.onRefresh() },
            onSettings = { vm.goToSettings() },
        )
    }

    val readyState = state as? AppsViewModel.State.Ready

    if (showFilterDialog && readyState != null) {
        MultiChoiceFilterDialog(
            title = stringResource(R.string.general_filter_action),
            options = AppsFilterOptions.Filter.entries.map { LabeledOption(it, it.labelRes) },
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
            options = AppsSortOptions.Sort.entries.map { LabeledOption(it, it.labelRes) },
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
fun AppsScreen(
    state: AppsViewModel.State,
    onSearchChanged: (String?) -> Unit,
    onAppClicked: (AppsViewModel.AppItem) -> Unit,
    onFilter: () -> Unit,
    onSort: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.apps_page_label))
                        if (state is AppsViewModel.State.Ready) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.generic_x_items_label,
                                    (state as AppsViewModel.State.Ready).itemCount,
                                    (state as AppsViewModel.State.Ready).itemCount,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onFilter) {
                        Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.general_filter_action))
                    }
                    IconButton(onClick = onSort) {
                        Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.general_sort_action))
                    }
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.apps_search_list_hint))
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
                    placeholder = { Text(stringResource(R.string.apps_search_list_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                )
            }

            when (state) {
                is AppsViewModel.State.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is AppsViewModel.State.Ready -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.items, key = { "${it.id.pkgName}:${it.userHandle}" }) { item ->
                            AppListItem(
                                item = item,
                                onClick = { onAppClicked(item) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    item: AppsViewModel.AppItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = item.pkg,
            contentDescription = item.label,
            modifier = Modifier.size(40.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.label ?: item.packageName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val grantedText = if (item.totalCount > 0) {
                pluralStringResource(R.plurals.apps_permissions_x_of_x_granted, item.grantedCount, item.grantedCount, item.totalCount)
            } else null
            val declaresText = if (item.declaredCount > 0) {
                pluralStringResource(R.plurals.apps_permissions_declares_x, item.declaredCount, item.declaredCount)
            } else null
            val subtitle = listOfNotNull(grantedText, declaresText).joinToString(" ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.tagIconRes.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    item.tagIconRes.forEach { iconRes ->
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (item.installerInfo?.installer != null) {
            AsyncImage(
                model = item.installerInfo.installer,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview2
@Composable
private fun AppsScreenReadyPreview() = PreviewWrapper {
    AppsScreen(
        state = AppsPreviewData.readyState(),
        onSearchChanged = {},
        onAppClicked = {},
        onFilter = {},
        onSort = {},
        onRefresh = {},
        onSettings = {},
    )
}

@Preview2
@Composable
private fun AppsScreenEmptyPreview() = PreviewWrapper {
    AppsScreen(
        state = AppsPreviewData.emptyReadyState(),
        onSearchChanged = {},
        onAppClicked = {},
        onFilter = {},
        onSort = {},
        onRefresh = {},
        onSettings = {},
    )
}

@Preview2
@Composable
private fun AppsScreenLoadingPreview() = PreviewWrapper {
    AppsScreen(
        state = AppsViewModel.State.Loading,
        onSearchChanged = {},
        onAppClicked = {},
        onFilter = {},
        onSort = {},
        onRefresh = {},
        onSettings = {},
    )
}
