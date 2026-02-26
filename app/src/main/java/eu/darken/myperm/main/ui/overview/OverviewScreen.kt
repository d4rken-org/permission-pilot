package eu.darken.myperm.main.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
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
                val summary = state.summaryInfo
                val device = state.deviceInfo
                if (summary != null) {
                    HeroCard(summary, device)
                    SummaryList(summary)
                }
            }
        }
    }
}

@Composable
private fun HeroCard(summary: OverviewViewModel.SummaryInfo, device: OverviewViewModel.DeviceInfo?) {
    val total = summary.activeProfileUser + summary.activeProfileSystem
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.overview_summary_apps_active_profile_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppRatioBar(summary.activeProfileUser, summary.activeProfileSystem, total)
            if (device != null) {
                Text(
                    text = "${device.deviceName} \u00B7 ${device.androidVersion} \u00B7 ${device.patchLevel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AppRatioBar(userCount: Int, systemCount: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LinearProgressIndicator(
            progress = { if (total > 0) userCount.toFloat() / total else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = pluralStringResource(R.plurals.generic_x_apps_user_label, userCount, userCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = pluralStringResource(R.plurals.generic_x_apps_system_label, systemCount, systemCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryList(summary: OverviewViewModel.SummaryInfo) {
    data class Category(val icon: ImageVector, val label: String, val userCount: Int, val systemCount: Int)

    val categories = listOf(
        Category(Icons.Filled.PhoneAndroid, stringResource(R.string.overview_summary_apps_active_profile_label), summary.activeProfileUser, summary.activeProfileSystem),
        Category(Icons.Filled.People, stringResource(R.string.overview_summary_apps_other_profile_label), summary.otherProfileUser, summary.otherProfileSystem),
        Category(Icons.Filled.InstallMobile, stringResource(R.string.overview_summary_apps_sideloaded_label), summary.sideloaded, 0),
        Category(Icons.Filled.GetApp, stringResource(R.string.overview_summary_apps_installers_label), summary.installerAppsUser, summary.installerAppsSystem),
        Category(Icons.Filled.Layers, stringResource(R.string.overview_summary_apps_overlayers_label), summary.systemAlertWindowUser, summary.systemAlertWindowSystem),
        Category(Icons.Filled.WifiOff, stringResource(R.string.overview_summary_apps_offline_label), summary.noInternetUser, summary.noInternetSystem),
        Category(Icons.Filled.ContentCopy, stringResource(R.string.overview_summary_apps_clones_label), summary.clonesUser, summary.clonesSystem),
        Category(Icons.Filled.Share, stringResource(R.string.overview_summary_apps_sharedids_label), summary.sharedIdsUser, summary.sharedIdsSystem),
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            categories.forEachIndexed { index, category ->
                CategoryRow(category.icon, category.label, category.userCount, category.systemCount)
                if (index < categories.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 48.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(icon: ImageVector, label: String, userCount: Int, systemCount: Int) {
    val total = userCount + systemCount
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (systemCount > 0) {
                val userText = pluralStringResource(R.plurals.generic_x_apps_user_label, userCount, userCount)
                val systemText = pluralStringResource(R.plurals.generic_x_apps_system_label, systemCount, systemCount)
                Text(
                    text = "$userText \u00B7 $systemText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CountPill(total)
    }
}

@Composable
private fun CountPill(count: Int) {
    Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Preview2
@Composable
private fun OverviewScreenPreview() = PreviewWrapper {
    OverviewScreen(
        state = OverviewPreviewData.loadedState(),
        onRefresh = {},
        onSettings = {},
    )
}

@Preview2
@Composable
private fun OverviewScreenLoadingPreview() = PreviewWrapper {
    OverviewScreen(
        state = OverviewPreviewData.loadingState(),
        onRefresh = {},
        onSettings = {},
    )
}
