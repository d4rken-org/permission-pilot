package eu.darken.myperm.common.debug.recording.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.darken.myperm.R

@Composable
fun RecorderConsentDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onPrivacyPolicy: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.support_debuglog_label)) },
        text = { Text(stringResource(R.string.settings_debuglog_explanation)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.debug_debuglog_record_action))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onPrivacyPolicy) {
                    Text(stringResource(R.string.settings_privacy_policy_label))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        },
    )
}
