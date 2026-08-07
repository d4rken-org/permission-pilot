package eu.darken.myperm.settings.ui.index

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.PreviewWrapper
import org.junit.Test
import org.robolectric.annotation.Config
import testhelpers.compose.BaseComposeRobolectricTest
import testhelpers.compose.brandTitleFor

/**
 * The settings entry names the same tier the dashboard does. It used to carry its own hardcoded
 * translation of the composed brand, which drifted: in es/ar/am/az/be the dashboard rendered the
 * localized app name while this row still said "Permission Pilot Pro". Composing both from the same
 * parts is what makes them unable to disagree, so these tests assert the agreement itself rather
 * than any particular string.
 */
class SettingsIndexBrandTitleTest : BaseComposeRobolectricTest() {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun showScreen() {
        composeRule.setContent {
            PreviewWrapper {
                SettingsIndexScreen(
                    onBack = {},
                    onChangelog = {},
                    onSupport = {},
                    onAcknowledgements = {},
                    onPrivacyPolicy = {},
                    versionSubtitle = "v1.2.3",
                )
            }
        }
    }

    @Test
    fun `the upgrade row carries the same composed brand as the dashboard`() {
        showScreen()

        composeRule.onAllNodesWithText(context.brandTitleFor(R.string.app_name)).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = "es")
    fun `the upgrade row is localized rather than pinned to English`() {
        showScreen()

        // Was "Permission Pilot Pro" here while the dashboard said "Piloto de los permisos Pro".
        requireLocalizedAppName()
        composeRule.onAllNodesWithText(context.brandTitleFor(R.string.app_name)).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = "ar")
    fun `the upgrade row is localized in arabic too`() {
        showScreen()

        requireLocalizedAppName()
        composeRule.onAllNodesWithText(context.brandTitleFor(R.string.app_name)).assertCountEquals(1)
    }

    // Guards the guard: if app_name ever stopped being translated in these locales the assertions
    // above would still pass while proving nothing.
    private fun requireLocalizedAppName() {
        val appName = context.getString(R.string.app_name)
        check(appName != "Permission Pilot") {
            "app_name is untranslated in this locale, so the localization assertion proves nothing"
        }
    }
}
