package eu.darken.myperm.apps.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper

@Composable
fun AppsFilterSortBottomSheet(
    currentFilterOptions: AppsFilterOptions,
    currentSortOptions: AppsSortOptions,
    onOptionsChanged: (AppsFilterOptions, AppsSortOptions) -> Unit,
    onDismiss: () -> Unit,
    onHelpClicked: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val currentFilters = remember {
        mutableStateSetOf<AppsFilterOptions.Filter>().apply { addAll(currentFilterOptions.filters) }
    }
    val currentSort = remember { mutableStateOf(currentSortOptions.mainSort) }

    val defaults = AppsFilterOptions() to AppsSortOptions()
    val hasNonDefaults = currentFilters.toSet() != defaults.first.filters || currentSort.value != defaults.second.mainSort

    val onlySystemApp = AppsFilterOptions.Filter.SYSTEM_APP in currentFilters
            && AppsFilterOptions.Filter.USER_APP !in currentFilters

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.apps_filter_sort_action),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onHelpClicked) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.label_help),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hasNonDefaults) {
                    TextButton(onClick = {
                        currentFilters.clear()
                        currentFilters.addAll(defaults.first.filters)
                        currentSort.value = defaults.second.mainSort
                        onOptionsChanged(defaults.first, defaults.second)
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.watcher_filter_reset))
                    }
                }
            }

            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
                Text(
                    text = stringResource(R.string.apps_filter_section_sort_by),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppsSortOptions.Sort.entries
                        .filter { it.showInSheet || it == currentSort.value }
                        .forEach { sort ->
                        FilterChip(
                            selected = sort == currentSort.value,
                            onClick = {
                                currentSort.value = sort
                                onOptionsChanged(
                                    AppsFilterOptions(filters = currentFilters.toSet()),
                                    AppsSortOptions(mainSort = sort),
                                )
                            },
                            label = { Text(stringResource(sort.labelRes)) },
                        )
                    }
                }

                AppsFilterOptions.Group.entries
                    .filter { group ->
                        AppsFilterOptions.Filter.entries.any { it.group == group && (it.showInSheet || it in currentFilters) }
                    }
                    .forEach { group ->
                    Text(
                        text = stringResource(group.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val isInstallSourceGroup = group == AppsFilterOptions.Group.INSTALL_SOURCE
                        AppsFilterOptions.Filter.entries
                            .filter { it.group == group && (it.showInSheet || it in currentFilters) }
                            .forEach { filter ->
                                val isSelected = filter in currentFilters
                                val isEnabled = !(isInstallSourceGroup && onlySystemApp)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) currentFilters.remove(filter)
                                        else currentFilters.add(filter)
                                        onOptionsChanged(
                                            AppsFilterOptions(filters = currentFilters.toSet()),
                                            AppsSortOptions(mainSort = currentSort.value),
                                        )
                                    },
                                    label = { Text(stringResource(filter.labelRes)) },
                                    enabled = isEnabled,
                                )
                            }
                    }
                }
            }
        }
    }
}

@Preview2
@Composable
private fun AppsFilterSortBottomSheetPreview() = PreviewWrapper {
    AppsFilterSortBottomSheet(
        currentFilterOptions = AppsFilterOptions(),
        currentSortOptions = AppsSortOptions(),
        onOptionsChanged = { _, _ -> },
        onDismiss = {},
        onHelpClicked = {},
    )
}

@Preview2
@Composable
private fun AppsFilterSortBottomSheetActivePreview() = PreviewWrapper {
    AppsFilterSortBottomSheet(
        currentFilterOptions = AppsFilterOptions(
            filters = setOf(AppsFilterOptions.Filter.USER_APP, AppsFilterOptions.Filter.NO_INTERNET)
        ),
        currentSortOptions = AppsSortOptions(mainSort = AppsSortOptions.Sort.APP_NAME),
        onOptionsChanged = { _, _ -> },
        onDismiss = {},
        onHelpClicked = {},
    )
}
