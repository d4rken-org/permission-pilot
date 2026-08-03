package eu.darken.myperm.watcher.ui.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.darken.myperm.watcher.core.PermissionDiff
import eu.darken.myperm.watcher.core.WatcherEventType
import io.kotest.matchers.shouldBe
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
                    onViewPermission = {},
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
                    onViewPermission = {},
                )
            }
        }

        composeRule.onNodeWithText("android.permission.CAMERA").assertDoesNotExist()
        composeRule.onNodeWithText("android.permission.INTERNET").assertDoesNotExist()
    }

    private val cameraDescription = "Allows the app to take pictures and videos"

    private val cameraEnrichedMap = mapOf(
        "android.permission.CAMERA" to EnrichedPermission(
            id = "android.permission.CAMERA",
            label = "Camera",
            description = cameraDescription,
            grantType = GrantType.RUNTIME,
        ),
    )

    private fun setUpdateWithCamera(onViewPermission: (String) -> Unit) {
        composeRule.setContent {
            MaterialTheme {
                PermissionCards(
                    diff = PermissionDiff(addedPermissions = listOf("android.permission.CAMERA")),
                    eventType = WatcherEventType.UPDATE,
                    enrichedMap = cameraEnrichedMap,
                    onViewPermission = onViewPermission,
                )
            }
        }
    }

    @Test
    fun `info button navigates and does not toggle the description`() {
        var viewed: String? = null
        setUpdateWithCamera { viewed = it }

        composeRule.onNodeWithContentDescription("View details for Camera").performClick()

        viewed shouldBe "android.permission.CAMERA"
        composeRule.onNodeWithText(cameraDescription).assertDoesNotExist()
    }

    @Test
    fun `clicking the row body toggles the description`() {
        var viewed: String? = null
        setUpdateWithCamera { viewed = it }

        composeRule.onNodeWithText("android.permission.CAMERA", useUnmergedTree = true).performClick()

        composeRule.onNodeWithText(cameraDescription, useUnmergedTree = true).assertIsDisplayed()
        viewed shouldBe null
    }
}
