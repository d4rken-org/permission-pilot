package eu.darken.myperm.watcher.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatcherFilterBottomSheet(
    currentOptions: WatcherFilterOptions,
    onFilterChanged: (WatcherFilterOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val currentSelection = remember {
        mutableStateSetOf<WatcherFilterOptions.Filter>().apply { addAll(currentOptions.keys) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.general_filter_action),
                    style = MaterialTheme.typography.titleLarge,
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

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                WatcherFilterOptions.Group.entries.forEach { group ->
                    Text(
                        text = stringResource(group.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    WatcherFilterOptions.Filter.entries
                        .filter { it.group == group }
                        .forEach { filter ->
                            val isChecked = filter in currentSelection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) currentSelection.remove(filter)
                                        else currentSelection.add(filter)
                                        onFilterChanged(WatcherFilterOptions(keys = currentSelection.toSet()))
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (isChecked) currentSelection.remove(filter)
                                        else currentSelection.add(filter)
                                        onFilterChanged(WatcherFilterOptions(keys = currentSelection.toSet()))
                                    },
                                )
                                Text(
                                    text = stringResource(filter.labelRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                }
            }
        }
    }
}
