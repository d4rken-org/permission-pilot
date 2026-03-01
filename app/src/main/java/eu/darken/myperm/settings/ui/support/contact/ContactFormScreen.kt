package eu.darken.myperm.settings.ui.support.contact

import android.content.ActivityNotFoundException
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.ContactSupport
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.settings.ui.support.contact.ContactFormViewModel.Category
import eu.darken.myperm.settings.ui.support.contact.ContactFormViewModel.Companion.MIN_WORDS
import eu.darken.myperm.settings.ui.support.contact.ContactFormViewModel.Companion.MIN_WORDS_EXPECTED
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ContactFormScreenHost() {
    val navCtrl = LocalNavigationController.current
    val vm: ContactFormViewModel = hiltViewModel()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val category by vm.category.collectAsState()
    val description by vm.description.collectAsState()
    val expectedBehavior by vm.expectedBehavior.collectAsState()
    val selectedLogFile by vm.selectedLogFile.collectAsState()
    val isRecording by vm.isRecording.collectAsState(initial = false)
    val logFiles by vm.logFiles.collectAsState(initial = emptyList())
    val descWordCount by vm.descriptionWordCount.collectAsState(initial = 0)
    val expectedWordCount by vm.expectedBehaviorWordCount.collectAsState(initial = 0)
    val canSend by vm.canSend.collectAsState(initial = false)

    val noEmailMsg = stringResource(R.string.contact_no_email_app)
    LaunchedEffect(Unit) {
        vm.emailEvent.collect { intent ->
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                snackbarHostState.showSnackbar(noEmailMsg)
            }
        }
    }

    ContactFormScreen(
        snackbarHostState = snackbarHostState,
        onBack = { navCtrl?.up() },
        category = category,
        onCategoryChange = { vm.setCategory(it) },
        description = description,
        onDescriptionChange = { vm.setDescription(it) },
        descWordCount = descWordCount,
        expectedBehavior = expectedBehavior,
        onExpectedBehaviorChange = { vm.setExpectedBehavior(it) },
        expectedWordCount = expectedWordCount,
        isRecording = isRecording,
        logFiles = logFiles,
        selectedLogFile = selectedLogFile,
        onSelectLogFile = { vm.selectLogFile(it) },
        onDeleteLogFile = { vm.deleteLogFile(it) },
        onStartRecording = { vm.startRecording() },
        onStopRecording = { vm.stopRecording() },
        canSend = canSend,
        onSend = { vm.send() },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactFormScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    category: Category,
    onCategoryChange: (Category) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    descWordCount: Int,
    expectedBehavior: String,
    onExpectedBehaviorChange: (String) -> Unit,
    expectedWordCount: Int,
    isRecording: Boolean,
    logFiles: List<File>,
    selectedLogFile: File?,
    onSelectLogFile: (File?) -> Unit,
    onDeleteLogFile: (File) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    canSend: Boolean,
    onSend: () -> Unit,
) {
    val isBug = category == Category.BUG
    var showRecordConsentDialog by rememberSaveable { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

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
            SectionCard(title = stringResource(R.string.contact_category_question_label).let { "Category" }) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == Category.QUESTION,
                        onClick = { onCategoryChange(Category.QUESTION) },
                        label = { Text(stringResource(R.string.contact_category_question_label)) },
                    )
                    FilterChip(
                        selected = category == Category.FEATURE,
                        onClick = { onCategoryChange(Category.FEATURE) },
                        label = { Text(stringResource(R.string.contact_category_feature_label)) },
                    )
                    FilterChip(
                        selected = category == Category.BUG,
                        onClick = { onCategoryChange(Category.BUG) },
                        label = { Text(stringResource(R.string.contact_category_bug_label)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Debug log picker (BUG only)
            AnimatedVisibility(visible = isBug) {
                Column {
                    DebugLogPickerCard(
                        isRecording = isRecording,
                        logFiles = logFiles,
                        selectedLogFile = selectedLogFile,
                        onSelectLogFile = onSelectLogFile,
                        onDeleteLogFile = { fileToDelete = it },
                        onStartRecording = { showRecordConsentDialog = true },
                        onStopRecording = onStopRecording,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Description
            val descHint = when (category) {
                Category.QUESTION -> stringResource(R.string.contact_description_hint_question)
                Category.FEATURE -> stringResource(R.string.contact_description_hint_feature)
                Category.BUG -> stringResource(R.string.contact_description_hint_bug)
            }
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.contact_description_label)) },
                placeholder = { Text(descHint) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                supportingText = {
                    WordCountText(count = descWordCount, minimum = MIN_WORDS)
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expected behavior (BUG only)
            AnimatedVisibility(visible = isBug) {
                Column {
                    OutlinedTextField(
                        value = expectedBehavior,
                        onValueChange = onExpectedBehaviorChange,
                        label = { Text(stringResource(R.string.contact_expected_behavior_label)) },
                        placeholder = { Text(stringResource(R.string.contact_expected_behavior_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        supportingText = {
                            WordCountText(count = expectedWordCount, minimum = MIN_WORDS_EXPECTED)
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
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
                        imageVector = Icons.TwoTone.ContactSupport,
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
                enabled = canSend,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Email,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
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

    // Recording consent dialog
    if (showRecordConsentDialog) {
        AlertDialog(
            onDismissRequest = { showRecordConsentDialog = false },
            title = { Text(stringResource(R.string.support_debuglog_label)) },
            text = { Text(stringResource(R.string.settings_debuglog_explanation)) },
            confirmButton = {
                TextButton(onClick = {
                    showRecordConsentDialog = false
                    onStartRecording()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordConsentDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    // Delete log confirmation dialog
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(stringResource(R.string.contact_debuglog_delete_title)) },
            text = { Text(stringResource(R.string.contact_debuglog_delete_message, file.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteLogFile(file)
                    fileToDelete = null
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
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
    logFiles: List<File>,
    selectedLogFile: File?,
    onSelectLogFile: (File?) -> Unit,
    onDeleteLogFile: (File) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    val context = LocalContext.current

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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

            if (logFiles.isEmpty() && !isRecording) {
                Text(
                    text = stringResource(R.string.contact_debuglog_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            logFiles.forEach { file ->
                val isSelected = selectedLogFile == file
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectLogFile(if (isSelected) null else file)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectLogFile(if (isSelected) null else file) },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                        Text(
                            text = Formatter.formatShortFileSize(context, file.length()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDeleteLogFile(file) }) {
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
