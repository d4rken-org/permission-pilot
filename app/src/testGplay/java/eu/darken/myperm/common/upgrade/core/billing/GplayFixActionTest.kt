package eu.darken.myperm.common.upgrade.core.billing

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import eu.darken.myperm.R
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import testhelper.BaseTest
import testhelpers.TestApplication

/**
 * The error dialog's "Google Play" button runs on an activity context: a device where the launch is
 * refused must get a toast, not a crash. The intent shape of a successful launch is pinned by
 * ComposeErrorDialogTest, which drives the same action through the dialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class GplayFixActionTest : BaseTest() {

    /** Play is installed but unreachable: disabled app, restricted profile or a guarding ROM. */
    class DeniedLaunchActivity : Activity() {
        override fun startActivity(intent: Intent): Unit = throw SecurityException("Permission Denial")
    }

    /** No component resolves the app-info screen for Play, e.g. Play isn't installed at all. */
    class MissingPlayActivity : Activity() {
        override fun startActivity(intent: Intent): Unit = throw ActivityNotFoundException("No Activity found")
    }

    private fun fixActionOf(context: Context): (Activity) -> Unit =
        GplayServiceUnavailableException(RuntimeException("Play hiccup"))
            .getLocalizedError(context).fixAction.shouldNotBeNull()

    private fun <T : Activity> activityOf(clazz: Class<T>): T = Robolectric.buildActivity(clazz).setup().get()

    private fun assertToastInsteadOfCrash(activity: Activity) {
        fixActionOf(activity).invoke(activity)

        ShadowToast.getTextOfLatestToast() shouldBe
            activity.getString(R.string.upgrades_gplay_not_installed_message)
    }

    @Test
    fun `a denied launch shows the not-installed toast instead of crashing`() {
        assertToastInsteadOfCrash(activityOf(DeniedLaunchActivity::class.java))
    }

    @Test
    fun `an unresolvable launch shows the not-installed toast instead of crashing`() {
        assertToastInsteadOfCrash(activityOf(MissingPlayActivity::class.java))
    }
}
