package eu.darken.myperm.main.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.apps.ui.list.AppsFilterOptions
import eu.darken.myperm.common.compose.LoadingContent
import eu.darken.myperm.common.compose.rememberFabVisibility
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.main.ui.overview.OverviewViewModel.SummaryCategory

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
    val (fabVisible, scrollConnection) = rememberFabVisibility()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollConnection),
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = { if (!isRefreshing) onRefresh() },
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.general_refresh_action))
                    }
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
    val activeProfile = summary[SummaryCategory.ACTIVE_PROFILE]
    val total = activeProfile.user + activeProfile.system
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
            AppRatioBar(activeProfile.user, activeProfile.system, total)
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

private data class CategoryDisplay(
    val icon: ImageVector,
    val label: String,
    val count: PkgCount,
    val filters: Set<AppsFilterOptions.Filter>,
)

private data class SectionDisplay(
    val label: String,
    val categories: List<CategoryDisplay>,
)

@Composable
private fun SummaryList(
    summary: OverviewViewModel.SummaryInfo,
    onCategoryClick: (Set<AppsFilterOptions.Filter>) -> Unit,
) {
    val sections = listOf(
        SectionDisplay(
            label = stringResource(R.string.overview_section_profile_label),
            categories = listOf(
                CategoryDisplay(Icons.Filled.PhoneAndroid, stringResource(R.string.overview_summary_apps_active_profile_label), summary[SummaryCategory.ACTIVE_PROFILE], setOf(AppsFilterOptions.Filter.PRIMARY_PROFILE)),
                CategoryDisplay(Icons.Filled.People, stringResource(R.string.overview_summary_apps_other_profile_label), summary[SummaryCategory.OTHER_PROFILES], setOf(AppsFilterOptions.Filter.SECONDARY_PROFILE)),
                CategoryDisplay(Icons.Filled.ContentCopy, stringResource(R.string.overview_summary_apps_clones_label), summary[SummaryCategory.CLONES], setOf(AppsFilterOptions.Filter.MULTI_PROFILE)),
            ),
        ),
        SectionDisplay(
            label = stringResource(R.string.overview_section_install_source_label),
            categories = listOf(
                CategoryDisplay(Icons.Filled.Shop, stringResource(R.string.overview_summary_apps_gplay_label), summary[SummaryCategory.GOOGLE_PLAY], setOf(AppsFilterOptions.Filter.GOOGLE_PLAY)),
                CategoryDisplay(Icons.Filled.Storefront, stringResource(R.string.overview_summary_apps_oemstore_label), summary[SummaryCategory.OEM_STORE], setOf(AppsFilterOptions.Filter.OEM_STORE)),
                CategoryDisplay(Icons.Filled.InstallMobile, stringResource(R.string.overview_summary_apps_sideloaded_label), summary[SummaryCategory.SIDELOADED], setOf(AppsFilterOptions.Filter.SIDELOADED)),
            ),
        ),
        SectionDisplay(
            label = stringResource(R.string.overview_section_privacy_label),
            categories = listOf(
                CategoryDisplay(Icons.Filled.CameraAlt, stringResource(R.string.overview_summary_apps_camera_label), summary[SummaryCategory.CAMERA], setOf(AppsFilterOptions.Filter.CAMERA)),
                CategoryDisplay(Icons.Filled.LocationOn, stringResource(R.string.overview_summary_apps_location_label), summary[SummaryCategory.LOCATION], setOf(AppsFilterOptions.Filter.LOCATION)),
                CategoryDisplay(Icons.Filled.Mic, stringResource(R.string.overview_summary_apps_microphone_label), summary[SummaryCategory.MICROPHONE], setOf(AppsFilterOptions.Filter.MICROPHONE)),
                CategoryDisplay(Icons.Filled.Contacts, stringResource(R.string.overview_summary_apps_contacts_label), summary[SummaryCategory.CONTACTS], setOf(AppsFilterOptions.Filter.CONTACTS)),
            ),
        ),
        SectionDisplay(
            label = stringResource(R.string.overview_section_security_label),
            categories = listOf(
                CategoryDisplay(Icons.Filled.GetApp, stringResource(R.string.overview_summary_apps_installers_label), summary[SummaryCategory.INSTALLERS], setOf(AppsFilterOptions.Filter.INSTALL_PACKAGES)),
                CategoryDisplay(Icons.Filled.Layers, stringResource(R.string.overview_summary_apps_overlayers_label), summary[SummaryCategory.OVERLAYERS], setOf(AppsFilterOptions.Filter.OVERLAY)),
            ),
        ),
        SectionDisplay(
            label = stringResource(R.string.overview_section_system_label),
            categories = listOf(
                CategoryDisplay(Icons.Filled.WifiOff, stringResource(R.string.overview_summary_apps_offline_label), summary[SummaryCategory.NO_INTERNET], setOf(AppsFilterOptions.Filter.NO_INTERNET)),
                CategoryDisplay(Icons.Filled.Share, stringResource(R.string.overview_summary_apps_sharedids_label), summary[SummaryCategory.SHARED_IDS], setOf(AppsFilterOptions.Filter.SHARED_ID)),
                CategoryDisplay(Icons.Filled.BatteryAlert, stringResource(R.string.overview_summary_apps_battery_label), summary[SummaryCategory.BATTERY_OPT], setOf(AppsFilterOptions.Filter.BATTERY_OPTIMIZATION)),
                CategoryDisplay(Icons.Filled.Warning, stringResource(R.string.overview_summary_apps_oldapi_label), summary[SummaryCategory.OLD_API], setOf(AppsFilterOptions.Filter.OLD_API_TARGET)),
            ),
        ),
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            sections.forEachIndexed { sectionIndex, section ->
                if (sectionIndex > 0) {
                    HorizontalDivider()
                }
                SectionHeader(section.label)
                section.categories.forEachIndexed { catIndex, category ->
                    CategoryRow(
                        icon = category.icon,
                        label = category.label,
                        count = category.count,
                        onClick = { onCategoryClick(category.filters) },
                    )
                    if (catIndex < section.categories.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun CategoryRow(icon: ImageVector, label: String, count: PkgCount, onClick: () -> Unit) {
    val total = count.user + count.system
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
            if (count.system > 0) {
                val userText = pluralStringResource(R.plurals.generic_x_apps_user_label, count.user, count.user)
                val systemText = pluralStringResource(R.plurals.generic_x_apps_system_label, count.system, count.system)
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
