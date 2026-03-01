package eu.darken.myperm.settings.ui.support

import android.content.Intent
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
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material.icons.twotone.Folder
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.common.settings.SettingsBaseItem
import eu.darken.myperm.common.settings.SettingsCategoryHeader
import eu.darken.myperm.common.settings.SettingsDivider

@Composable
fun SupportScreenHost() {
    val navCtrl = LocalNavigationController.current
    val vm: SupportViewModel = hiltViewModel()
    val recorderState by vm.recorderState.collectAsState(initial = RecorderModule.State())
    val folderStats by vm.logFolderStats.collectAsState(initial = SupportViewModel.LogFolderStats())
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.stoppedRecording.collect { stopped ->
            try {
                val intent = Intent().apply {
                    setClassName(context, "eu.darken.myperm.common.debug.recording.ui.RecorderActivity")
                    putExtra("logPath", stopped.path)
                    putExtra("recordingStartedAt", stopped.startedAt)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    SupportScreen(
        onBack = { navCtrl?.up() },
        onContactSupport = { vm.navigateToContactForm() },
        onIssueTracker = { vm.openIssueTracker() },
        onDiscord = { vm.openDiscord() },
        isRecording = recorderState.isRecording,
        onStartDebugLog = { vm.startDebugLog() },
        onStopDebugLog = { vm.stopDebugLog() },
        logFileCount = folderStats.fileCount,
        logTotalSize = folderStats.totalSize,
        onClearDebugLogs = { vm.clearDebugLogs() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBack: () -> Unit,
    onContactSupport: () -> Unit,
    onIssueTracker: () -> Unit,
    onDiscord: () -> Unit,
    isRecording: Boolean,
    onStartDebugLog: () -> Unit,
    onStopDebugLog: () -> Unit,
    logFileCount: Int,
    logTotalSize: Long,
    onClearDebugLogs: () -> Unit,
) {
    var showDebugDialog by rememberSaveable { mutableStateOf(false) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
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
                title = stringResource(R.string.contact_support_label),
                subtitle = null,
                icon = Icons.TwoTone.Email,
                onClick = onContactSupport,
            )
            SettingsDivider()
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

            SettingsCategoryHeader(text = stringResource(R.string.settings_category_other_label))

            if (isRecording) {
                SettingsBaseItem(
                    title = stringResource(R.string.support_debuglog_stop_label),
                    subtitle = stringResource(R.string.support_debuglog_recording),
                    icon = Icons.TwoTone.Stop,
                    onClick = onStopDebugLog,
                )
            } else {
                SettingsBaseItem(
                    title = stringResource(R.string.support_debuglog_label),
                    subtitle = stringResource(R.string.support_debuglog_desc),
                    icon = Icons.TwoTone.Description,
                    onClick = { showDebugDialog = true },
                )
            }

            SettingsDivider()

            val folderSubtitle = if (logFileCount > 0) {
                pluralStringResource(
                    R.plurals.support_debuglog_folder_stats,
                    logFileCount,
                    logFileCount,
                    Formatter.formatShortFileSize(context, logTotalSize),
                )
            } else {
                stringResource(R.string.support_debuglog_folder_empty)
            }

            SettingsBaseItem(
                title = stringResource(R.string.support_debuglog_folder_label),
                subtitle = folderSubtitle,
                icon = Icons.TwoTone.Folder,
                enabled = logFileCount > 0,
                onClick = { showClearDialog = true },
                trailingContent = if (logFileCount > 0) {
                    {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.TwoTone.Delete, contentDescription = null)
                        }
                    }
                } else null,
            )
        }
    }

    if (showDebugDialog) {
        AlertDialog(
            onDismissRequest = { showDebugDialog = false },
            title = { Text(text = stringResource(R.string.support_debuglog_label)) },
            text = { Text(text = stringResource(R.string.settings_debuglog_explanation)) },
            confirmButton = {
                TextButton(onClick = {
                    showDebugDialog = false
                    onStartDebugLog()
                }) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDebugDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = stringResource(R.string.support_debuglog_clear_title)) },
            text = { Text(text = stringResource(R.string.support_debuglog_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearDebugLogs()
                }) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Preview2
@Composable
private fun SupportScreenPreview() = PreviewWrapper {
    SupportScreen(
        onBack = {},
        onContactSupport = {},
        onIssueTracker = {},
        onDiscord = {},
        isRecording = false,
        onStartDebugLog = {},
        onStopDebugLog = {},
        logFileCount = 3,
        logTotalSize = 1024 * 500L,
        onClearDebugLogs = {},
    )
}
