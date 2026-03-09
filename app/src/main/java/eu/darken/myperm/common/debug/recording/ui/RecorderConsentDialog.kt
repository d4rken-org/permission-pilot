package eu.darken.myperm.common.debug.recording.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper

@Composable
fun RecorderConsentDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onPrivacyPolicy: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.support_debuglog_label)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_debuglog_explanation))
                TextButton(
                    onClick = onPrivacyPolicy,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_privacy_policy_label))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.debug_debuglog_record_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Preview2
@Composable
private fun RecorderConsentDialogPreview() = PreviewWrapper {
    RecorderConsentDialog(
        onConfirm = {},
        onDismiss = {},
        onPrivacyPolicy = {},
    )
}
