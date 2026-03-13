package eu.darken.myperm.watcher.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.compose.AppIcon
import eu.darken.myperm.common.compose.LucideRadar
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler
import java.text.DateFormat
import java.util.Date

@Composable
fun WatcherDashboardScreenHost(vm: WatcherDashboardViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()

    WatcherDashboardScreen(
        state = state,
        onToggle = { vm.toggleWatcher() },
        onReportClicked = { vm.onReportClicked(it) },
        onMarkAllSeen = { vm.markAllSeen() },
        onSettings = { vm.goToSettings() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatcherDashboardScreen(
    state: WatcherDashboardViewModel.State?,
    onToggle: () -> Unit,
    onReportClicked: (WatcherDashboardViewModel.ReportItem) -> Unit,
    onMarkAllSeen: () -> Unit,
    onSettings: () -> Unit,
) {
    val isEnabled = state?.isWatcherEnabled ?: false
    val reports = state?.reports ?: emptyList()
    val hasUnseen = reports.any { !it.isSeen }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.watcher_tab_label)) },
                actions = {
                    if (isEnabled && hasUnseen) {
                        IconButton(onClick = onMarkAllSeen) {
                            Icon(Icons.Filled.DoneAll, contentDescription = stringResource(R.string.watcher_mark_all_seen))
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_page_label))
                    }
                },
            )
        }
    ) { innerPadding ->
        if (!isEnabled) {
            DisabledContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                isPro = state?.isPro ?: false,
                onToggle = onToggle,
            )
        } else {
            EnabledContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                reports = reports,
                onReportClicked = onReportClicked,
            )
        }
    }
}

@Composable
private fun DisabledContent(
    modifier: Modifier = Modifier,
    isPro: Boolean,
    onToggle: () -> Unit,
) {
    Column(modifier = modifier) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = LucideRadar,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.watcher_dashboard_feature_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Text(
                    text = stringResource(R.string.watcher_dashboard_feature_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.watcher_dashboard_manage_in_settings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Button(
                    onClick = onToggle,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = if (isPro) {
                            stringResource(R.string.watcher_enable_action)
                        } else {
                            stringResource(R.string.general_upgrade_action)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnabledContent(
    modifier: Modifier = Modifier,
    reports: List<WatcherDashboardViewModel.ReportItem>,
    onReportClicked: (WatcherDashboardViewModel.ReportItem) -> Unit,
) {
    if (reports.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.watcher_reports_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(reports, key = { it.id }) { item ->
                ReportListItem(
                    item = item,
                    onClick = { onReportClicked(item) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ReportListItem(
    item: WatcherDashboardViewModel.ReportItem,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(
                pkg = Pkg.Container(Pkg.Id(item.packageName)),
                isSystemApp = false,
                modifier = Modifier.size(40.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.appLabel ?: item.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val eventLabel = when (item.eventType) {
                        "INSTALL" -> stringResource(R.string.watcher_event_install)
                        "UPDATE" -> stringResource(R.string.watcher_event_update)
                        "REMOVED" -> stringResource(R.string.watcher_event_removed)
                        else -> item.eventType
                    }
                    Text(
                        text = eventLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                            .format(Date(item.detectedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!item.isSeen) {
            Icon(
                imageVector = Icons.Filled.FiberNew,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview2
@Composable
private fun WatcherDashboardWithReportsPreview() = PreviewWrapper {
    WatcherDashboardScreen(
        state = WatcherDashboardViewModel.State(
            isWatcherEnabled = true,
            isPro = true,
            reports = listOf(
                WatcherDashboardViewModel.ReportItem(
                    id = 1,
                    packageName = "com.example.app",
                    appLabel = "Example App",
                    versionName = "2.1.0",
                    previousVersionName = "1.8.3",
                    eventType = "UPDATE",
                    detectedAt = System.currentTimeMillis(),
                    isSeen = false,
                ),
                WatcherDashboardViewModel.ReportItem(
                    id = 2,
                    packageName = "com.example.browser",
                    appLabel = "My Browser",
                    versionName = "4.0.1",
                    previousVersionName = null,
                    eventType = "INSTALL",
                    detectedAt = System.currentTimeMillis() - 86400000,
                    isSeen = true,
                ),
            ),
        ),
        onToggle = {},
        onReportClicked = {},
        onMarkAllSeen = {},
        onSettings = {},
    )
}

@Preview2
@Composable
private fun WatcherDashboardDisabledPreview() = PreviewWrapper {
    WatcherDashboardScreen(
        state = WatcherDashboardViewModel.State(isWatcherEnabled = false, isPro = false),
        onToggle = {},
        onReportClicked = {},
        onMarkAllSeen = {},
        onSettings = {},
    )
}

@Preview2
@Composable
private fun WatcherDashboardDisabledProPreview() = PreviewWrapper {
    WatcherDashboardScreen(
        state = WatcherDashboardViewModel.State(isWatcherEnabled = false, isPro = true),
        onToggle = {},
        onReportClicked = {},
        onMarkAllSeen = {},
        onSettings = {},
    )
}

@Preview2
@Composable
private fun WatcherDashboardEmptyPreview() = PreviewWrapper {
    WatcherDashboardScreen(
        state = WatcherDashboardViewModel.State(isWatcherEnabled = true, isPro = true),
        onToggle = {},
        onReportClicked = {},
        onMarkAllSeen = {},
        onSettings = {},
    )
}
