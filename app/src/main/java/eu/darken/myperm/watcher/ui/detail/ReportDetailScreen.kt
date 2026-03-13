package eu.darken.myperm.watcher.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.watcher.core.PermissionDiff
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    state: ReportDetailViewModel.State,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.watcher_report_detail_label)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (state.isLoading) return@Column

            Text(
                text = state.appLabel ?: state.packageName,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = state.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val eventLabel = when (state.eventType) {
                "INSTALL" -> stringResource(R.string.watcher_event_install)
                "UPDATE" -> stringResource(R.string.watcher_event_update)
                "REMOVED" -> stringResource(R.string.watcher_event_removed)
                else -> state.eventType
            }
            Text(
                text = "$eventLabel${state.versionName?.let { " (v$it)" } ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(state.detectedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            DiffSection(state.diff)
        }
    }
}

@Composable
private fun DiffSection(diff: PermissionDiff) {
    if (diff.addedPermissions.isNotEmpty()) {
        SectionHeader(stringResource(R.string.watcher_diff_added_permissions))
        diff.addedPermissions.forEach { PermissionRow(it) }
    }
    if (diff.removedPermissions.isNotEmpty()) {
        SectionHeader(stringResource(R.string.watcher_diff_removed_permissions))
        diff.removedPermissions.forEach { PermissionRow(it) }
    }
    if (diff.grantChanges.isNotEmpty()) {
        SectionHeader(stringResource(R.string.watcher_diff_grant_changes))
        diff.grantChanges.forEach { change ->
            Text(
                text = stringResource(
                    R.string.watcher_diff_grant_change_detail,
                    change.permissionId.substringAfterLast('.'),
                    change.oldStatus,
                    change.newStatus,
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            )
        }
    }
    if (diff.addedDeclared.isNotEmpty()) {
        SectionHeader(stringResource(R.string.watcher_diff_added_declared))
        diff.addedDeclared.forEach { PermissionRow(it) }
    }
    if (diff.removedDeclared.isNotEmpty()) {
        SectionHeader(stringResource(R.string.watcher_diff_removed_declared))
        diff.removedDeclared.forEach { PermissionRow(it) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun PermissionRow(permissionId: String) {
    Text(
        text = permissionId.substringAfterLast('.'),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    )
}
