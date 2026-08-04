package eu.darken.myperm.common.error

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private class ThrowingFixError(private val onFixDispatched: () -> Unit) :
        Exception(ERROR_BODY), HasLocalizedError {

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
    fun `a throwing fix action still dismisses the dialog`() {
        var invoked = false
        showError(ThrowingFixError { invoked = true })

        composeRule.onNodeWithText(FIX_LABEL).performClick()
        composeRule.waitForIdle()

        invoked shouldBe true
        // The fix button is gone: the throw neither crashed the click handler nor swallowed the
        // dismissal that closes the dialog.
        composeRule.onAllNodesWithText(FIX_LABEL).assertCountEquals(0)
    }
}

private const val ERROR_TITLE = "Test error title"
private const val ERROR_BODY = "Test error description"
private const val FIX_LABEL = "Fix it"
