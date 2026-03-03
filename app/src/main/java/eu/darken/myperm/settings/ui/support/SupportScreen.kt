package eu.darken.myperm.settings.ui.support

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.ChatBubble
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material.icons.twotone.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.compose.waitForState
import eu.darken.myperm.common.debug.recording.ui.RecorderConsentDialog
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.common.settings.SettingsBaseItem
import eu.darken.myperm.common.settings.SettingsCategoryHeader
import eu.darken.myperm.common.settings.SettingsDivider

@Composable
fun SupportScreenHost(vm: SupportViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by waitForState(vm.state)

    var showConsentDialog by remember { mutableStateOf(false) }
    var showShortRecordingWarning by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                SupportViewModel.Event.ShowConsentDialog -> {
                    showConsentDialog = true
                }

                SupportViewModel.Event.ShowShortRecordingWarning -> {
                    showShortRecordingWarning = true
                }
            }
        }
    }

    if (showConsentDialog) {
        RecorderConsentDialog(
            onConfirm = {
                showConsentDialog = false
                vm.startDebugLog()
            },
            onDismiss = { showConsentDialog = false },
            onPrivacyPolicy = {
                showConsentDialog = false
                vm.openPrivacyPolicy()
            },
        )
    }

    if (showShortRecordingWarning) {
        AlertDialog(
            onDismissRequest = { showShortRecordingWarning = false },
            title = { Text(stringResource(R.string.debug_debuglog_short_recording_title)) },
            text = { Text(stringResource(R.string.debug_debuglog_short_recording_message)) },
            confirmButton = {
                TextButton(onClick = { showShortRecordingWarning = false }) {
                    Text(stringResource(R.string.debug_debuglog_short_recording_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showShortRecordingWarning = false
                    vm.forceStopDebugLog()
                }) {
                    Text(stringResource(R.string.debug_debuglog_short_recording_stop))
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.support_debuglog_clear_title)) },
            text = { Text(stringResource(R.string.support_debuglog_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    vm.clearDebugLogs()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    state?.let {
        SupportScreen(
            state = it,
            onBack = { vm.navUp() },
            onContactSupport = { vm.navigateToContactForm() },
            onIssueTracker = { vm.openIssueTracker() },
            onDiscord = { vm.openDiscord() },
            onDebugLogToggle = { vm.onDebugLogToggle() },
            onClearDebugLogs = { showClearDialog = true },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    state: SupportViewModel.State,
    onBack: () -> Unit,
    onContactSupport: () -> Unit,
    onIssueTracker: () -> Unit,
    onDiscord: () -> Unit,
    onDebugLogToggle: () -> Unit,
    onClearDebugLogs: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_support_label)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsBaseItem(
                title = stringResource(R.string.support_contact_label),
                subtitle = stringResource(R.string.support_contact_desc),
                icon = Icons.TwoTone.Email,
                onClick = onContactSupport,
            )

            SettingsCategoryHeader(text = stringResource(R.string.settings_category_gethelp_label))

            SettingsBaseItem(
                title = stringResource(R.string.issue_tracker_label),
                subtitle = stringResource(R.string.issue_tracker_description),
                icon = Icons.TwoTone.BugReport,
                onClick = onIssueTracker,
            )
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.discord_label),
                subtitle = stringResource(R.string.discord_description),
                icon = Icons.TwoTone.ChatBubble,
                onClick = onDiscord,
            )

            SettingsCategoryHeader(text = stringResource(R.string.settings_category_debug_label))

            if (state.isRecording) {
                SettingsBaseItem(
                    title = stringResource(R.string.support_debuglog_stop_label),
                    subtitle = state.currentLogPath?.path ?: stringResource(R.string.support_debuglog_recording),
                    icon = Icons.TwoTone.Stop,
                    onClick = onDebugLogToggle,
                )
            } else {
                SettingsBaseItem(
                    title = stringResource(R.string.support_debuglog_label),
                    subtitle = stringResource(R.string.support_debuglog_desc),
                    icon = Icons.TwoTone.BugReport,
                    onClick = onDebugLogToggle,
                )
            }

            if (state.logSessionCount > 0 && !state.isRecording) {
                SettingsDivider()

                val logSizeFormatted = Formatter.formatShortFileSize(context, state.logFolderSize)
                SettingsBaseItem(
                    title = stringResource(R.string.support_debuglog_clear_action),
                    subtitle = pluralStringResource(
                        R.plurals.support_debuglog_folder_summary,
                        state.logSessionCount,
                        state.logSessionCount,
                        logSizeFormatted,
                    ),
                    icon = Icons.TwoTone.Delete,
                    onClick = onClearDebugLogs,
                )
            }
        }
    }
}

@Preview2
@Composable
private fun SupportScreenPreview() = PreviewWrapper {
    SupportScreen(
        state = SupportViewModel.State(),
        onBack = {},
        onContactSupport = {},
        onIssueTracker = {},
        onDiscord = {},
        onDebugLogToggle = {},
        onClearDebugLogs = {},
    )
}
