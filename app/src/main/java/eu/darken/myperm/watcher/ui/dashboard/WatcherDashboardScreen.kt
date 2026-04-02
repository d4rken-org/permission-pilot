package eu.darken.myperm.watcher.ui.dashboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.compose.AppIcon
import eu.darken.myperm.common.compose.LucideRadar
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.SearchTextField
import eu.darken.myperm.common.compose.rememberFabVisibility
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.watcher.core.WatcherEventType
import eu.darken.myperm.watcher.core.WatcherManager
import java.text.DateFormat
import java.util.Date

@Composable
fun WatcherDashboardScreenHost(vm: WatcherDashboardViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var useSettingsFallback by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            val activity = context as? android.app.Activity
            if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.POST_NOTIFICATIONS
                )
                if (!showRationale) useSettingsFallback = true
            }
        }
        vm.refreshNotificationState()
    }

    LifecycleResumeEffect(Unit) {
        vm.refreshNotificationState()
        vm.refreshBatteryState()
        onPauseOrDispose {}
    }

    val canRequest = state?.canRequestNotificationPermission ?: false

    val onGrantNotificationPermission: () -> Unit = {
        if (canRequest && !useSettingsFallback && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    }

    val onOpenBatterySettings: () -> Unit = {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }.onFailure {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        }
    }

    WatcherDashboardScreen(
        state = state,
        onToggle = { vm.toggleWatcher() },
        onRefresh = { vm.refreshNow() },
        onReportClicked = { vm.onReportClicked(it) },
        onMarkAllSeen = { vm.markAllSeen() },
        onSettings = { vm.goToSettings() },
        onSearchChanged = { vm.onSearchInputChanged(it) },
        onFilter = { showFilterSheet = true },
        onGrantNotificationPermission = onGrantNotificationPermission,
        onDisableNotifications = { vm.disableNotifications() },
        onOpenBatterySettings = onOpenBatterySettings,
        onDismissBatteryHint = { vm.dismissBatteryHint() },
        onUpgrade = { vm.goToUpgrade() },
        useSettingsFallback = useSettingsFallback,
    )

    if (showFilterSheet) {
        WatcherFilterBottomSheet(
            currentOptions = state?.filterOptions ?: WatcherFilterOptions(),
            onFilterChanged = { newOptions ->
                vm.updateFilterOptions { newOptions }
            },
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
fun WatcherDashboardScreen(
    state: WatcherDashboardViewModel.State?,
    onToggle: () -> Unit,
    onRefresh: () -> Unit = {},
    onReportClicked: (WatcherReportItem) -> Unit,
    onMarkAllSeen: () -> Unit,
    onSettings: () -> Unit,
    onSearchChanged: (String?) -> Unit,
    onFilter: () -> Unit,
    onGrantNotificationPermission: () -> Unit = {},
    onDisableNotifications: () -> Unit = {},
    onOpenBatterySettings: () -> Unit = {},
    onDismissBatteryHint: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    useSettingsFallback: Boolean = false,
) {
    val isEnabled = state?.isWatcherEnabled ?: false
    val refreshPhase = state?.refreshPhase
    val reports = state?.reports ?: emptyList()
    val hasUnseen = state?.hasUnseen ?: false
    val hasActiveFilters = state?.filterOptions?.filters?.isNotEmpty() == true

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val (fabVisible, scrollConnection) = rememberFabVisibility()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollConnection),
        floatingActionButton = {
            if (isEnabled) {
                AnimatedVisibility(
                    visible = fabVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    FloatingActionButton(
                        onClick = { if (refreshPhase == null) onRefresh() },
                    ) {
                        if (refreshPhase != null) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.general_refresh_action))
                        }
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.watcher_tab_label))
                        if (refreshPhase != null) {
                            val subtitleRes = when (refreshPhase) {
                                WatcherManager.Phase.SCANNING -> R.string.watcher_refresh_scanning
                                WatcherManager.Phase.CHECKING_CHANGES -> R.string.watcher_refresh_checking
                            }
                            Text(
                                text = stringResource(subtitleRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (isEnabled) {
                        IconButton(onClick = {
                            if (isSearchActive) {
                                searchQuery = ""
                                onSearchChanged(null)
                            }
                            isSearchActive = !isSearchActive
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.watcher_search_hint))
                        }
                        IconButton(onClick = onFilter) {
                            Box {
                                Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.general_filter_action))
                                if (hasActiveFilters) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    )
                                }
                            }
                        }
                    }
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
                onToggle = onToggle,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    SearchTextField(
                        query = searchQuery,
                        onQueryChanged = {
                            searchQuery = it
                            onSearchChanged(it.ifBlank { null })
                        },
                        placeholder = stringResource(R.string.watcher_search_hint),
                    )
                }
                EnabledContent(
                    modifier = Modifier.fillMaxSize(),
                    reports = reports,
                    totalReportCount = state?.totalReportCount ?: 0,
                    lockedReportCount = state?.lockedReportCount ?: 0,
                    onReportClicked = onReportClicked,
                    onUpgrade = onUpgrade,
                    showNotificationPermissionCard = state?.showNotificationPermissionCard ?: false,
                    canRequestNotificationPermission = state?.canRequestNotificationPermission ?: false,
                    useSettingsFallback = useSettingsFallback,
                    onGrantNotificationPermission = onGrantNotificationPermission,
                    onDisableNotifications = onDisableNotifications,
                    showBatteryOptimizationCard = state?.showBatteryOptimizationCard ?: false,
                    onOpenBatterySettings = onOpenBatterySettings,
                    onDismissBatteryHint = onDismissBatteryHint,
                )
            }
        }
    }
}

@Composable
private fun DisabledContent(
    modifier: Modifier = Modifier,
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
                    Text(text = stringResource(R.string.watcher_enable_action))
                }
            }
        }
    }
}

@Composable
private fun EnabledContent(
    modifier: Modifier = Modifier,
    reports: List<WatcherReportItem>,
    totalReportCount: Int,
    lockedReportCount: Int = 0,
    onReportClicked: (WatcherReportItem) -> Unit,
    onUpgrade: () -> Unit = {},
    showNotificationPermissionCard: Boolean = false,
    canRequestNotificationPermission: Boolean = false,
    useSettingsFallback: Boolean = false,
    onGrantNotificationPermission: () -> Unit = {},
    onDisableNotifications: () -> Unit = {},
    showBatteryOptimizationCard: Boolean = false,
    onOpenBatterySettings: () -> Unit = {},
    onDismissBatteryHint: () -> Unit = {},
) {
    LazyColumn(modifier = modifier) {
        if (showNotificationPermissionCard) {
            item(key = "notification_permission") {
                NotificationPermissionCard(
                    canRequestPermission = canRequestNotificationPermission && !useSettingsFallback,
                    onAction = onGrantNotificationPermission,
                    onDisable = onDisableNotifications,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        if (showBatteryOptimizationCard) {
            item(key = "battery_optimization") {
                BatteryOptimizationCard(
                    onOpenSettings = onOpenBatterySettings,
                    onDismiss = onDismissBatteryHint,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        val hasInfoCards = showNotificationPermissionCard || showBatteryOptimizationCard
        if (reports.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .then(
                            if (hasInfoCards) Modifier.fillMaxWidth()
                            else Modifier.fillParentMaxSize()
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (totalReportCount > 0) {
                            stringResource(R.string.watcher_reports_no_matches)
                        } else {
                            stringResource(R.string.watcher_reports_empty)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(reports, key = { it.id }) { item ->
                ReportListItem(
                    item = item,
                    onClick = { onReportClicked(item) },
                )
                HorizontalDivider()
            }
        }
        if (lockedReportCount > 0) {
            item(key = "upgrade_card") {
                UpgradeCard(
                    lockedReportCount = lockedReportCount,
                    onUpgrade = onUpgrade,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun NotificationPermissionCard(
    canRequestPermission: Boolean,
    onAction: () -> Unit,
    onDisable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
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
                    imageVector = Icons.Filled.NotificationsOff,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.watcher_notification_permission_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.watcher_notification_permission_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onDisable) {
                    Text(text = stringResource(R.string.watcher_notification_permission_disable))
                }
                Button(onClick = onAction) {
                    Text(
                        text = if (canRequestPermission) {
                            stringResource(R.string.watcher_notification_permission_grant)
                        } else {
                            stringResource(R.string.watcher_notification_permission_open_settings)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryOptimizationCard(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
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
                    imageVector = Icons.Filled.BatteryAlert,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.watcher_battery_optimization_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.watcher_battery_optimization_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.watcher_battery_optimization_dismiss))
                }
                Button(onClick = onOpenSettings) {
                    Text(text = stringResource(R.string.watcher_battery_optimization_open_settings))
                }
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    lockedReportCount: Int,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = pluralStringResource(R.plurals.watcher_upgrade_card_title, lockedReportCount, lockedReportCount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.watcher_upgrade_card_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Button(
                onClick = onUpgrade,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = stringResource(R.string.general_upgrade_action))
            }
        }
    }
}

@Composable
private fun ReportListItem(
    item: WatcherReportItem,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    var textBlockHeight by remember { mutableIntStateOf(0) }
    val iconSize = with(density) { if (textBlockHeight > 0) textBlockHeight.toDp() else 40.dp }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (item.eventType == WatcherEventType.REMOVED) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                AppIcon(
                    pkg = Pkg.Container(Pkg.Id(item.packageName)),
                    isSystemApp = false,
                    modifier = Modifier.size(iconSize),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { textBlockHeight = it.height },
            ) {
                Text(
                    text = item.appLabel ?: item.packageName.value,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.showPkgName || item.appLabel == null) {
                    Text(
                        text = item.packageName.value,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val eventLabel = when (item.eventType) {
                        WatcherEventType.INSTALL -> stringResource(R.string.watcher_event_install)
                        WatcherEventType.UPDATE -> stringResource(R.string.watcher_event_update)
                        WatcherEventType.REMOVED -> stringResource(R.string.watcher_event_removed)
                        WatcherEventType.GRANT_CHANGE -> stringResource(R.string.watcher_event_grant_change)
                    }
                    val countSuffix = when {
                        item.gainedCount > 0 && item.lostCount > 0 -> "(+${item.gainedCount}, -${item.lostCount})"
                        item.gainedCount > 0 -> "(+${item.gainedCount})"
                        item.lostCount > 0 -> "(-${item.lostCount})"
                        else -> null
                    }
                    val displayLabel = if (countSuffix != null) "$eventLabel $countSuffix" else eventLabel
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.gainedCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
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
                WatcherReportItem(
                    id = 1,
                    packageName = Pkg.Name("com.example.app"),
                    appLabel = "Example App",
                    versionName = "2.1.0",
                    previousVersionName = "1.8.3",
                    eventType = WatcherEventType.UPDATE,
                    detectedAt = System.currentTimeMillis(),
                    isSeen = false,
                    hasAddedPermissions = true,
                    hasLostPermissions = false,
                    gainedCount = 2,
                ),
                WatcherReportItem(
                    id = 2,
                    packageName = Pkg.Name("com.example.browser"),
                    appLabel = "My Browser",
                    versionName = "4.0.1",
                    previousVersionName = null,
                    eventType = WatcherEventType.INSTALL,
                    detectedAt = System.currentTimeMillis() - 86400000,
                    isSeen = true,
                    hasAddedPermissions = false,
                    hasLostPermissions = false,
                    lostCount = 1,
                ),
            ),
            hasUnseen = true,
            totalReportCount = 2,
        ),
        onToggle = {},
        onRefresh = {},
        onReportClicked = {},
        onMarkAllSeen = {},
        onSettings = {},
        onSearchChanged = {},
        onFilter = {},
    )
}

@Preview2
@Composable
private fun WatcherDashboardNotificationCardPreview() = PreviewWrapper {
    WatcherDashboardScreen(
        state = WatcherDashboardViewModel.State(
            isWatcherEnabled = true,
            isPro = true,
            showNotificationPermissionCard = true,
            canRequestNotificationPermission = true,
            reports = listOf(
                WatcherReportItem(
                    id = 1,
                    packageName = Pkg.Name("com.example.app"),
                    appLabel = "Example App",
                    versionName = "2.1.0",
                    previousVersionName = "1.8.3",
                    eventType = WatcherEventType.UPDATE,
                    detectedAt = System.currentTimeMillis(),
                    isSeen = false,
                    hasAddedPermissions = true,
                    hasLostPermissions = false,
                    gainedCount = 3,
                    lostCount = 1,
                ),
            ),
            hasUnseen = true,
            totalReportCount = 1,
        ),
        onToggle = {},
        onRefresh = {},
        onReportClicked = {},
        onMarkAllSeen = {},
        onSettings = {},
        onSearchChanged = {},
        onFilter = {},
    )
}

@Preview2
@Composable
private fun WatcherDashboardDisabledPreview() = PreviewWrapper {
    WatcherDashboardScreen(
        state = WatcherDashboardViewModel.State(isWatcherEnabled = false, isPro = false),
        onToggle = {},
        onRefresh = {},
        onReportClicked = {},
        onMarkAllSeen = {},
        onSettings = {},
        onSearchChanged = {},
        onFilter = {},
    )
}

@Preview2
@Composable
private fun WatcherDashboardDisabledProPreview() = PreviewWrapper {
    WatcherDashboardScreen(
        state = WatcherDashboardViewModel.State(isWatcherEnabled = false, isPro = true),
        onToggle = {},
        onRefresh = {},
        onReportClicked = {},
        onMarkAllSeen = {},
        onSettings = {},
        onSearchChanged = {},
        onFilter = {},
    )
}

@Preview2
@Composable
private fun WatcherDashboardEmptyPreview() = PreviewWrapper {
    WatcherDashboardScreen(
        state = WatcherDashboardViewModel.State(isWatcherEnabled = true, isPro = true),
        onToggle = {},
        onRefresh = {},
        onReportClicked = {},
        onMarkAllSeen = {},
        onSettings = {},
        onSearchChanged = {},
        onFilter = {},
    )
}
