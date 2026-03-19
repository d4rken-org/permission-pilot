package eu.darken.myperm.settings.ui.watcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.DeleteSweep
import androidx.compose.material.icons.twotone.FilterList
import eu.darken.myperm.common.compose.LucideRadar
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.Schedule
import androidx.compose.material3.Switch
import androidx.compose.material.icons.twotone.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.LabeledOption
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.compose.SingleChoiceSortDialog
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.common.settings.SettingsBaseItem
import eu.darken.myperm.common.settings.SettingsDivider
import eu.darken.myperm.common.settings.SettingsSwitchItem
import eu.darken.myperm.common.settings.SettingsUpgradeIcon
import eu.darken.myperm.watcher.core.WatcherScope

@Composable
fun WatcherSettingsScreenHost() {
    val navCtrl = LocalNavigationController.current
    val vm: WatcherSettingsViewModel = hiltViewModel()

    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val context = LocalContext.current

    val isPro by vm.isPro.collectAsState()
    val isWatcherEnabled by vm.isWatcherEnabled.collectAsState(initial = false)
    val watcherScope by vm.watcherScope.collectAsState(initial = WatcherScope.NON_SYSTEM)
    val isNotificationsEnabled by vm.isNotificationsEnabled.collectAsState(initial = true)
    val isNotifyOnlyOnGained by vm.isNotifyOnlyOnGained.collectAsState(initial = true)
    val retentionDays by vm.retentionDays.collectAsState(initial = 30)
    val pollingIntervalHours by vm.pollingIntervalHours.collectAsState(initial = 4)
    val reportCount by vm.reportCount.collectAsState(initial = 0)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    }

    WatcherSettingsScreen(
        onBack = { navCtrl?.up() },
        isPro = isPro,
        isWatcherEnabled = isWatcherEnabled,
        onWatcherEnabledChanged = { vm.setWatcherEnabled(it) },
        onUpgrade = { vm.onUpgrade() },
        watcherScope = watcherScope,
        onWatcherScopeSelected = { vm.setWatcherScope(it) },
        isNotificationsEnabled = isNotificationsEnabled,
        onNotificationsEnabledChanged = { enabled ->
            vm.setNotificationsEnabled(enabled)
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && vm.isNotificationPermissionDenied()) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        isNotifyOnlyOnGained = isNotifyOnlyOnGained,
        onNotifyOnlyOnGainedChanged = { vm.setNotifyOnlyOnGained(it) },
        retentionDays = retentionDays,
        onRetentionDaysSelected = { vm.setRetentionDays(it) },
        pollingIntervalHours = pollingIntervalHours,
        onPollingIntervalChanged = { vm.setPollingIntervalHours(it) },
        onClearReports = { vm.clearAllReports() },
        reportCount = reportCount,
    )
}

@Composable
fun WatcherSettingsScreen(
    onBack: () -> Unit,
    isPro: Boolean = true,
    isWatcherEnabled: Boolean = false,
    onWatcherEnabledChanged: (Boolean) -> Unit = {},
    onUpgrade: () -> Unit = {},
    watcherScope: WatcherScope = WatcherScope.NON_SYSTEM,
    onWatcherScopeSelected: (WatcherScope) -> Unit = {},
    isNotificationsEnabled: Boolean = true,
    onNotificationsEnabledChanged: (Boolean) -> Unit = {},
    isNotifyOnlyOnGained: Boolean = true,
    onNotifyOnlyOnGainedChanged: (Boolean) -> Unit = {},
    retentionDays: Int = 30,
    onRetentionDaysSelected: (Int) -> Unit = {},
    pollingIntervalHours: Int = 4,
    onPollingIntervalChanged: (Int) -> Unit = {},
    onClearReports: () -> Unit = {},
    reportCount: Int = 0,
) {
    var showScopeDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.watcher_settings_label)) },
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
            SettingsSwitchItem(
                icon = LucideRadar,
                title = stringResource(R.string.watcher_enabled_label),
                subtitle = stringResource(R.string.watcher_enabled_desc),
                checked = isWatcherEnabled,
                onCheckedChange = onWatcherEnabledChanged,
            )
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.watcher_scope_label),
                subtitle = when (watcherScope) {
                    WatcherScope.ALL -> stringResource(R.string.watcher_scope_all)
                    WatcherScope.NON_SYSTEM -> stringResource(R.string.watcher_scope_non_system)
                },
                icon = Icons.TwoTone.FilterList,
                onClick = if (isPro) {{ showScopeDialog = true }} else onUpgrade,
                trailingContent = if (!isPro) {{ SettingsUpgradeIcon() }} else null,
            )
            SettingsDivider()
            PollingIntervalItem(
                intervalHours = pollingIntervalHours,
                enabled = isWatcherEnabled,
                onIntervalChanged = onPollingIntervalChanged,
            )
            SettingsDivider()
            SettingsBaseItem(
                icon = Icons.TwoTone.Notifications,
                title = stringResource(R.string.watcher_settings_notifications_label),
                subtitle = stringResource(R.string.watcher_settings_notifications_desc),
                onClick = if (isPro) {{ onNotificationsEnabledChanged(!isNotificationsEnabled) }} else {{
                    onNotificationsEnabledChanged(false)
                    onUpgrade()
                }},
                trailingContent = if (isPro) {{
                    Switch(
                        checked = isNotificationsEnabled,
                        onCheckedChange = onNotificationsEnabledChanged,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }} else {{ SettingsUpgradeIcon() }},
            )
            if (isPro) {
                SettingsSwitchItem(
                    icon = Icons.TwoTone.Notifications,
                    title = stringResource(R.string.watcher_settings_notifications_only_gained_label),
                    subtitle = stringResource(R.string.watcher_settings_notifications_only_gained_desc),
                    checked = isNotifyOnlyOnGained,
                    onCheckedChange = onNotifyOnlyOnGainedChanged,
                    enabled = isNotificationsEnabled,
                )
            }
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.watcher_settings_retention_label),
                subtitle = pluralStringResource(R.plurals.watcher_settings_retention_days, retentionDays, retentionDays),
                icon = Icons.TwoTone.Schedule,
                onClick = { showRetentionDialog = true },
            )
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.watcher_settings_clear_reports_label),
                subtitle = pluralStringResource(R.plurals.watcher_settings_report_count, reportCount, reportCount),
                icon = Icons.TwoTone.DeleteSweep,
                onClick = { showClearConfirmDialog = true },
            )
        }
    }

    if (showScopeDialog) {
        SingleChoiceSortDialog(
            title = stringResource(R.string.watcher_scope_label),
            options = WatcherScope.entries.map {
                LabeledOption(
                    it, when (it) {
                        WatcherScope.ALL -> R.string.watcher_scope_all
                        WatcherScope.NON_SYSTEM -> R.string.watcher_scope_non_system
                    }
                )
            },
            selected = watcherScope,
            onSelect = {
                onWatcherScopeSelected(it)
                showScopeDialog = false
            },
            onDismiss = { showScopeDialog = false },
        )
    }

    if (showRetentionDialog) {
        SingleChoiceSortDialog(
            title = stringResource(R.string.watcher_settings_retention_label),
            options = listOf(7, 14, 30, 60, 90).map {
                LabeledOption(it, labelText = pluralStringResource(R.plurals.watcher_settings_retention_days, it, it))
            },
            selected = retentionDays,
            onSelect = {
                onRetentionDaysSelected(it)
                showRetentionDialog = false
            },
            onDismiss = { showRetentionDialog = false },
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.watcher_settings_clear_reports_confirm_title)) },
            text = { Text(stringResource(R.string.watcher_settings_clear_reports_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onClearReports()
                    showClearConfirmDialog = false
                }) {
                    Text(stringResource(R.string.general_done_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.general_cancel_action))
                }
            },
        )
    }
}

@Composable
private fun PollingIntervalItem(
    intervalHours: Int,
    enabled: Boolean,
    onIntervalChanged: (Int) -> Unit,
) {
    val alpha = if (enabled) 1f else 0.38f
    var sliderValue by remember(intervalHours) { mutableFloatStateOf(intervalHours.toFloat()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.TwoTone.Timer,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.watcher_settings_polling_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Text(
                text = pluralStringResource(R.plurals.watcher_settings_polling_interval, sliderValue.toInt(), sliderValue.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onIntervalChanged(sliderValue.toInt()) },
                valueRange = 2f..24f,
                steps = 21,
                enabled = enabled,
            )
        }
    }
}

@Preview2
@Composable
private fun WatcherSettingsScreenPreview() = PreviewWrapper {
    WatcherSettingsScreen(onBack = {})
}
