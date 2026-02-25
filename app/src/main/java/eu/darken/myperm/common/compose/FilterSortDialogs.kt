package eu.darken.myperm.common.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R
import eu.darken.myperm.apps.ui.details.AppDetailsFilterOptions
import eu.darken.myperm.apps.ui.list.AppsSortOptions
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper

data class LabeledOption<T>(
    val value: T,
    @StringRes val labelRes: Int,
)

@Composable
fun <T> MultiChoiceFilterDialog(
    title: String,
    options: List<LabeledOption<T>>,
    selected: Set<T>,
    onConfirm: (Set<T>) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentSelection = remember { mutableStateSetOf<T>().apply { addAll(selected) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    val isChecked = option.value in currentSelection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) currentSelection.remove(option.value)
                                else currentSelection.add(option.value)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                if (isChecked) currentSelection.remove(option.value)
                                else currentSelection.add(option.value)
                            },
                        )
                        Text(
                            text = stringResource(option.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentSelection.toSet()) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
fun <T> SingleChoiceSortDialog(
    title: String,
    options: List<LabeledOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.value) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option.value == selected,
                            onClick = { onSelect(option.value) },
                        )
                        Text(
                            text = stringResource(option.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Preview2
@Composable
private fun MultiChoiceFilterDialogPreview() = PreviewWrapper {
    MultiChoiceFilterDialog(
        title = "Filter",
        options = AppDetailsFilterOptions.Filter.entries.map { LabeledOption(it, it.labelRes) },
        selected = setOf(AppDetailsFilterOptions.Filter.GRANTED),
        onConfirm = {},
        onDismiss = {},
    )
}

@Preview2
@Composable
private fun SingleChoiceSortDialogPreview() = PreviewWrapper {
    SingleChoiceSortDialog(
        title = "Sort",
        options = AppsSortOptions.Sort.entries.map { LabeledOption(it, it.labelRes) },
        selected = AppsSortOptions.Sort.UPDATED_AT,
        onSelect = {},
        onDismiss = {},
    )
}
