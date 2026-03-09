package eu.darken.myperm.settings.ui.index

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.FormatListNumbered
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material.icons.automirrored.twotone.HelpOutline
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.settings.SettingsBaseItem
import eu.darken.myperm.common.settings.SettingsCategoryHeader
import eu.darken.myperm.common.settings.SettingsDivider

@Composable
fun SettingsIndexScreenHost() {
    val navCtrl = LocalNavigationController.current
    val vm: SettingsIndexViewModel = hiltViewModel()

    SettingsIndexScreen(
        onBack = { navCtrl?.up() },
        onChangelog = { vm.openChangelog() },
        onSupport = { navCtrl?.goTo(Nav.Settings.Support) },
        onAcknowledgements = { navCtrl?.goTo(Nav.Settings.Acknowledgements) },
        onPrivacyPolicy = { vm.openPrivacyPolicy() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsIndexScreen(
    onBack: () -> Unit,
    onChangelog: () -> Unit,
    onSupport: () -> Unit,
    onAcknowledgements: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    versionSubtitle: String = BuildConfigWrap.VERSION_DESCRIPTION,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_page_label)) },
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
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsCategoryHeader(text = stringResource(R.string.settings_category_other_label))

            SettingsBaseItem(
                title = stringResource(R.string.settings_support_label),
                subtitle = "\u00AF\\_(ツ)_/\u00AF",
                icon = Icons.AutoMirrored.TwoTone.HelpOutline,
                onClick = onSupport,
            )
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.changelog_label),
                subtitle = versionSubtitle,
                icon = Icons.TwoTone.FormatListNumbered,
                onClick = onChangelog,
            )
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.settings_acknowledgements_label),
                subtitle = stringResource(R.string.general_thank_you_label),
                icon = Icons.TwoTone.Favorite,
                onClick = onAcknowledgements,
            )
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.settings_privacy_policy_label),
                subtitle = stringResource(R.string.settings_privacy_policy_desc),
                icon = Icons.TwoTone.Info,
                onClick = onPrivacyPolicy,
            )
        }
    }
}

@Preview2
@Composable
private fun SettingsIndexScreenPreview() = PreviewWrapper {
    SettingsIndexScreen(onBack = {}, onChangelog = {}, onSupport = {}, onAcknowledgements = {}, onPrivacyPolicy = {})
}
