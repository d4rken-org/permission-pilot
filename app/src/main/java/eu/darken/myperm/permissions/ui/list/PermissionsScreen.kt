package eu.darken.myperm.permissions.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.LabeledOption
import eu.darken.myperm.common.compose.MultiChoiceFilterDialog
import eu.darken.myperm.common.compose.Pill
import eu.darken.myperm.common.compose.PermissionIcon
import eu.darken.myperm.common.compose.PermissionTagPill
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.compose.SingleChoiceSortDialog
import eu.darken.myperm.common.compose.icon
import androidx.compose.runtime.collectAsState
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
    var showTagHelpDialog by rememberSaveable { mutableStateOf(false) }

    if (showTagHelpDialog) {
        PermissionTagHelpDialog(onDismiss = { showTagHelpDialog = false })
    }

    Scaffold(
        topBar = {
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
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            searchQuery = ""
                            onSearchChanged(null)
                        }
                    }) {
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
            // Filter/Sort chip row + help icon
            if (state is PermissionsViewModel.State.Ready) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val hasActiveFilter = state.filterOptions.keys != PermsFilterOptions().keys
                        FilterChip(
                            selected = hasActiveFilter,
                            onClick = onFilterClicked,
                            label = { Text(stringResource(R.string.general_filter_action)) },
                            leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                        FilterChip(
                            selected = false,
                            onClick = onSortClicked,
                            label = { Text("${stringResource(R.string.general_sort_action)}: ${stringResource(state.sortOptions.mainSort.labelRes)}") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                    IconButton(onClick = { showTagHelpDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.label_help),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Search bar
            AnimatedVisibility(visible = isSearchActive) {
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
                                        // Full-width divider between groups (not before first)
                                        if (index > 0) {
                                            val prevItem = state.listData[index - 1]
                                            if (prevItem is PermissionsViewModel.ListItem.Group ||
                                                prevItem is PermissionsViewModel.ListItem.Perm
                                            ) {
                                                HorizontalDivider()
                                            }
                                        }
                                        PermissionGroupHeader(
                                            item = listItem.item,
                                            onClick = { onGroupClicked(listItem.item.group.id) },
                                        )
                                    }

                                    is PermissionsViewModel.ListItem.Perm -> {
                                        // Inset divider between permission items
                                        if (index > 0 && state.listData[index - 1] is PermissionsViewModel.ListItem.Perm) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 48.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                            )
                                        }
                                        PermissionListItem(
                                            item = listItem.item,
                                            onClick = { onPermClicked(listItem.item) },
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
                imageVector = item.group.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
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
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick),
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
            PermissionIcon(
                permissionId = item.permission.id,
                modifier = Modifier.size(24.dp),
                fallbackModel = item.permission,
            )
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
private fun PermissionTagHelpDialog(onDismiss: () -> Unit) {
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
private fun PermissionsScreenActiveFilterPreview() = PreviewWrapper {
    PermissionsScreen(
        state = PermissionsPreviewData.activeFilterState(),
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
