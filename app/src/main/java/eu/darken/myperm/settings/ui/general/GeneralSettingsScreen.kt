package eu.darken.myperm.settings.ui.general

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.ColorLens
import androidx.compose.material.icons.twotone.Contrast
import androidx.compose.material.icons.twotone.DarkMode
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.LabeledOption
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.compose.SingleChoiceSortDialog
import eu.darken.myperm.common.error.ErrorEventHandler
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.common.navigation.NavigationEventHandler
import eu.darken.myperm.common.settings.SettingsBaseItem
import eu.darken.myperm.common.settings.SettingsCategoryHeader
import eu.darken.myperm.common.settings.SettingsDivider
import eu.darken.myperm.common.settings.ThemeColorSelectorDialog
import eu.darken.myperm.common.theming.LocalIsDynamicColorActive
import eu.darken.myperm.common.theming.ThemeColor
import eu.darken.myperm.common.theming.ThemeMode
import eu.darken.myperm.common.theming.ThemeStyle

@Composable
fun GeneralSettingsScreenHost() {
    val navCtrl = LocalNavigationController.current
    val vm: GeneralSettingsViewModel = hiltViewModel()

    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val themeMode by vm.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val themeStyle by vm.themeStyle.collectAsState(initial = ThemeStyle.DEFAULT)
    val themeColor by vm.themeColor.collectAsState(initial = ThemeColor.BLUE)
    val isDynamicColorActive = LocalIsDynamicColorActive.current
    val isPro by vm.isPro.collectAsState()

    GeneralSettingsScreen(
        onBack = { navCtrl?.up() },
        themeMode = themeMode,
        themeStyle = themeStyle,
        themeColor = themeColor,
        isDynamicColorActive = isDynamicColorActive,
        isPro = isPro,
        onThemeModeSelected = { vm.setThemeMode(it) },
        onThemeStyleSelected = { vm.setThemeStyle(it) },
        onThemeColorSelected = { vm.setThemeColor(it) },
        onUpgrade = { vm.onUpgrade() },
    )
}

private enum class ThemeDialog { MODE, STYLE, COLOR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeStyle: ThemeStyle = ThemeStyle.DEFAULT,
    themeColor: ThemeColor = ThemeColor.BLUE,
    isDynamicColorActive: Boolean = false,
    isPro: Boolean = true,
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    onThemeStyleSelected: (ThemeStyle) -> Unit = {},
    onThemeColorSelected: (ThemeColor) -> Unit = {},
    onUpgrade: () -> Unit = {},
) {
    var openDialog by remember { mutableStateOf<ThemeDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.general_settings_label)) },
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
            SettingsCategoryHeader(
                text = stringResource(R.string.settings_category_appearance_label),
                action = if (!isPro) {{
                    FilledTonalButton(onClick = onUpgrade) {
                        Icon(
                            Icons.TwoTone.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.upgrade_required_subtitle),
                            modifier = Modifier.padding(start = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }} else null,
            )

            SettingsBaseItem(
                title = stringResource(R.string.ui_theme_mode_label),
                subtitle = stringResource(themeMode.labelRes),
                icon = Icons.TwoTone.DarkMode,
                enabled = isPro,
                onClick = { openDialog = ThemeDialog.MODE },
            )
            SettingsDivider()
            SettingsBaseItem(
                title = stringResource(R.string.ui_theme_style_label),
                subtitle = stringResource(themeStyle.labelRes),
                icon = Icons.TwoTone.Contrast,
                enabled = isPro,
                onClick = { openDialog = ThemeDialog.STYLE },
            )
            SettingsDivider()

            val colorEnabled = isPro && !isDynamicColorActive
            SettingsBaseItem(
                title = stringResource(R.string.ui_theme_color_label),
                subtitle = if (!isDynamicColorActive) {
                    stringResource(themeColor.labelRes)
                } else {
                    stringResource(R.string.ui_theme_color_disabled_subtitle)
                },
                icon = Icons.TwoTone.ColorLens,
                enabled = colorEnabled,
                onClick = { openDialog = ThemeDialog.COLOR },
            )
        }
    }

    when (openDialog) {
        ThemeDialog.MODE -> SingleChoiceSortDialog(
            title = stringResource(R.string.ui_theme_mode_label),
            options = ThemeMode.entries.map { LabeledOption(it, it.labelRes) },
            selected = themeMode,
            onSelect = {
                onThemeModeSelected(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )

        ThemeDialog.STYLE -> SingleChoiceSortDialog(
            title = stringResource(R.string.ui_theme_style_label),
            options = ThemeStyle.entries.map { LabeledOption(it, it.labelRes) },
            selected = themeStyle,
            onSelect = {
                onThemeStyleSelected(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )

        ThemeDialog.COLOR -> ThemeColorSelectorDialog(
            selectedColor = themeColor,
            onColorSelected = {
                onThemeColorSelected(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )

        null -> {}
    }
}

@Preview2
@Composable
private fun GeneralSettingsScreenPreview() = PreviewWrapper {
    GeneralSettingsScreen(onBack = {})
}
