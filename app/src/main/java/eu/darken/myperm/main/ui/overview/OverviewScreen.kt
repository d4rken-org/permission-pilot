package eu.darken.myperm.main.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FloatingActionButton
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
import eu.darken.myperm.common.compose.LoadingContent
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import androidx.compose.runtime.collectAsState
import eu.darken.myperm.apps.ui.list.AppsFilterOptions
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler

@Composable
fun OverviewScreenHost(vm: OverviewViewModel = hiltViewModel()) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val state by vm.state.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()

    state?.let {
        OverviewScreen(
            state = it,
            isRefreshing = isRefreshing,
            onRefresh = { vm.onRefresh() },
            onSettings = { vm.goToSettings() },
            onCategoryClick = { filters -> vm.onCategoryClicked(filters) },
        )
    }
}

@Composable
fun OverviewScreen(
    state: OverviewViewModel.State,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onCategoryClick: (Set<AppsFilterOptions.Filter>) -> Unit = {},
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!isRefreshing) onRefresh() },
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.general_refresh_action))
                }
            }
        },
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
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_page_label))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingContent(modifier = Modifier.padding(innerPadding))
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
                    SummaryList(summary, onCategoryClick)
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
private fun SummaryList(
    summary: OverviewViewModel.SummaryInfo,
    onCategoryClick: (Set<AppsFilterOptions.Filter>) -> Unit,
) {
    data class Category(
        val icon: ImageVector,
        val label: String,
        val userCount: Int,
        val systemCount: Int,
        val filters: Set<AppsFilterOptions.Filter>,
    )

    val categories = listOf(
        Category(Icons.Filled.PhoneAndroid, stringResource(R.string.overview_summary_apps_active_profile_label), summary.activeProfileUser, summary.activeProfileSystem, setOf(AppsFilterOptions.Filter.PRIMARY_PROFILE)),
        Category(Icons.Filled.People, stringResource(R.string.overview_summary_apps_other_profile_label), summary.otherProfileUser, summary.otherProfileSystem, setOf(AppsFilterOptions.Filter.SECONDARY_PROFILE)),
        Category(Icons.Filled.InstallMobile, stringResource(R.string.overview_summary_apps_sideloaded_label), summary.sideloaded, 0, setOf(AppsFilterOptions.Filter.SIDELOADED)),
        Category(Icons.Filled.GetApp, stringResource(R.string.overview_summary_apps_installers_label), summary.installerAppsUser, summary.installerAppsSystem, setOf(AppsFilterOptions.Filter.INSTALL_PACKAGES)),
        Category(Icons.Filled.Layers, stringResource(R.string.overview_summary_apps_overlayers_label), summary.systemAlertWindowUser, summary.systemAlertWindowSystem, setOf(AppsFilterOptions.Filter.OVERLAY)),
        Category(Icons.Filled.WifiOff, stringResource(R.string.overview_summary_apps_offline_label), summary.noInternetUser, summary.noInternetSystem, setOf(AppsFilterOptions.Filter.NO_INTERNET)),
        Category(Icons.Filled.ContentCopy, stringResource(R.string.overview_summary_apps_clones_label), summary.clonesUser, summary.clonesSystem, setOf(AppsFilterOptions.Filter.MULTI_PROFILE)),
        Category(Icons.Filled.Share, stringResource(R.string.overview_summary_apps_sharedids_label), summary.sharedIdsUser, summary.sharedIdsSystem, setOf(AppsFilterOptions.Filter.SHARED_ID)),
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            categories.forEachIndexed { index, category ->
                CategoryRow(
                    icon = category.icon,
                    label = category.label,
                    userCount = category.userCount,
                    systemCount = category.systemCount,
                    onClick = { onCategoryClick(category.filters) },
                )
                if (index < categories.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 48.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(icon: ImageVector, label: String, userCount: Int, systemCount: Int, onClick: () -> Unit) {
    val total = userCount + systemCount
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
