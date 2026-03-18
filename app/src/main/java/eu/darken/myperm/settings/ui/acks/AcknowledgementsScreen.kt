package eu.darken.myperm.settings.ui.acks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
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
import eu.darken.myperm.common.compose.Preview2
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.navigation.LocalNavigationController
import eu.darken.myperm.common.settings.SettingsBaseItem
import eu.darken.myperm.common.settings.SettingsCategoryHeader

private data class AckEntry(val title: String, val subtitle: String, val url: String)

private val LICENSE_ENTRIES = listOf(
    AckEntry(
        "Material Design Icons",
        "materialdesignicons.com (SIL Open Font License 1.1 / Attribution 4.0 International)",
        "https://github.com/Templarian/MaterialDesign",
    ),
    AckEntry(
        "Lottie",
        "Airbnb's Lottie for Android. (APACHE 2.0)",
        "https://github.com/airbnb/lottie-android",
    ),
    AckEntry(
        "Kotlin",
        "The Kotlin Programming Language. (APACHE 2.0)",
        "https://github.com/JetBrains/kotlin",
    ),
    AckEntry(
        "Dagger",
        "A fast dependency injector for Android and Java. (APACHE 2.0)",
        "https://github.com/google/dagger",
    ),
    AckEntry(
        "Android",
        "Android Open Source Project (APACHE 2.0)",
        "https://source.android.com/source/licenses.html",
    ),
    AckEntry(
        "Android",
        "The Android robot is reproduced or modified from work created and shared by Google and used according to terms described in the Creative Commons 3.0 Attribution License.",
        "https://developer.android.com/distribute/tools/promote/brand.html",
    ),
    AckEntry(
        "apk-parser",
        "APK parser for Java/Android. (BSD 2-Clause)",
        "https://github.com/nicedoc/apk-parser",
    ),
)

@Composable
fun AcknowledgementsScreenHost() {
    val navCtrl = LocalNavigationController.current
    val vm: AcknowledgementsViewModel = hiltViewModel()

    AcknowledgementsScreen(
        onBack = { navCtrl?.up() },
        onOpenUrl = { vm.openUrl(it) },
    )
}

@Composable
fun AcknowledgementsScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val thankYouEntries = listOf(
        AckEntry("Max Patchs", stringResource(R.string.acks_maxpatchs_desc), "https://twitter.com/maxpatchs"),
        AckEntry("Crowdin", stringResource(R.string.acks_crowdin_desc), "https://crowdin.com/project/permission-pilot"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_acknowledgements_label)) },
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
            SettingsCategoryHeader(text = stringResource(R.string.general_thank_you_label))
            thankYouEntries.forEachIndexed { index, entry ->
                SettingsBaseItem(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    onClick = { onOpenUrl(entry.url) },
                )
                if (index < thankYouEntries.lastIndex) HorizontalDivider()
            }

            SettingsCategoryHeader(text = stringResource(R.string.settings_licenses_label))
            LICENSE_ENTRIES.forEachIndexed { index, entry ->
                SettingsBaseItem(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    onClick = { onOpenUrl(entry.url) },
                )
                if (index < LICENSE_ENTRIES.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Preview2
@Composable
private fun AcknowledgementsScreenPreview() = PreviewWrapper {
    AcknowledgementsScreen(onBack = {}, onOpenUrl = {})
}
