package eu.darken.myperm.main.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.waitForState
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler

@Composable
fun OverviewScreenHost(vm: OverviewViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by waitForState(vm.state)

    state?.let {
        OverviewScreen(
            state = it,
            onRefresh = { vm.onRefresh() },
            onSettings = { vm.goToSettings() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    state: OverviewViewModel.State,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.app_name))
                        Text(
                            text = state.versionDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.general_refresh_action))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_page_label))
                    }
                }
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
                state.deviceInfo?.let { device ->
                    DeviceInfoCard(device)
                }
                state.summaryInfo?.let { summary ->
                    SummaryCard(summary)
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(device: OverviewViewModel.DeviceInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.overview_device_name_label),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = device.deviceName, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.overview_device_android_version_label),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = device.androidVersion, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.overview_device_android_patch_label),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = device.patchLevel, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SummaryCard(summary: OverviewViewModel.SummaryInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.overview_summary_title_label),
                style = MaterialTheme.typography.titleMedium,
            )
            SummaryRow(stringResource(R.string.overview_summary_apps_active_profile_label), summary.activeProfileUser, summary.activeProfileSystem)
            SummaryRow(stringResource(R.string.overview_summary_apps_other_profile_label), summary.otherProfileUser, summary.otherProfileSystem)
            SummaryRow(stringResource(R.string.overview_summary_apps_sideloaded_label), summary.sideloaded, 0)
            SummaryRow(stringResource(R.string.overview_summary_apps_installers_label), summary.installerAppsUser, summary.installerAppsSystem)
            SummaryRow(stringResource(R.string.overview_summary_apps_overlayers_label), summary.systemAlertWindowUser, summary.systemAlertWindowSystem)
            SummaryRow(stringResource(R.string.overview_summary_apps_offline_label), summary.noInternetUser, summary.noInternetSystem)
            SummaryRow(stringResource(R.string.overview_summary_apps_clones_label), summary.clonesUser, summary.clonesSystem)
            SummaryRow(stringResource(R.string.overview_summary_apps_sharedids_label), summary.sharedIdsUser, summary.sharedIdsSystem)
        }
    }
}

@Composable
private fun SummaryRow(label: String, userCount: Int, systemCount: Int) {
    val total = userCount + systemCount
    val totalText = pluralStringResource(R.plurals.generic_x_apps_label, total, total)
    val userText = pluralStringResource(R.plurals.generic_x_apps_user_label, userCount, userCount)
    val systemText = pluralStringResource(R.plurals.generic_x_apps_system_label, systemCount, systemCount)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$totalText ($userText, $systemText)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
