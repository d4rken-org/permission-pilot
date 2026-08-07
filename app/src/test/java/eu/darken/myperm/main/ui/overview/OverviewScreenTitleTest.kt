package eu.darken.myperm.main.ui.overview

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.upgrade.UpgradeRepo
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.robolectric.annotation.Config
import testhelpers.compose.BaseComposeRobolectricTest
import testhelpers.compose.brandQualifier
import testhelpers.compose.expectedBrandTitle
import testhelpers.compose.shouldHighlightOnlyQualifier

/**
 * The dashboard titles itself with the branded app name while Pro is active. Composed from
 * `app_name`, NOT from the upgrade screen's title prefix: only `app_name` is translated everywhere,
 * so the title must not switch language when the entitlement lands.
 */
class OverviewScreenTitleTest : BaseComposeRobolectricTest() {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val appName: String
        get() = context.getString(R.string.app_name)

    private val suffix: String
        get() = context.brandQualifier

    // Derived from the resolved template, never spelled as "$appName $suffix": arrangement is the
    // translator's, so a locale that reorders or repunctuates is intended behaviour, not a failure.
    private val brandedTitle: String
        get() = context.expectedBrandTitle

    private fun proInfo(): UpgradeRepo.Info = mockk<UpgradeRepo.Info>(relaxed = true).also {
        every { it.isPro } returns true
        every { it.isSettled } returns true
        every { it.error } returns null
    }

    private fun freeInfo(): UpgradeRepo.Info = mockk<UpgradeRepo.Info>(relaxed = true).also {
        every { it.isPro } returns false
        every { it.isSettled } returns true
        every { it.error } returns null
    }

    private fun state(upgradeInfo: UpgradeRepo.Info?) =
        OverviewPreviewData.loadedState().copy(upgradeInfo = upgradeInfo)

    private fun showState(upgradeInfo: UpgradeRepo.Info?) {
        composeRule.setContent {
            PreviewWrapper {
                OverviewScreen(
                    state = state(upgradeInfo),
                    onRefresh = {},
                    onSettings = {},
                )
            }
        }
    }

    @Test
    fun `a supporter gets the branded title`() {
        showState(proInfo())

        composeRule.onAllNodesWithText(brandedTitle).assertCountEquals(1)
    }

    @Test
    fun `the branded title colors exactly the suffix`() {
        var expectedTertiary = Color.Unspecified
        composeRule.setContent {
            PreviewWrapper {
                // The highlight is a theme role: capture it from the composition under test.
                expectedTertiary = MaterialTheme.colorScheme.tertiary
                OverviewScreen(
                    state = state(proInfo()),
                    onRefresh = {},
                    onSettings = {},
                )
            }
        }

        val rendered = composeRule.onNodeWithText(brandedTitle)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .single()

        rendered.text shouldBe brandedTitle
        rendered.shouldHighlightOnlyQualifier(suffix, expectedTertiary)
    }

    @Test
    fun `without the entitlement the title stays the plain app name`() {
        showState(freeInfo())

        composeRule.onAllNodesWithText(appName).assertCountEquals(1)
        composeRule.onAllNodesWithText(brandedTitle).assertCountEquals(0)
    }

    @Test
    fun `an unknown entitlement stays the plain app name`() {
        // What the state looks like before the entitlement arm has answered at all.
        showState(null)

        composeRule.onAllNodesWithText(appName).assertCountEquals(1)
        composeRule.onAllNodesWithText(brandedTitle).assertCountEquals(0)
    }

    @Test
    fun `the title follows the entitlement as it changes`() {
        // The realistic path: the screen is already up when the entitlement lands (and, on a
        // revocation, when it goes away again).
        var current by mutableStateOf<UpgradeRepo.Info?>(null)
        composeRule.setContent {
            PreviewWrapper {
                OverviewScreen(
                    state = state(current),
                    onRefresh = {},
                    onSettings = {},
                )
            }
        }

        composeRule.onAllNodesWithText(brandedTitle).assertCountEquals(0)

        composeRule.runOnUiThread { current = proInfo() }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText(brandedTitle).assertCountEquals(1)
        composeRule.onAllNodesWithText(appName).assertCountEquals(0)

        composeRule.runOnUiThread { current = freeInfo() }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText(brandedTitle).assertCountEquals(0)
        composeRule.onAllNodesWithText(appName).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = "es")
    fun `the branded title keeps the locale's app name`() {
        showState(proInfo())

        // Gaining Pro must not switch the title's language: the brand stays "Piloto de los
        // permisos", never the untranslated "Permission Pilot".
        appName shouldBe "Piloto de los permisos"
        composeRule.onAllNodesWithText(brandedTitle).assertCountEquals(1)
        composeRule.onAllNodesWithText("Permission Pilot $suffix").assertCountEquals(0)
    }
}
