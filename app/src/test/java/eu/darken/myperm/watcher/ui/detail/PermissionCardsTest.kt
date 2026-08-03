package eu.darken.myperm.watcher.ui.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import eu.darken.myperm.watcher.core.PermissionDiff
import eu.darken.myperm.watcher.core.WatcherEventType
import org.junit.Test
import testhelpers.compose.BaseComposeRobolectricTest

class PermissionCardsTest : BaseComposeRobolectricTest() {

    @Test
    fun `removed event shows removed permissions`() {
        val diff = PermissionDiff(
            removedPermissions = listOf("android.permission.CAMERA"),
            removedDeclared = listOf("android.permission.INTERNET"),
        )

        composeRule.setContent {
            MaterialTheme {
                PermissionCards(
                    diff = diff,
                    eventType = WatcherEventType.REMOVED,
                    enrichedMap = emptyMap(),
                )
            }
        }

        composeRule.onNodeWithText("android.permission.CAMERA").assertIsDisplayed()
        composeRule.onNodeWithText("android.permission.INTERNET").assertIsDisplayed()
    }

    @Test
    fun `removed event ignores added permissions`() {
        val diff = PermissionDiff(
            addedPermissions = listOf("android.permission.CAMERA"),
            addedDeclared = listOf("android.permission.INTERNET"),
        )

        composeRule.setContent {
            MaterialTheme {
                PermissionCards(
                    diff = diff,
                    eventType = WatcherEventType.REMOVED,
                    enrichedMap = emptyMap(),
                )
            }
        }

        composeRule.onNodeWithText("android.permission.CAMERA").assertDoesNotExist()
        composeRule.onNodeWithText("android.permission.INTERNET").assertDoesNotExist()
    }
}
