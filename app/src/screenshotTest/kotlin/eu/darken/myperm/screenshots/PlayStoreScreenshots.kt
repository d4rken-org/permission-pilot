// For IDE design preview, open ScreenshotContent.kt instead.
package eu.darken.myperm.screenshots

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@PlayStoreLocales
@Composable
fun OverviewLight() = OverviewLightContent()

@PreviewTest
@PlayStoreLocalesDark
@Composable
fun OverviewDark() = OverviewDarkContent()

@PreviewTest
@PlayStoreLocales
@Composable
fun AppsList() = AppsListContent()

@PreviewTest
@PlayStoreLocales
@Composable
fun AppDetails() = AppDetailsContent()

@PreviewTest
@PlayStoreLocales
@Composable
fun PermissionsList() = PermissionsListContent()

@PreviewTest
@PlayStoreLocales
@Composable
fun PermissionDetails() = PermissionDetailsContent()

@PreviewTest
@PlayStoreLocales
@Composable
fun SettingsIndex() = SettingsIndexContent()
