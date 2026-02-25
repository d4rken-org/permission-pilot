package eu.darken.myperm.settings.ui.index

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Policy
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.darken.myperm.R
import eu.darken.myperm.common.BuildConfigWrap
import eu.darken.myperm.common.PrivacyPolicy
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

    SettingsIndexScreen(
        onBack = { navCtrl?.up() },
        onGeneral = { navCtrl?.goTo(Nav.Settings.General) },
        onSupport = { navCtrl?.goTo(Nav.Settings.Support) },
        onAcknowledgements = { navCtrl?.goTo(Nav.Settings.Acknowledgements) },
        onPrivacyPolicy = null, // Handled via WebpageTool — will be wired later
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsIndexScreen(
    onBack: () -> Unit,
    onGeneral: () -> Unit,
    onSupport: () -> Unit,
    onAcknowledgements: () -> Unit,
    onPrivacyPolicy: (() -> Unit)?,
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
            SettingsBaseItem(
                title = stringResource(R.string.general_settings_label),
                subtitle = BuildConfigWrap.VERSION_DESCRIPTION,
                icon = Icons.TwoTone.Settings,
                onClick = onGeneral,
            )
            SettingsDivider()

            SettingsCategoryHeader(text = stringResource(R.string.settings_category_other_label))

            SettingsBaseItem(
                title = stringResource(R.string.settings_privacy_policy_label),
                subtitle = stringResource(R.string.settings_privacy_policy_desc),
                icon = Icons.TwoTone.Policy,
                onClick = { onPrivacyPolicy?.invoke() },
            )
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.settings_support_label),
                icon = Icons.TwoTone.Favorite,
                onClick = onSupport,
            )
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.settings_licenses_label),
                icon = Icons.TwoTone.Info,
                onClick = onAcknowledgements,
            )
        }
    }
}

@Preview2
@Composable
private fun SettingsIndexScreenPreview() = PreviewWrapper {
    SettingsIndexScreen(onBack = {}, onGeneral = {}, onSupport = {}, onAcknowledgements = {}, onPrivacyPolicy = null)
}
