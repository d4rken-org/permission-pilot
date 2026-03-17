package eu.darken.myperm.permissions.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.ui.state.ToggleableState
import androidx.compose.material3.AlertDialog
import eu.darken.myperm.common.compose.LoadingContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.Pill
import eu.darken.myperm.common.compose.PermissionIcon
import eu.darken.myperm.common.compose.PermissionTagPill
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.compose.SearchTextField
import eu.darken.myperm.common.compose.rememberFabVisibility
import eu.darken.myperm.common.compose.icon
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.features.InstallTimeGrant
import eu.darken.myperm.permissions.core.features.PermissionTag
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import eu.darken.myperm.permissions.core.known.APermGrp

@Composable
fun PermissionsScreenHost(vm: PermissionsViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    var showFilterSortSheet by rememberSaveable { mutableStateOf(false) }
    var showTagHelpDialog by rememberSaveable { mutableStateOf(false) }

    val effectiveState = state ?: PermissionsViewModel.State.Loading
    val readyState = effectiveState as? PermissionsViewModel.State.Ready
    val hasActiveFilters = readyState != null
            && (readyState.filterOptions != PermsFilterOptions() || readyState.sortOptions != PermsSortOptions())

    PermissionsScreen(
        state = effectiveState,
        isRefreshing = isRefreshing,
        hasActiveFilters = hasActiveFilters,
        onSearchChanged = { vm.onSearchInputChanged(it) },
        onGroupClicked = { vm.toggleGroup(it) },
        onGroupLongPressed = { vm.onGroupLongPressed(it) },
        onPermClicked = { vm.onPermissionClicked(it) },
        onPermLongPressed = { vm.onPermissionLongPressed(it) },
        onExpandAll = { vm.expandAll() },
        onCollapseAll = { vm.collapseAll() },
        onRefresh = { vm.onRefresh() },
        onSettings = { vm.goToSettings() },
        onFilter = { showFilterSortSheet = true },
        onSelectAll = { vm.selectAllPermissions() },
        onClearSelection = { vm.clearPermissionSelection() },
        onExportSelected = { vm.onExportSelectedPermissions() },
    )

    if (showFilterSortSheet && readyState != null) {
        PermsFilterSortBottomSheet(
            currentFilterOptions = readyState.filterOptions,
            currentSortOptions = readyState.sortOptions,
            onOptionsChanged = { filter, sort -> vm.updateOptions { _, _ -> filter to sort } },
            onDismiss = { showFilterSortSheet = false },
            onHelpClicked = { showTagHelpDialog = true },
        )
    }

    if (showTagHelpDialog) {
        PermissionTagHelpDialog(onDismiss = { showTagHelpDialog = false })
    }
}

@Composable
fun PermissionsScreen(
    state: PermissionsViewModel.State,
    isRefreshing: Boolean = false,
    hasActiveFilters: Boolean = false,
    onSearchChanged: (String?) -> Unit,
    onGroupClicked: (PermissionGroup.Id) -> Unit,
    onGroupLongPressed: (PermissionsViewModel.GroupItem) -> Unit = {},
    onPermClicked: (PermissionsViewModel.PermItem) -> Unit,
    onPermLongPressed: (PermissionsViewModel.PermItem) -> Unit = {},
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onFilter: () -> Unit,
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onExportSelected: () -> Unit = {},
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    val (fabVisible, scrollConnection) = rememberFabVisibility()

    val selection = (state as? PermissionsViewModel.State.Ready)?.selection ?: emptySet()
    val isSelecting = selection.isNotEmpty()

    BackHandler(enabled = isSelecting) { onClearSelection() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollConnection),
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible && !isSelecting,
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
            if (isSelecting) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.general_close_action))
                        }
                    },
                    title = {
                        Text(
                            text = pluralStringResource(R.plurals.general_x_selected_label, selection.size, selection.size),
                        )
                    },
                    actions = {
                        IconButton(onClick = onSelectAll) {
                            Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.general_select_all_action))
                        }
                        IconButton(onClick = onExportSelected) {
                            Icon(Icons.Filled.Description, contentDescription = stringResource(R.string.export_permission_info_action))
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(text = stringResource(R.string.permissions_page_label))
                            if (state is PermissionsViewModel.State.Ready) {
                                Text(
                                    text = "${pluralStringResource(R.plurals.generic_x_groups_label, state.countGroups, state.countGroups)}, ${pluralStringResource(R.plurals.generic_x_items_label, state.countPermissions, state.countPermissions)}",
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
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.permissions_search_list_hint))
                        }
                        if (state is PermissionsViewModel.State.Ready) {
                            IconButton(onClick = onFilter) {
                                Box {
                                    Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.permissions_filter_sort_action))
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
                                val hasAnyExpanded = (state as? PermissionsViewModel.State.Ready)
                                    ?.listData
                                    ?.any { it is PermissionsViewModel.ListItem.Group && it.item.isExpanded }
                                    ?: false
                                if (hasAnyExpanded) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.general_collapse_all_action)) },
                                        onClick = { showOverflowMenu = false; onCollapseAll() },
                                        leadingIcon = { Icon(Icons.Filled.UnfoldLess, contentDescription = null) },
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.general_expand_all_action)) },
                                        onClick = { showOverflowMenu = false; onExpandAll() },
                                        leadingIcon = { Icon(Icons.Filled.UnfoldMore, contentDescription = null) },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_page_label)) },
                                    onClick = { showOverflowMenu = false; onSettings() },
                                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            AnimatedVisibility(
                visible = isSearchActive && !isSelecting,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SearchTextField(
                    query = searchQuery,
                    onQueryChanged = {
                        searchQuery = it
                        onSearchChanged(it.ifBlank { null })
                    },
                    placeholder = stringResource(R.string.permissions_search_list_hint),
                )
            }

            when (state) {
                is PermissionsViewModel.State.Loading -> {
                    LoadingContent()
                }

                is PermissionsViewModel.State.Ready -> {
                    if (state.listData.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.permissions_list_empty_search_message)
                                } else {
                                    stringResource(R.string.permissions_list_empty_message)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(32.dp),
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(
                                items = state.listData,
                                key = { _, item ->
                                    when (item) {
                                        is PermissionsViewModel.ListItem.Group -> "group_${item.item.group.id.value}"
                                        is PermissionsViewModel.ListItem.Perm -> "perm_${item.item.id.value}"
                                    }
                                }
                            ) { index, listItem ->
                                when (listItem) {
                                    is PermissionsViewModel.ListItem.Group -> {
                                        if (index > 0) {
                                            val prevItem = state.listData[index - 1]
                                            if (prevItem is PermissionsViewModel.ListItem.Group ||
                                                prevItem is PermissionsViewModel.ListItem.Perm
                                            ) {
                                                HorizontalDivider()
                                            }
                                        }
                                        val groupPermIds = (state as? PermissionsViewModel.State.Ready)
                                            ?.groupPerms?.get(listItem.item.group.id)
                                            ?: emptyList()
                                        val selectedInGroup = groupPermIds.count { it in selection }
                                        val groupSelectionState = when {
                                            selectedInGroup == 0 -> ToggleableState.Off
                                            selectedInGroup == groupPermIds.size -> ToggleableState.On
                                            else -> ToggleableState.Indeterminate
                                        }
                                        PermissionGroupHeader(
                                            item = listItem.item,
                                            isSelecting = isSelecting,
                                            groupSelectionState = groupSelectionState,
                                            onClick = { onGroupClicked(listItem.item.group.id) },
                                            onLongClick = { onGroupLongPressed(listItem.item) },
                                        )
                                    }

                                    is PermissionsViewModel.ListItem.Perm -> {
                                        if (index > 0 && state.listData[index - 1] is PermissionsViewModel.ListItem.Perm) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 48.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                            )
                                        }
                                        PermissionListItem(
                                            item = listItem.item,
                                            isSelecting = isSelecting,
                                            isSelected = listItem.item.id in selection,
                                            onClick = { onPermClicked(listItem.item) },
                                            onLongClick = { onPermLongPressed(listItem.item) },
                                        )
                                    }
                                }
                            }
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
    isSelecting: Boolean = false,
    groupSelectionState: ToggleableState = ToggleableState.Off,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
            if (isSelecting) {
                TriStateCheckbox(
                    state = groupSelectionState,
                    onClick = onLongClick,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Icon(
                    imageVector = item.group.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(item.group.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = item.permCount.toString(),
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
                Text(
                    text = stringResource(item.group.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = if (item.isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (item.isExpanded) {
                    stringResource(R.string.general_collapse_all_action)
                } else {
                    stringResource(R.string.general_expand_all_action)
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionListItem(
    item: PermissionsViewModel.PermItem,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent connector bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isSelecting) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                PermissionIcon(
                    permissionId = item.permission.id,
                    modifier = Modifier.size(24.dp),
                    fallbackModel = item.permission,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                // Label + type pill row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.label ?: item.id.value.substringAfterLast('.'),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    val displayTag = pickDisplayTag(item.permission.tags)
                    if (displayTag != null) {
                        PermissionTagPill(tag = displayTag, compact = true)
                    }
                }
                // Permission ID
                Text(
                    text = item.id.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Grant ratio row
                val progress = if (item.requestingCount > 0) {
                    (item.grantedCount.toFloat() / item.requestingCount).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val ratioDescription = "${item.grantedCount} of ${item.requestingCount} granted"
                Row(
                    modifier = Modifier.semantics { contentDescription = ratioDescription },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        text = "${item.grantedCount}/${item.requestingCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun pickDisplayTag(tags: Collection<PermissionTag>): PermissionTag? = when {
    tags.any { it is RuntimeGrant } -> RuntimeGrant
    tags.any { it is SpecialAccess } -> SpecialAccess
    tags.any { it is InstallTimeGrant } -> InstallTimeGrant
    else -> null
}

@Composable
internal fun PermissionTagHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permissions_details_help_section_tags)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TagHelpRow(
                    pill = stringResource(R.string.permissions_tag_runtime_label),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    description = stringResource(R.string.permissions_details_help_tag_runtime),
                )
                TagHelpRow(
                    pill = stringResource(R.string.permissions_tag_special_access_label),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    description = stringResource(R.string.permissions_details_help_tag_special_access),
                )
                TagHelpRow(
                    pill = stringResource(R.string.permissions_tag_install_time_label),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    description = stringResource(R.string.permissions_details_help_tag_install_time),
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
private fun TagHelpRow(
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
private fun PermissionsScreenReadyPreview() = PreviewWrapper {
    PermissionsScreen(
        state = PermissionsPreviewData.readyState(),
        onSearchChanged = {},
        onGroupClicked = {},
        onGroupLongPressed = {},
        onPermClicked = {},
        onPermLongPressed = {},
        onExpandAll = {},
        onCollapseAll = {},
        onRefresh = {},
        onSettings = {},
        onFilter = {},
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
