package eu.darken.myperm.settings.ui.support.contact

import android.content.ActivityNotFoundException
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.twotone.ContactSupport
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.SectionCard
import eu.darken.myperm.common.debug.recording.core.DebugSession
import eu.darken.myperm.common.debug.recording.ui.RecorderConsentDialog
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.settings.ui.support.contact.ContactFormViewModel.Category
import eu.darken.myperm.settings.ui.support.contact.ContactFormViewModel.Companion.MIN_WORDS
import eu.darken.myperm.settings.ui.support.contact.ContactFormViewModel.Companion.MIN_WORDS_EXPECTED

@Composable
fun ContactFormScreenHost(vm: ContactFormViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val state by vm.state.collectAsStateWithLifecycle(initialValue = null)

    var showConsentDialog by remember { mutableStateOf(false) }
    var showShortRecordingWarning by remember { mutableStateOf(false) }
    var sessionIdToDelete by remember { mutableStateOf<String?>(null) }
    var hasSentEmail by rememberSaveable { mutableStateOf(false) }
    var showSentConfirm by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        vm.refreshLogSessions()
        if (hasSentEmail) {
            showSentConfirm = true
            hasSentEmail = false
        }
        onPauseOrDispose {}
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is ContactFormViewModel.Event.OpenEmail -> {
                    hasSentEmail = true
                    try {
                        context.startActivity(event.intent)
                    } catch (_: ActivityNotFoundException) {
                        hasSentEmail = false
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.contact_no_email_app)
                        )
                    }
                }

                is ContactFormViewModel.Event.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                ContactFormViewModel.Event.ShowConsentDialog -> {
                    showConsentDialog = true
                }

                ContactFormViewModel.Event.ShowShortRecordingWarning -> {
                    showShortRecordingWarning = true
                }
            }
        }
    }

    if (showConsentDialog) {
        RecorderConsentDialog(
            onConfirm = {
                showConsentDialog = false
                vm.doStartRecording()
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
                    vm.forceStopRecording()
                }) {
                    Text(stringResource(R.string.debug_debuglog_short_recording_stop))
                }
            },
        )
    }

    sessionIdToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { sessionIdToDelete = null },
            title = { Text(stringResource(R.string.contact_debuglog_delete_title)) },
            text = { Text(stringResource(R.string.contact_debuglog_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteLogSession(id)
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

    if (showSentConfirm) {
        val selectedId = state?.selectedSessionId
        AlertDialog(
            onDismissRequest = { showSentConfirm = false },
            title = { Text(stringResource(R.string.support_contact_sent_title)) },
            text = { Text(stringResource(R.string.support_contact_sent_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showSentConfirm = false
                    vm.confirmSent(selectedId)
                }) {
                    Text(stringResource(R.string.general_done_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSentConfirm = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    state?.let {
        ContactFormScreen(
            state = it,
            snackbarHostState = snackbarHostState,
            onBack = { vm.navUp() },
            onCategoryChange = { cat -> vm.updateCategory(cat) },
            onDescriptionChange = { text -> vm.updateDescription(text) },
            onExpectedBehaviorChange = { text -> vm.updateExpectedBehavior(text) },
            onSelectSession = { id -> vm.selectLogSession(id) },
            onDeleteSession = { id -> sessionIdToDelete = id },
            onStartRecording = { vm.startRecording() },
            onStopRecording = { vm.stopRecording() },
            onSend = { vm.send() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactFormScreen(
    state: ContactFormViewModel.State,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCategoryChange: (Category) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onExpectedBehaviorChange: (String) -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.contact_support_label)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Category chips
            SectionCard(title = stringResource(R.string.contact_category_label)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.category == Category.QUESTION,
                        onClick = { onCategoryChange(Category.QUESTION) },
                        label = { Text(stringResource(R.string.contact_category_question_label)) },
                    )
                    FilterChip(
                        selected = state.category == Category.FEATURE,
                        onClick = { onCategoryChange(Category.FEATURE) },
                        label = { Text(stringResource(R.string.contact_category_feature_label)) },
                    )
                    FilterChip(
                        selected = state.category == Category.BUG,
                        onClick = { onCategoryChange(Category.BUG) },
                        label = { Text(stringResource(R.string.contact_category_bug_label)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Debug log picker (BUG only)
            if (state.isBug) {
                DebugLogPickerCard(
                    isRecording = state.isRecording,
                    sessions = state.sessions,
                    selectedSessionId = state.selectedSessionId,
                    onSelectSession = onSelectSession,
                    onDeleteSession = onDeleteSession,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Description
            val descHint = when (state.category) {
                Category.QUESTION -> stringResource(R.string.contact_description_hint_question)
                Category.FEATURE -> stringResource(R.string.contact_description_hint_feature)
                Category.BUG -> stringResource(R.string.contact_description_hint_bug)
            }
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.contact_description_label)) },
                placeholder = { Text(descHint) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                supportingText = {
                    WordCountText(count = state.descriptionWords, minimum = MIN_WORDS)
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expected behavior (BUG only)
            if (state.isBug) {
                OutlinedTextField(
                    value = state.expectedBehavior,
                    onValueChange = onExpectedBehaviorChange,
                    label = { Text(stringResource(R.string.contact_expected_behavior_label)) },
                    placeholder = { Text(stringResource(R.string.contact_expected_behavior_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    supportingText = {
                        WordCountText(count = state.expectedWords, minimum = MIN_WORDS_EXPECTED)
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Welcome information card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.TwoTone.ContactSupport,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.contact_welcome_message),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Send button
            Button(
                onClick = onSend,
                enabled = state.canSend,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.TwoTone.Email,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                }
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.contact_send_action))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer text
            Text(
                text = stringResource(R.string.contact_footer_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WordCountText(count: Int, minimum: Int) {
    val color = when {
        count == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        count < minimum -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Text(
        text = pluralStringResource(R.plurals.contact_word_count, count, count, minimum),
        color = color,
    )
}

@Composable
private fun DebugLogPickerCard(
    isRecording: Boolean,
    sessions: List<DebugSession.Ready>,
    selectedSessionId: String?,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.TwoTone.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.contact_debuglog_picker_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.contact_debuglog_picker_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty() && !isRecording) {
                Text(
                    text = stringResource(R.string.contact_debuglog_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            sessions.forEach { session ->
                val isSelected = selectedSessionId == session.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSession(session.id) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectSession(session.id) },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                        Row {
                            Text(
                                text = Formatter.formatShortFileSize(context, session.diskSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = DateUtils.getRelativeTimeSpanString(
                                    session.createdAt.toEpochMilli(),
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS,
                                ).toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { onDeleteSession(session.id) }) {
                        Icon(
                            imageVector = Icons.TwoTone.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { if (isRecording) onStopRecording() else onStartRecording() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) {
                Icon(
                    imageVector = Icons.TwoTone.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = if (isRecording) {
                        stringResource(R.string.contact_debuglog_stop_action)
                    } else {
                        stringResource(R.string.contact_debuglog_record_action)
                    }
                )
            }
        }
    }
}
