package eu.darken.myperm.apps.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppsFilterSortBottomSheet(
    currentFilterOptions: AppsFilterOptions,
    currentSortOptions: AppsSortOptions,
    onOptionsChanged: (AppsFilterOptions, AppsSortOptions) -> Unit,
    onDismiss: () -> Unit,
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

            Text(
                text = stringResource(R.string.apps_filter_section_sort_by),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppsSortOptions.Sort.entries.forEach { sort ->
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

            AppsFilterOptions.Group.entries.forEach { group ->
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
                        .filter { it.group == group }
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
