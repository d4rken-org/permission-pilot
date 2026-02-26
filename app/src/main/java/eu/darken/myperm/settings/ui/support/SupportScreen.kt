package eu.darken.myperm.settings.ui.support

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.ChatBubble
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    val recorderState by vm.recorderState.collectAsState(initial = RecorderModule.State(isAvailable = false))

    SupportScreen(
        onBack = { navCtrl?.up() },
        onIssueTracker = { vm.openIssueTracker() },
        onDiscord = { vm.openDiscord() },
        isDebugLogAvailable = recorderState.isAvailable,
        isRecording = recorderState.isRecording,
        onStartDebugLog = { vm.startDebugLog() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBack: () -> Unit,
    onIssueTracker: () -> Unit,
    onDiscord: () -> Unit,
    isDebugLogAvailable: Boolean,
    isRecording: Boolean,
    onStartDebugLog: () -> Unit,
) {
    var showDebugDialog by rememberSaveable { mutableStateOf(false) }

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

            if (isDebugLogAvailable) {
                SettingsCategoryHeader(text = stringResource(R.string.settings_category_other_label))
                SettingsBaseItem(
                    title = stringResource(R.string.support_debuglog_label),
                    subtitle = if (isRecording) "Recording..." else stringResource(R.string.support_debuglog_desc),
                    icon = Icons.TwoTone.Description,
                    enabled = !isRecording,
                    onClick = { showDebugDialog = true },
                )
            }
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
}

@Preview2
@Composable
private fun SupportScreenPreview() = PreviewWrapper {
    SupportScreen(
        onBack = {},
        onIssueTracker = {},
        onDiscord = {},
        isDebugLogAvailable = true,
        isRecording = false,
        onStartDebugLog = {},
    )
}
