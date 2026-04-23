package eu.darken.myperm.permissions.core.features

import android.os.Build
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.APerm
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class PermissionActionTest : BaseTest() {

    @Test
    fun `every launchableSpecialId produces a non-empty intent list on API 34`() {
        PermissionAction.launchableSpecialIds.forEach { id ->
            withClue(id) {
                PermissionAction.buildSpecialIntents(id, pkg = null, apiLevel = 34).shouldNotBeEmpty()
            }
        }
    }

    @Test
    fun `canLaunchSettings is true for every launchableSpecialId on API 34`() {
        PermissionAction.launchableSpecialIds.forEach { id ->
            withClue(id) {
                PermissionAction.canLaunchSettings(id, apiLevel = 34) shouldBe true
            }
        }
    }

    @Test
    fun `canLaunchSettings is false for tagged-but-unsupported special permissions`() {
        val tagDriftFalsePositives = listOf(
            APerm.CHANGE_WIFI_STATE.id,
            APerm.TURN_SCREEN_ON.id,
            APerm.INTERACT_ACROSS_PROFILES.id,
            APerm.LOADER_USAGE_STATS.id,
            APerm.USE_FULL_SCREEN_INTENT.id,
        )
        tagDriftFalsePositives.forEach { id ->
            withClue(id) {
                PermissionAction.canLaunchSettings(id, apiLevel = 34) shouldBe false
            }
        }
    }

    @Test
    fun `canLaunchSettings is false for arbitrary runtime permissions`() {
        val runtimeIds = listOf(
            Permission.Id("android.permission.CAMERA"),
            Permission.Id("android.permission.READ_CONTACTS"),
            Permission.Id("android.permission.ACCESS_FINE_LOCATION"),
            Permission.Id("com.example.unknown.permission"),
        )
        runtimeIds.forEach { id ->
            withClue(id) {
                PermissionAction.canLaunchSettings(id, apiLevel = 34) shouldBe false
            }
        }
    }

    @Test
    fun `MANAGE_EXTERNAL_STORAGE is gated behind R`() {
        PermissionAction
            .buildSpecialIntents(APerm.MANAGE_EXTERNAL_STORAGE.id, pkg = null, apiLevel = Build.VERSION_CODES.Q)
            .shouldBeEmpty()
        PermissionAction
            .buildSpecialIntents(APerm.MANAGE_EXTERNAL_STORAGE.id, pkg = null, apiLevel = Build.VERSION_CODES.R)
            .shouldHaveSize(1)
    }

    @Test
    fun `MANAGE_MEDIA is gated behind S`() {
        PermissionAction
            .buildSpecialIntents(APerm.MANAGE_MEDIA.id, pkg = null, apiLevel = Build.VERSION_CODES.R)
            .shouldBeEmpty()
        PermissionAction
            .buildSpecialIntents(APerm.MANAGE_MEDIA.id, pkg = null, apiLevel = Build.VERSION_CODES.S)
            .shouldHaveSize(1)
    }

    @Test
    fun `SCHEDULE_EXACT_ALARM is gated behind S`() {
        PermissionAction
            .buildSpecialIntents(APerm.SCHEDULE_EXACT_ALARM.id, pkg = null, apiLevel = Build.VERSION_CODES.R)
            .shouldBeEmpty()
        PermissionAction
            .buildSpecialIntents(APerm.SCHEDULE_EXACT_ALARM.id, pkg = null, apiLevel = Build.VERSION_CODES.S)
            .shouldHaveSize(1)
    }

    @Test
    fun `REQUEST_INSTALL_PACKAGES is gated behind O`() {
        PermissionAction
            .buildSpecialIntents(APerm.REQUEST_INSTALL_PACKAGES.id, pkg = null, apiLevel = Build.VERSION_CODES.N)
            .shouldBeEmpty()
        PermissionAction
            .buildSpecialIntents(APerm.REQUEST_INSTALL_PACKAGES.id, pkg = null, apiLevel = Build.VERSION_CODES.O)
            .shouldHaveSize(1)
    }

    @Test
    fun `REQUEST_COMPANION_USE_DATA_IN_BACKGROUND is gated behind N`() {
        PermissionAction
            .buildSpecialIntents(APerm.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND.id, pkg = null, apiLevel = Build.VERSION_CODES.M)
            .shouldBeEmpty()
        PermissionAction
            .buildSpecialIntents(APerm.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND.id, pkg = null, apiLevel = Build.VERSION_CODES.N)
            .shouldHaveSize(1)
    }

    @Test
    fun `DND has a 2-entry fallback chain (public action plus private component)`() {
        // Index 0 is public Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS;
        // index 1 is the ZenAccessSettingsActivity component fallback.
        // Intent content is verified by code review — the android.jar stubs used by unit tests
        // return default values for getAction()/component, so we assert the shape only.
        PermissionAction.buildSpecialIntents(
            APerm.ACCESS_NOTIFICATION_POLICY.id,
            pkg = null,
            apiLevel = 34,
        ) shouldHaveSize 2
    }

    @Test
    fun `notification-listener has a 2-entry fallback chain`() {
        PermissionAction.buildSpecialIntents(
            APerm.ACCESS_NOTIFICATIONS.id,
            pkg = null,
            apiLevel = 34,
        ) shouldHaveSize 2
    }

    private inline fun withClue(context: Any?, block: () -> Unit) {
        try {
            block()
        } catch (e: AssertionError) {
            throw AssertionError("For $context: ${e.message}", e)
        }
    }
}
