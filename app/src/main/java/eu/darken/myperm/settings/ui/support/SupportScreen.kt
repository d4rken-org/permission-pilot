package eu.darken.myperm.settings.ui.support

import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.ChatBubble
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material.icons.twotone.ErrorOutline
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material.icons.twotone.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.compose.waitForState
import eu.darken.myperm.common.debug.recording.core.DebugSessionManager
import eu.darken.myperm.common.debug.recording.ui.RecorderActivity
import eu.darken.myperm.common.debug.recording.ui.RecorderConsentDialog
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.common.settings.SettingsBaseItem
import eu.darken.myperm.common.settings.SettingsCategoryHeader
import eu.darken.myperm.common.settings.SettingsDivider
import java.text.DateFormat
import java.util.Date

@Composable
fun SupportScreenHost(vm: SupportViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val context = LocalContext.current
    val state by waitForState(vm.state)

    var showConsentDialog by remember { mutableStateOf(false) }
    var showShortRecordingWarning by remember { mutableStateOf(false) }
    var showSessionSheet by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<DebugSessionManager.LogSession?>(null) }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                SupportViewModel.Event.ShowConsentDialog -> {
                    showConsentDialog = true
                }

                SupportViewModel.Event.ShowShortRecordingWarning -> {
                    showShortRecordingWarning = true
                }

                is SupportViewModel.Event.OpenRecorderActivity -> {
                    val intent = RecorderActivity.getLaunchIntent(context, event.path.path).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
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

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(stringResource(R.string.support_debuglog_session_delete_title)) },
            text = { Text(stringResource(R.string.support_debuglog_session_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteLogSession(session)
                    sessionToDelete = null
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    state?.let { currentState ->
        SupportScreen(
            state = currentState,
            onBack = { vm.navUp() },
            onContactSupport = { vm.navigateToContactForm() },
            onIssueTracker = { vm.openIssueTracker() },
            onDiscord = { vm.openDiscord() },
            onDebugLogToggle = { vm.onDebugLogToggle() },
            onOpenSessionSheet = { showSessionSheet = true },
        )

        if (showSessionSheet) {
            DebugLogSessionSheet(
                sessions = currentState.sessions,
                onOpenSession = { session -> vm.openSession(session) },
                onStopRecording = { vm.stopActiveRecording() },
                onDeleteSession = { session -> sessionToDelete = session },
                onDeleteAll = { showClearDialog = true },
                onDismiss = { showSessionSheet = false },
            )
        }
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
    onOpenSessionSheet: () -> Unit,
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

            if (state.hasAnySessions) {
                SettingsDivider()

                val logSizeFormatted = Formatter.formatShortFileSize(context, state.storedSessionSize)
                val sessionCount = state.storedSessionCount
                SettingsBaseItem(
                    title = stringResource(R.string.support_debuglog_sessions_title),
                    subtitle = if (sessionCount > 0) {
                        pluralStringResource(
                            R.plurals.support_debuglog_folder_summary,
                            sessionCount,
                            sessionCount,
                            logSizeFormatted,
                        )
                    } else {
                        stringResource(R.string.support_debuglog_recording)
                    },
                    icon = Icons.TwoTone.Folder,
                    onClick = onOpenSessionSheet,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebugLogSessionSheet(
    sessions: List<DebugSessionManager.LogSession>,
    onOpenSession: (DebugSessionManager.LogSession.Ready) -> Unit,
    onStopRecording: () -> Unit,
    onDeleteSession: (DebugSessionManager.LogSession) -> Unit,
    onDeleteAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    val hasStoredSessions = sessions.any { it !is DebugSessionManager.LogSession.Recording }

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
                    text = stringResource(R.string.support_debuglog_sessions_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (hasStoredSessions) {
                    TextButton(onClick = onDeleteAll) {
                        Text(stringResource(R.string.support_debuglog_clear_action))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (sessions.isEmpty()) {
                Text(
                    text = stringResource(R.string.support_debuglog_sessions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                sessions.forEach { session ->
                    when (session) {
                        is DebugSessionManager.LogSession.Recording -> RecordingSessionRow(
                            session = session,
                            onStop = onStopRecording,
                        )

                        is DebugSessionManager.LogSession.Ready -> ReadySessionRow(
                            session = session,
                            dateFormat = dateFormat,
                            context = context,
                            onOpen = { onOpenSession(session) },
                            onDelete = { onDeleteSession(session) },
                        )

                        is DebugSessionManager.LogSession.Failed -> FailedSessionRow(
                            session = session,
                            onDelete = { onDeleteSession(session) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecordingSessionRow(
    session: DebugSessionManager.LogSession.Recording,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.path.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.support_debuglog_recording),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        IconButton(onClick = onStop) {
            Icon(
                imageVector = Icons.TwoTone.Stop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ReadySessionRow(
    session: DebugSessionManager.LogSession.Ready,
    dateFormat: DateFormat,
    context: android.content.Context,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.path.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Row {
                Text(
                    text = Formatter.formatShortFileSize(context, session.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormat.format(Date(session.lastModified)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.TwoTone.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FailedSessionRow(
    session: DebugSessionManager.LogSession.Failed,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.TwoTone.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.path.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.support_debuglog_session_invalid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.TwoTone.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        onOpenSessionSheet = {},
    )
}
