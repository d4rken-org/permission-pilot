package eu.darken.myperm.settings.ui.watcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.FilterList
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.LabeledOption
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.compose.SingleChoiceSortDialog
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.common.settings.SettingsBaseItem
import eu.darken.myperm.common.settings.SettingsDivider
import eu.darken.myperm.common.settings.SettingsSwitchItem
import eu.darken.myperm.watcher.core.WatcherScope

@Composable
fun WatcherSettingsScreenHost() {
    val navCtrl = LocalNavigationController.current
    val vm: WatcherSettingsViewModel = hiltViewModel()

    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val isWatcherEnabled by vm.isWatcherEnabled.collectAsState(initial = false)
    val watcherScope by vm.watcherScope.collectAsState(initial = WatcherScope.NON_SYSTEM)

    WatcherSettingsScreen(
        onBack = { navCtrl?.up() },
        isWatcherEnabled = isWatcherEnabled,
        watcherScope = watcherScope,
        onWatcherEnabledChanged = { vm.setWatcherEnabled(it) },
        onWatcherScopeSelected = { vm.setWatcherScope(it) },
        onViewReports = { vm.goToReports() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatcherSettingsScreen(
    onBack: () -> Unit,
    isWatcherEnabled: Boolean = false,
    watcherScope: WatcherScope = WatcherScope.NON_SYSTEM,
    onWatcherEnabledChanged: (Boolean) -> Unit = {},
    onWatcherScopeSelected: (WatcherScope) -> Unit = {},
    onViewReports: () -> Unit = {},
) {
    var showScopeDialog by remember { mutableStateOf(false) }

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
                icon = Icons.TwoTone.Notifications,
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
                enabled = isWatcherEnabled,
                onClick = { showScopeDialog = true },
            )
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.watcher_view_reports),
                subtitle = null,
                icon = Icons.TwoTone.History,
                onClick = onViewReports,
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
}

@Preview2
@Composable
private fun WatcherSettingsScreenPreview() = PreviewWrapper {
    WatcherSettingsScreen(onBack = {})
}
