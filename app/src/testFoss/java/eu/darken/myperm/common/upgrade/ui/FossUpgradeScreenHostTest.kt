package eu.darken.myperm.common.upgrade.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.upgrade.core.UpgradeRepoFoss
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider
import testhelpers.TestApplication
import java.time.Duration

/**
 * Host-level counterpart to the ViewModel's sponsor-return tests: those prove the ViewModel reacts,
 * they cannot prove the screen actually bridges the lifecycle to it. Only a real STOP/RESUME
 * round-trip catches a missing or mis-scoped lifecycle effect, or a tracker that isn't seeded from
 * the handle after a recreation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class FossUpgradeScreenHostTest : BaseTest() {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var persisted = 0

    private fun mockRepo(): UpgradeRepoFoss = mockk<UpgradeRepoFoss>(relaxed = true).apply {
        every { upgradeInfo } returns MutableStateFlow(UpgradeRepoFoss.Info())
        coEvery { persistUpgrade() } answers { persisted++ }
    }

    private fun buildVm(
        repo: UpgradeRepoFoss,
        handle: SavedStateHandle = SavedStateHandle(),
    ) = UpgradeViewModel(
        handle = handle,
        dispatcherProvider = TestDispatcherProvider(),
        upgradeRepo = repo,
    )

    @Test
    fun `a stop-resume round-trip after a sponsor launch runs the return check`() {
        // The real ViewModel instance, passed explicitly: hiltViewModel() has nothing to resolve
        // here. The route is passed explicitly too -- PP binds it through the host's own
        // LaunchedEffect, not from a SavedStateHandle.
        val vm = buildVm(mockRepo())

        composeRule.setContent {
            PreviewWrapper {
                UpgradeScreenHost(route = Nav.Main.Upgrade(), vm = vm)
            }
        }
        composeRule.waitForIdle()

        vm.goGithubSponsors()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(6))

        // CREATED, not STARTED: only that far down does the activity emit ON_STOP, which is what
        // "the browser took the foreground" looks like to the return tracker.
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.waitUntil { persisted == 1 }
        vm.hasPendingSponsorLaunch() shouldBe false
    }

    @Test
    fun `a recreated screen consumes the pending sponsor launch on the first resume`() {
        // Process death while the browser was in front: the fresh screen's tracker never saw the
        // ON_STOP, so it has to be seeded from the handle or the first return is swallowed.
        val handle = SavedStateHandle()
        buildVm(mockRepo(), handle).goGithubSponsors()

        val recreatedVm = buildVm(mockRepo(), handle)
        recreatedVm.hasPendingSponsorLaunch() shouldBe true

        ShadowSystemClock.advanceBy(Duration.ofSeconds(6))

        composeRule.setContent {
            PreviewWrapper {
                UpgradeScreenHost(route = Nav.Main.Upgrade(), vm = recreatedVm)
            }
        }
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.waitUntil { persisted == 1 }
        recreatedVm.hasPendingSponsorLaunch() shouldBe false
    }
}
