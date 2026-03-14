package eu.darken.myperm.watcher.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WatcherFilterBottomSheet(
    currentOptions: WatcherFilterOptions,
    onFilterChanged: (WatcherFilterOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val currentSelection = remember {
        mutableStateSetOf<WatcherFilterOptions.Filter>().apply { addAll(currentOptions.filters) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.general_filter_action),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (currentSelection.isNotEmpty()) {
                    TextButton(onClick = {
                        currentSelection.clear()
                        onFilterChanged(WatcherFilterOptions())
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.watcher_filter_reset))
                    }
                }
            }

            WatcherFilterOptions.Group.entries.forEach { group ->
                Text(
                    text = stringResource(group.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    WatcherFilterOptions.Filter.entries
                        .filter { it.group == group }
                        .forEach { filter ->
                            val isSelected = filter in currentSelection
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) currentSelection.remove(filter)
                                    else currentSelection.add(filter)
                                    onFilterChanged(WatcherFilterOptions(filters = currentSelection.toSet()))
                                },
                                label = { Text(stringResource(filter.labelRes)) },
                            )
                        }
                }
            }
        }
    }
}
