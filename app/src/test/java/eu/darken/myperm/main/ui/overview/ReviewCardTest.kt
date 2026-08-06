package eu.darken.myperm.main.ui.overview

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.PreviewWrapper
import io.kotest.matchers.shouldBe
import org.junit.Test
import testhelpers.compose.BaseComposeRobolectricTest

class ReviewCardTest : BaseComposeRobolectricTest() {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun string(id: Int): String = context.getString(id)

    private fun show(
        reviewEnabled: Boolean = true,
        onReview: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            PreviewWrapper {
                ReviewCard(
                    onReview = onReview,
                    onDismiss = onDismiss,
                    reviewEnabled = reviewEnabled,
                )
            }
        }
    }

    @Test
    fun `the card offers both the review and the maybe later action`() {
        show()

        composeRule.onNodeWithText(string(R.string.review_app_body)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.review_app_review_action)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.review_app_dismiss_action)).assertIsDisplayed()
    }

    @Test
    fun `tapping review invokes the review callback`() {
        var reviews = 0
        var dismisses = 0
        show(onReview = { reviews++ }, onDismiss = { dismisses++ })

        composeRule.onNodeWithText(string(R.string.review_app_review_action)).performClick()

        composeRule.runOnIdle {
            reviews shouldBe 1
            dismisses shouldBe 0
        }
    }

    @Test
    fun `tapping maybe later invokes the dismiss callback`() {
        var reviews = 0
        var dismisses = 0
        show(onReview = { reviews++ }, onDismiss = { dismisses++ })

        composeRule.onNodeWithText(string(R.string.review_app_dismiss_action)).performClick()

        composeRule.runOnIdle {
            dismisses shouldBe 1
            reviews shouldBe 0
        }
    }

    @Test
    fun `without a hosting activity the review action is disabled`() {
        // Play's flow needs an activity, so the host disables the action when it has none. Maybe
        // later stays available: dismissing is purely local bookkeeping.
        var reviews = 0
        var dismisses = 0
        show(reviewEnabled = false, onReview = { reviews++ }, onDismiss = { dismisses++ })

        composeRule.onNodeWithText(string(R.string.review_app_review_action)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.review_app_review_action)).performClick()
        composeRule.onNodeWithText(string(R.string.review_app_dismiss_action)).assertIsEnabled()

        composeRule.runOnIdle {
            reviews shouldBe 0
            dismisses shouldBe 0
        }

        // Nothing was handed to the caller, so the blocked review tap must not consume the latch
        // that keeps the card dismissable.
        composeRule.onNodeWithText(string(R.string.review_app_dismiss_action)).performClick()

        composeRule.runOnIdle {
            reviews shouldBe 0
            dismisses shouldBe 1
        }
    }

    // The card only disappears once the next state emission arrives, so its tap targets need a
    // latch: a dismiss after a review would overwrite the completed-review bookkeeping with a
    // snooze, a review after a dismiss would re-open what the user just closed. The latch is
    // asymmetric — repeated review taps stay allowed, because a Play request can fail without
    // persisting anything, which leaves the card on screen and in need of a retry.
    @Test
    fun `a dismissed card ignores a later review tap`() {
        var reviews = 0
        var dismisses = 0
        show(onReview = { reviews++ }, onDismiss = { dismisses++ })

        composeRule.onNodeWithText(string(R.string.review_app_dismiss_action)).performClick()
        composeRule.runOnIdle { dismisses shouldBe 1 }

        composeRule.onNodeWithText(string(R.string.review_app_review_action)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.review_app_review_action)).performClick()

        composeRule.runOnIdle {
            reviews shouldBe 0
            dismisses shouldBe 1
        }
    }

    @Test
    fun `a reviewed card ignores a later dismiss tap`() {
        var reviews = 0
        var dismisses = 0
        show(onReview = { reviews++ }, onDismiss = { dismisses++ })

        composeRule.onNodeWithText(string(R.string.review_app_review_action)).performClick()
        composeRule.runOnIdle { reviews shouldBe 1 }

        composeRule.onNodeWithText(string(R.string.review_app_dismiss_action)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.review_app_dismiss_action)).performClick()

        composeRule.runOnIdle {
            reviews shouldBe 1
            dismisses shouldBe 0
        }
    }

    @Test
    fun `repeated review taps are not absorbed by the card`() {
        var reviews = 0
        var dismisses = 0
        show(onReview = { reviews++ }, onDismiss = { dismisses++ })

        composeRule.onNodeWithText(string(R.string.review_app_review_action)).performClick()
        composeRule.runOnIdle { reviews shouldBe 1 }

        // A failed Play request persists nothing and leaves the card up, so the retry has to work.
        // Duplicates are the tool's problem, it holds a single-flight lock for exactly this.
        composeRule.onNodeWithText(string(R.string.review_app_review_action)).assertIsEnabled()
        composeRule.onNodeWithText(string(R.string.review_app_review_action)).performClick()

        composeRule.runOnIdle {
            reviews shouldBe 2
            dismisses shouldBe 0
        }
    }
}
