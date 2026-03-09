package eu.darken.myperm.settings.ui.support

import android.content.Intent
import android.text.format.DateUtils
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.ChatBubble
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material.icons.twotone.FiberManualRecord
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material.icons.twotone.QuestionAnswer
import androidx.compose.material.icons.twotone.Stop
import androidx.compose.material.icons.twotone.Warning
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.debug.recording.core.DebugSession
import eu.darken.myperm.common.debug.recording.ui.RecorderActivity
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

    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle(initialValue = null)

    var showConsentDialog by remember { mutableStateOf(false) }
    var showShortRecordingWarning by remember { mutableStateOf(false) }
    var showSessionSheet by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var sessionIdToDelete by remember { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(Unit) {
        vm.refreshSessions()
        onPauseOrDispose {}
    }

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
                    val intent = RecorderActivity.getLaunchIntent(
                        context,
                        event.sessionId,
                        event.legacyPath,
                    ).apply {
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
                    vm.deleteAllSessions()
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

    sessionIdToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { sessionIdToDelete = null },
            title = { Text(stringResource(R.string.support_debuglog_session_delete_title)) },
            text = { Text(stringResource(R.string.support_debuglog_session_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSession(id)
                    sessionIdToDelete = null
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionIdToDelete = null }) {
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
            onFaq = { vm.openFaq() },
            onIssueTracker = { vm.openIssueTracker() },
            onDiscord = { vm.openDiscord() },
            onDebugLogToggle = { vm.onDebugLogToggle() },
            onOpenSessionSheet = { showSessionSheet = true },
        )

        if (showSessionSheet) {
            DebugLogSessionSheet(
                sessions = currentState.sessions,
                onOpenSession = { sessionId -> vm.openSession(sessionId) },
                onStopRecording = { vm.forceStopDebugLog() },
                onDeleteSession = { sessionId -> sessionIdToDelete = sessionId },
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
    onFaq: () -> Unit,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item { SettingsCategoryHeader(text = stringResource(R.string.settings_category_gethelp_label)) }

            item {
                SettingsBaseItem(
                    title = stringResource(R.string.faq_label),
                    subtitle = stringResource(R.string.faq_description),
                    icon = Icons.TwoTone.QuestionAnswer,
                    onClick = onFaq,
                )
            }
            item { SettingsDivider() }
            item {
                SettingsBaseItem(
                    title = stringResource(R.string.issue_tracker_label),
                    subtitle = stringResource(R.string.issue_tracker_description),
                    icon = Icons.TwoTone.BugReport,
                    onClick = onIssueTracker,
                )
            }
            item { SettingsDivider() }
            item {
                SettingsBaseItem(
                    title = stringResource(R.string.discord_label),
                    subtitle = stringResource(R.string.discord_description),
                    icon = Icons.TwoTone.ChatBubble,
                    onClick = onDiscord,
                )
            }
            item { SettingsDivider() }
            item {
                SettingsBaseItem(
                    title = stringResource(R.string.support_contact_label),
                    subtitle = stringResource(R.string.support_contact_desc),
                    icon = Icons.TwoTone.Email,
                    onClick = onContactSupport,
                )
            }

            item { SettingsCategoryHeader(text = stringResource(R.string.settings_category_debug_label)) }

            item {
                if (state.isRecording) {
                    SettingsBaseItem(
                        title = stringResource(R.string.support_debuglog_stop_label),
                        subtitle = stringResource(R.string.support_debuglog_recording),
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
            }

            if (state.hasAnySessions) {
                item { SettingsDivider() }

                item {
                    val logSizeFormatted = Formatter.formatShortFileSize(context, state.logFolderSize)
                    val sessionCount = state.logSessionCount
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebugLogSessionSheet(
    sessions: List<DebugSession>,
    onOpenSession: (String) -> Unit,
    onStopRecording: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val hasStoredSessions = sessions.any { it !is DebugSession.Recording }

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
                    SessionRow(
                        session = session,
                        context = context,
                        onOpen = { onOpenSession(session.id) },
                        onStop = onStopRecording,
                        onDelete = { onDeleteSession(session.id) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SessionRow(
    session: DebugSession,
    context: android.content.Context,
    onOpen: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (session is DebugSession.Ready) Modifier.clickable(onClick = onOpen)
                else Modifier
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (session) {
            is DebugSession.Recording -> {
                Icon(
                    imageVector = Icons.TwoTone.FiberManualRecord,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            is DebugSession.Compressing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            is DebugSession.Ready -> {
                Icon(
                    imageVector = Icons.TwoTone.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            is DebugSession.Failed -> {
                Icon(
                    imageVector = Icons.TwoTone.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = when (session) {
                    is DebugSession.Recording -> stringResource(R.string.support_debuglog_session_recording)
                    is DebugSession.Compressing -> stringResource(R.string.support_debuglog_session_compressing)
                    is DebugSession.Ready -> {
                        val size = Formatter.formatShortFileSize(context, session.diskSize)
                        val ago = DateUtils.getRelativeTimeSpanString(
                            session.createdAt.toEpochMilli(),
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                        )
                        "$size \u00B7 $ago"
                    }
                    is DebugSession.Failed -> {
                        val reason = when (session.reason) {
                            DebugSession.Failed.Reason.EMPTY_LOG -> stringResource(R.string.support_debuglog_failed_empty_log)
                            DebugSession.Failed.Reason.MISSING_LOG -> stringResource(R.string.support_debuglog_failed_missing_log)
                            DebugSession.Failed.Reason.CORRUPT_ZIP -> stringResource(R.string.support_debuglog_failed_corrupt_zip)
                            DebugSession.Failed.Reason.ZIP_FAILED -> stringResource(R.string.support_debuglog_failed_zip_failed)
                        }
                        val ago = DateUtils.getRelativeTimeSpanString(
                            session.createdAt.toEpochMilli(),
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                        )
                        "$reason \u00B7 $ago"
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (session) {
                    is DebugSession.Recording -> MaterialTheme.colorScheme.error
                    is DebugSession.Failed -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        when (session) {
            is DebugSession.Recording -> {
                IconButton(onClick = onStop) {
                    Icon(
                        imageVector = Icons.TwoTone.Stop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is DebugSession.Compressing -> {
                // No action while compressing
            }
            is DebugSession.Ready, is DebugSession.Failed -> {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.TwoTone.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        onFaq = {},
        onIssueTracker = {},
        onDiscord = {},
        onDebugLogToggle = {},
        onOpenSessionSheet = {},
    )
}
