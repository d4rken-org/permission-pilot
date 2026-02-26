package eu.darken.myperm.screenshots

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import eu.darken.myperm.R
import eu.darken.myperm.apps.ui.details.AppDetailsPreviewData
import eu.darken.myperm.apps.ui.details.AppDetailsScreen
import eu.darken.myperm.apps.ui.list.AppsPreviewData
import eu.darken.myperm.apps.ui.list.AppsScreen
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.main.ui.overview.OverviewPreviewData
import eu.darken.myperm.main.ui.overview.OverviewScreen
import eu.darken.myperm.permissions.ui.details.PermissionDetailsPreviewData
import eu.darken.myperm.permissions.ui.details.PermissionDetailsScreen
import eu.darken.myperm.permissions.ui.list.PermissionsPreviewData
import eu.darken.myperm.permissions.ui.list.PermissionsScreen
import eu.darken.myperm.settings.ui.index.SettingsIndexScreen

internal const val DS = "spec:width=1080px,height=2400px,dpi=428"

private enum class SelectedTab { OVERVIEW, APPS, PERMISSIONS }

/**
 * Wraps tab screen content with the bottom navigation bar, matching the real app layout.
 */
@Composable
private fun WithBottomBar(selectedTab: SelectedTab, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        NavigationBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            NavigationBarItem(
                selected = selectedTab == SelectedTab.OVERVIEW,
                onClick = {},
                icon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) },
                label = { Text(stringResource(R.string.overview_page_label)) },
            )
            NavigationBarItem(
                selected = selectedTab == SelectedTab.APPS,
                onClick = {},
                icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                label = { Text(stringResource(R.string.apps_page_label)) },
            )
            NavigationBarItem(
                selected = selectedTab == SelectedTab.PERMISSIONS,
                onClick = {},
                icon = { Icon(Icons.Filled.Security, contentDescription = null) },
                label = { Text(stringResource(R.string.permissions_page_label)) },
            )
        }
    }
}

@Composable
internal fun OverviewLightContent() = PreviewWrapper {
    WithBottomBar(SelectedTab.OVERVIEW) {
        OverviewScreen(
            state = OverviewPreviewData.loadedState().copy(versionDesc = "v1.2.3 (42)"),
            onRefresh = {},
            onSettings = {},
        )
    }
}

@Composable
internal fun OverviewDarkContent() = PreviewWrapper {
    WithBottomBar(SelectedTab.OVERVIEW) {
        OverviewScreen(
            state = OverviewPreviewData.loadedState().copy(versionDesc = "v1.2.3 (42)"),
            onRefresh = {},
            onSettings = {},
        )
    }
}

@Composable
internal fun AppsListContent() = PreviewWrapper {
    WithBottomBar(SelectedTab.APPS) {
        AppsScreen(
            state = AppsPreviewData.readyState(),
            onSearchChanged = {},
            onAppClicked = {},
            onFilter = {},
            onSort = {},
            onRefresh = {},
            onSettings = {},
        )
    }
}

@Composable
internal fun AppDetailsContent() = PreviewWrapper {
    AppDetailsScreen(
        state = AppDetailsPreviewData.loadedState(),
        onBack = {},
        onPermClicked = {},
        onTwinClicked = {},
        onSiblingClicked = {},
        onGoSettings = {},
        onOpenApp = {},
        onFilter = {},
        onInstallerClicked = {},
    )
}

@Composable
internal fun PermissionsListContent() = PreviewWrapper {
    WithBottomBar(SelectedTab.PERMISSIONS) {
        PermissionsScreen(
            state = PermissionsPreviewData.readyState(),
            onSearchChanged = {},
            onGroupClicked = {},
            onPermClicked = {},
            onExpandAll = {},
            onCollapseAll = {},
            onRefresh = {},
            onSettings = {},
            onFilterClicked = {},
            onSortClicked = {},
        )
    }
}

@Composable
internal fun PermissionDetailsContent() = PreviewWrapper {
    PermissionDetailsScreen(
        state = PermissionDetailsPreviewData.loadedState(),
        onBack = {},
        onAppClicked = { _, _ -> },
        onFilterClicked = {},
        onPermissionHelpClicked = {},
        onStatusHelpClicked = {},
    )
}

@Composable
internal fun SettingsIndexContent() = PreviewWrapper {
    SettingsIndexScreen(
        onBack = {},
        onGeneral = {},
        onSupport = {},
        onAcknowledgements = {},
        onPrivacyPolicy = null,
        versionSubtitle = "v1.2.3",
    )
}

@Preview(name = "1 - Overview Light", locale = "en", device = DS, showSystemUi = true)
@Composable
private fun PreviewOverviewLight() = OverviewLightContent()

@Preview(name = "2 - Overview Dark", locale = "en", device = DS, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
private fun PreviewOverviewDark() = OverviewDarkContent()

@Preview(name = "3 - Apps List", locale = "en", device = DS, showSystemUi = true)
@Composable
private fun PreviewAppsList() = AppsListContent()

@Preview(name = "4 - App Details", locale = "en", device = DS, showSystemUi = true)
@Composable
private fun PreviewAppDetails() = AppDetailsContent()

@Preview(name = "5 - Permissions List", locale = "en", device = DS, showSystemUi = true)
@Composable
private fun PreviewPermissionsList() = PermissionsListContent()

@Preview(name = "6 - Permission Details", locale = "en", device = DS, showSystemUi = true)
@Composable
private fun PreviewPermissionDetails() = PermissionDetailsContent()

@Preview(name = "7 - Settings", locale = "en", device = DS, showSystemUi = true)
@Composable
private fun PreviewSettingsIndex() = SettingsIndexContent()
