package eu.darken.myperm.apps.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import eu.darken.myperm.common.compose.LoadingContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.twotone.Android
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import coil.compose.AsyncImage
import eu.darken.myperm.apps.core.Pkg
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.AppIcon
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.compose.SearchTextField
import eu.darken.myperm.common.compose.rememberFabVisibility
import androidx.compose.runtime.collectAsState
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler

@Composable
fun AppsScreenHost(vm: AppsViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    var showFilterSortSheet by rememberSaveable { mutableStateOf(false) }
    var showTagHelpDialog by rememberSaveable { mutableStateOf(false) }

    val effectiveState = state ?: AppsViewModel.State.Loading
    val readyState = state as? AppsViewModel.State.Ready
    val hasActiveFilters = readyState != null
            && (readyState.filterOptions != AppsFilterOptions() || readyState.sortOptions != AppsSortOptions())

    AppsScreen(
        state = effectiveState,
        isRefreshing = isRefreshing,
        hasActiveFilters = hasActiveFilters,
        onSearchChanged = { vm.onSearchInputChanged(it) },
        onAppClicked = { vm.onAppClicked(it) },
        onFilter = { showFilterSortSheet = true },
        onRefresh = { vm.onRefresh() },
        onSettings = { vm.goToSettings() },
    )

    if (showFilterSortSheet && readyState != null) {
        AppsFilterSortBottomSheet(
            currentFilterOptions = readyState.filterOptions,
            currentSortOptions = readyState.sortOptions,
            onOptionsChanged = { filter, sort -> vm.updateOptions { _, _ -> filter to sort } },
            onDismiss = { showFilterSortSheet = false },
            onHelpClicked = { showTagHelpDialog = true },
        )
    }

    if (showTagHelpDialog) {
        AppTagHelpDialog(onDismiss = { showTagHelpDialog = false })
    }
}

@Composable
fun AppsScreen(
    state: AppsViewModel.State,
    isRefreshing: Boolean = false,
    hasActiveFilters: Boolean = false,
    onSearchChanged: (String?) -> Unit,
    onAppClicked: (AppsViewModel.AppItem) -> Unit,
    onFilter: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    val (fabVisible, scrollConnection) = rememberFabVisibility()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollConnection),
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = { if (!isRefreshing) onRefresh() },
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.general_refresh_action))
                    }
                }
            }
        },
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
                    IconButton(onClick = {
                        if (isSearchActive) {
                            searchQuery = ""
                            onSearchChanged(null)
                        }
                        isSearchActive = !isSearchActive
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.apps_search_list_hint))
                    }
                    if (state is AppsViewModel.State.Ready) {
                        IconButton(onClick = onFilter) {
                            Box {
                                Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.apps_filter_sort_action))
                                if (hasActiveFilters) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    )
                                }
                            }
                        }
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
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SearchTextField(
                    query = searchQuery,
                    onQueryChanged = {
                        searchQuery = it
                        onSearchChanged(it.ifBlank { null })
                    },
                    placeholder = stringResource(R.string.apps_search_list_hint),
                )
            }

            when (state) {
                is AppsViewModel.State.Loading -> {
                    LoadingContent()
                }

                is AppsViewModel.State.Ready -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.items, key = { "${it.pkgName}:${it.userHandleId}" }) { item ->
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
    val density = LocalDensity.current
    var textBlockHeight by remember { mutableIntStateOf(0) }
    val iconSize = with(density) { if (textBlockHeight > 0) textBlockHeight.toDp() else 40.dp }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(
            pkg = item.iconModel,
            isSystemApp = item.isSystemApp,
            modifier = Modifier.size(iconSize),
        )

        Column(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.onSizeChanged { textBlockHeight = it.height }) {
                if (item.showPkgName) {
                    Text(
                        text = item.pkgName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = item.label,
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
            }
            if (item.tagIcons.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    item.tagIcons.forEach { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (item.installerPkgName != null) {
            AsyncImage(
                model = Pkg.Container(Pkg.Id(item.installerPkgName)),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                error = rememberVectorPainter(Icons.TwoTone.Android),
                fallback = rememberVectorPainter(Icons.TwoTone.Android),
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
        onRefresh = {},
        onSettings = {},
    )
}
