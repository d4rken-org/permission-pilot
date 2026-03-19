package eu.darken.myperm.watcher.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.compose.AppIcon
import eu.darken.myperm.common.compose.Pill
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.watcher.core.PermissionDiff
import eu.darken.myperm.watcher.core.WatcherEventType
import java.text.DateFormat
import java.util.Date

@Composable
fun ReportDetailScreenHost(
    route: Nav.Watcher.ReportDetail,
    vm: ReportDetailViewModel = hiltViewModel(
        key = "report_${route.reportId}",
        creationCallback = { factory: ReportDetailViewModel.Factory -> factory.create(route.reportId) },
    ),
) {
    val navCtrl = LocalNavigationController.current
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()

    ReportDetailScreen(
        state = state,
        onBack = { navCtrl?.up() },
        onViewApp = { vm.onViewApp() },
    )
}

@Composable
fun ReportDetailScreen(
    state: ReportDetailViewModel.State,
    onBack: () -> Unit,
    onViewApp: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.watcher_report_detail_label))
                        if (!state.isLoading && state.detectedAt > 0) {
                            Text(
                                text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date(state.detectedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!state.isLoading && state.packageName.value.isNotEmpty()) {
                        IconButton(onClick = onViewApp) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = stringResource(R.string.apps_app_details_label),
                            )
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppHeaderCard(state)
                EventDetailsCard(state)
                if (state.diff != null) {
                    PermissionCards(state.diff, state.eventType, state.permissionInfoMap)
                } else {
                    DiffErrorCard()
                }
            }
        }
    }
}

// Section 1: App Header

@Composable
private fun AppHeaderCard(state: ReportDetailViewModel.State) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.eventType == WatcherEventType.REMOVED) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                AppIcon(
                    pkg = Pkg.Container(Pkg.Id(state.packageName)),
                    isSystemApp = state.isSystemApp,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.appLabel ?: state.packageName.value,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = state.packageName.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(6.dp))

                EventTypePill(state.eventType)
            }
        }
    }
}

@Composable
private fun EventTypePill(eventType: WatcherEventType) {
    val (containerColor, contentColor, label) = when (eventType) {
        WatcherEventType.INSTALL -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            stringResource(R.string.watcher_event_install),
        )
        WatcherEventType.UPDATE -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            stringResource(R.string.watcher_event_update),
        )
        WatcherEventType.REMOVED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            stringResource(R.string.watcher_event_removed),
        )
        WatcherEventType.GRANT_CHANGE -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            stringResource(R.string.watcher_event_grant_change),
        )
    }

    Pill(text = label, containerColor = containerColor, contentColor = contentColor)
}

// Section 2: Event Details

@Composable
private fun EventDetailsCard(state: ReportDetailViewModel.State) {
    val hasContent = when (state.eventType) {
        WatcherEventType.INSTALL, WatcherEventType.UPDATE -> true
        WatcherEventType.REMOVED -> buildVersionText(state.versionName, state.versionCode) != null
        WatcherEventType.GRANT_CHANGE -> false
    }
    if (!hasContent) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (state.eventType) {
                WatcherEventType.INSTALL -> {
                    val versionText = buildVersionText(state.versionName, state.versionCode)
                    if (versionText != null) {
                        DetailRow(stringResource(R.string.watcher_detail_version_label), versionText)
                    }
                    DetailRow(
                        stringResource(R.string.watcher_detail_installer_label),
                        state.installerLabel ?: stringResource(R.string.watcher_detail_installer_unknown),
                    )
                }
                WatcherEventType.UPDATE -> {
                    if (state.previousVersionName != null || state.versionName != null) {
                        val fromVersion = state.previousVersionName ?: "?"
                        val toVersion = state.versionName ?: "?"
                        DetailRow(stringResource(R.string.watcher_detail_version_label), "$fromVersion \u2192 $toVersion")
                    }
                    if (state.previousVersionCode != null || state.versionCode != null) {
                        val fromCode = state.previousVersionCode?.toString() ?: "?"
                        val toCode = state.versionCode?.toString() ?: "?"
                        DetailRow(stringResource(R.string.watcher_detail_version_code_label), "$fromCode \u2192 $toCode")
                    }
                }
                WatcherEventType.REMOVED -> {
                    val versionText = buildVersionText(state.versionName, state.versionCode)
                    if (versionText != null) {
                        DetailRow(stringResource(R.string.watcher_detail_version_label), versionText)
                    }
                }
                WatcherEventType.GRANT_CHANGE -> Unit
            }
        }
    }
}

private fun buildVersionText(versionName: String?, versionCode: Long?): String? {
    if (versionName == null && versionCode == null) return null
    return when {
        versionName != null && versionCode != null -> "$versionName ($versionCode)"
        versionName != null -> versionName
        else -> "($versionCode)"
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// Section 3: Permissions

@Composable
private fun DiffErrorCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(R.string.watcher_detail_diff_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Preview
@Composable
private fun ReportDetailUpdatePreview() {
    MaterialTheme {
        ReportDetailScreen(
            state = ReportDetailViewModel.State(
                isLoading = false,
                packageName = Pkg.Name("com.example.app"),
                appLabel = "Example App",
                eventType = WatcherEventType.UPDATE,
                versionName = "2.1.0",
                previousVersionName = "2.0.3",
                versionCode = 210,
                previousVersionCode = 203,
                detectedAt = System.currentTimeMillis(),
                diff = PermissionDiff(
                    addedPermissions = listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO"),
                    removedPermissions = listOf("android.permission.READ_CONTACTS"),
                    grantChanges = listOf(
                        PermissionDiff.GrantChange("android.permission.LOCATION", UsesPermission.Status.DENIED, UsesPermission.Status.GRANTED),
                    ),
                ),
            ),
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ReportDetailInstallPreview() {
    MaterialTheme {
        ReportDetailScreen(
            state = ReportDetailViewModel.State(
                isLoading = false,
                packageName = Pkg.Name("com.example.newapp"),
                appLabel = "New App",
                eventType = WatcherEventType.INSTALL,
                versionName = "1.0.0",
                versionCode = 100,
                installerLabel = "Google Play Store",
                detectedAt = System.currentTimeMillis(),
                diff = PermissionDiff(
                    addedPermissions = listOf("android.permission.INTERNET", "android.permission.CAMERA"),
                ),
            ),
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ReportDetailRemovedPreview() {
    MaterialTheme {
        ReportDetailScreen(
            state = ReportDetailViewModel.State(
                isLoading = false,
                packageName = Pkg.Name("com.example.oldapp"),
                appLabel = "Old App",
                eventType = WatcherEventType.REMOVED,
                versionName = "3.2.1",
                versionCode = 321,
                detectedAt = System.currentTimeMillis(),
                diff = PermissionDiff(
                    addedPermissions = listOf("android.permission.INTERNET"),
                ),
            ),
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun ReportDetailGrantChangePreview() {
    MaterialTheme {
        ReportDetailScreen(
            state = ReportDetailViewModel.State(
                isLoading = false,
                packageName = Pkg.Name("com.example.app"),
                appLabel = "Some App",
                eventType = WatcherEventType.GRANT_CHANGE,
                detectedAt = System.currentTimeMillis(),
                diff = PermissionDiff(
                    grantChanges = listOf(
                        PermissionDiff.GrantChange("android.permission.CAMERA", UsesPermission.Status.DENIED, UsesPermission.Status.GRANTED),
                        PermissionDiff.GrantChange("android.permission.LOCATION", UsesPermission.Status.GRANTED, UsesPermission.Status.DENIED),
                    ),
                ),
            ),
            onBack = {},
        )
    }
}
