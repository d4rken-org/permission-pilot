package eu.darken.myperm.common.error

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.findActivity
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag

@Composable
fun ErrorEventHandler(source: ErrorEventSource2) {
    val errorEvents = source.errorEvents
    var currentError by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(errorEvents) { errorEvents.collect { error -> currentError = error } }

    currentError?.let { error ->
        ComposeErrorDialog(
            throwable = error,
            onDismiss = { currentError = null },
        )
    }
}

@Composable
private fun ComposeErrorDialog(
    throwable: Throwable,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // Prefer the curated HasLocalizedError strings (e.g. billing errors) over raw exception
    // messages like Play Billing's internal debugMessage.
    val localizedError = remember(throwable, context) { throwable.localized(context) }

    // findActivity(), not a raw cast: the composition's context is usually a ContextWrapper chain
    // (ContextThemeWrapper around the activity), which a cast would miss.
    val activity = remember(context) { context.findActivity() }
    // An error that offers a way out gets its own action plus a dismiss; everything else keeps the
    // acknowledge-only shape. The activity is required to launch the fix.
    val hasFix = localizedError.fixAction != null && activity != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = localizedError.label) },
        text = { Text(text = localizedError.description) },
        confirmButton = {
            if (hasFix) {
                TextButton(
                    onClick = {
                        // Fix actions are arbitrary code (e.g. intent launches): a throw here would
                        // crash the UI thread from inside a click handler, and skipping onDismiss()
                        // would leave the dialog latched on the current error with no way out.
                        try {
                            localizedError.fixAction!!.invoke(activity!!)
                        } catch (e: Exception) {
                            log(TAG, ERROR) { "Error action failed: ${e.asLog()}" }
                        } finally {
                            onDismiss()
                        }
                    },
                ) {
                    Text(text = localizedError.fixActionLabel ?: stringResource(android.R.string.ok))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        },
        dismissButton = if (hasFix) {
            {
                TextButton(onClick = onDismiss) {
                    // PP's established dismiss idiom on every other dialog — and fully translated.
                    Text(text = stringResource(R.string.general_close_action))
                }
            }
        } else {
            null
        },
    )
}

private val TAG = logTag("Error", "Dialog")
