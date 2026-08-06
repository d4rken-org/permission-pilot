package eu.darken.myperm.common.error

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import eu.darken.myperm.R
import eu.darken.myperm.common.compose.PreviewWrapper
import eu.darken.myperm.common.flow.SingleEventFlow
import io.kotest.matchers.shouldBe
import org.junit.Test
import testhelpers.compose.BaseComposeRobolectricTest

/**
 * The shared error dialog's fix action is arbitrary code: a fix that blows up must never take the
 * UI — or the dialog's own exit — down with it. [ErrorEventHandler] backs every screen, so a latched
 * dialog would leave the user stuck on the current error.
 */
class ComposeErrorDialogGuardTest : BaseComposeRobolectricTest() {

    private class FakeErrorSource : ErrorEventSource2 {
        override val errorEvents = SingleEventFlow<Throwable>()
    }

    private class ThrowingFixError(
        private val fixErrorMessage: String? = null,
        private val onFixDispatched: () -> Unit = {},
    ) : Exception(ERROR_BODY), HasLocalizedError {

        override fun getLocalizedError(context: Context): LocalizedError = LocalizedError(
            throwable = this,
            label = ERROR_TITLE,
            description = ERROR_BODY,
            fixActionLabel = FIX_LABEL,
            fixAction = {
                // Flag first: the assertion has to distinguish "action ran and threw" from
                // "action was never dispatched".
                onFixDispatched()
                throw IllegalStateException("fix action exploded")
            },
            fixActionErrorMessage = fixErrorMessage,
        )
    }

    private fun showError(error: Throwable) {
        val source = FakeErrorSource()
        composeRule.setContent {
            PreviewWrapper {
                ErrorEventHandler(source)
            }
        }
        // Buffered channel: the event survives until the handler's collector attaches.
        source.errorEvents.tryEmit(error)
        composeRule.waitForIdle()
    }

    @Test
    fun `a throwing fix action without its own message still dismisses the dialog`() {
        // The other half of the per-dispatch contract: a dispatch that ships NO failure copy keeps
        // the plain log-then-dismiss behaviour and can never pick up someone else's message.
        var invoked = false
        showError(ThrowingFixError(onFixDispatched = { invoked = true }))

        composeRule.onNodeWithText(FIX_LABEL).performClick()
        composeRule.waitForIdle()

        invoked shouldBe true
        // The fix button is gone: the throw neither crashed the click handler nor swallowed the
        // dismissal that closes the dialog.
        composeRule.onAllNodesWithText(FIX_LABEL).assertCountEquals(0)
    }

    @Test
    fun `a throwing fix action with its own message keeps the dialog open and shows it inline`() {
        // A Toast caps at 2 lines and clipped this kind of message; the dialog body has no cap.
        showError(ThrowingFixError(fixErrorMessage = FIX_ERROR_MESSAGE))

        composeRule.onNodeWithText(FIX_LABEL).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(FIX_ERROR_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText(FIX_LABEL).assertExists()
        // Not latched: the way out stays available while the message is shown.
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.onNodeWithText(context.getString(R.string.general_close_action)).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(FIX_LABEL).assertCountEquals(0)
        composeRule.onAllNodesWithText(FIX_ERROR_MESSAGE).assertCountEquals(0)
    }
}

private const val ERROR_TITLE = "Test error title"
private const val ERROR_BODY = "Test error description"
private const val FIX_LABEL = "Fix it"
private const val FIX_ERROR_MESSAGE = "Fixing it did not work"
