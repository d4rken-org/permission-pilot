package eu.darken.myperm.apps.ui.details

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageInfo
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.apps.core.manifest.ManifestHintRepo
import eu.darken.myperm.apps.core.tryCreateUserHandle
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.settings.core.GeneralSettings
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider
import testhelpers.TestApplication

/**
 * `startAppDetailsActivity` is a void binder call: when the package is not resolvable for the target
 * user, Settings starts, fails to resolve it and finishes itself in another process. Nothing throws
 * here, so the only observable behaviour to test is that the call is not made at all.
 *
 * Robolectric, like PP's other framework-touching tests: the ViewModel talks to LauncherApps,
 * PackageManager and Toast, and constructing it initializes
 * [eu.darken.myperm.apps.core.known.AKnownPkg], whose `Pkg.Id`s default their userHandle to
 * `Process.myUserHandle()` (null on a bare JVM).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class AppDetailsViewModelTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    private val appRepo: AppRepo = mockk(relaxed = true)
    private val permissionRepo: PermissionRepo = mockk(relaxed = true)
    private val generalSettings: GeneralSettings = mockk(relaxed = true)
    private val manifestHintRepo: ManifestHintRepo = mockk(relaxed = true)
    private val upgradeRepo: UpgradeRepo = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // `vm.state` is never collected here, so no repository flow has to emit.
    private fun createVM() = AppDetailsViewModel(
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        context = context,
        appRepo = appRepo,
        permissionRepo = permissionRepo,
        generalSettings = generalSettings,
        manifestHintRepo = manifestHintRepo,
        upgradeRepo = upgradeRepo,
    )

    private fun createVM(userHandleId: Int) = createVM().apply {
        init(Nav.Details.AppDetails(pkgName = TEST_PKG, userHandle = userHandleId, appLabel = null))
    }

    private fun installForCurrentUser() {
        val pkgInfo = PackageInfo().apply {
            packageName = TEST_PKG
            applicationInfo = ApplicationInfo().apply {
                packageName = TEST_PKG
                name = TEST_PKG
            }
        }
        shadowOf(context.packageManager).installPackage(pkgInfo)
    }

    private fun otherUserHandle(): UserHandle = context
        .getSystemService(UserManager::class.java)!!
        .tryCreateUserHandle(OTHER_USER_ID)!!

    private val unavailableText: String
        get() = context.getString(R.string.apps_details_open_settings_unavailable)

    @Test
    fun `current user, package not installed, shows the unavailable toast`() {
        val vm = createVM(userHandleId = Process.myUserHandle().hashCode())

        vm.onGoSettings()

        ShadowToast.getTextOfLatestToast() shouldBe unavailableText
    }

    @Test
    fun `current user, package installed, is not blocked`() {
        installForCurrentUser()
        val vm = createVM(userHandleId = Process.myUserHandle().hashCode())

        vm.onGoSettings()

        ShadowToast.getTextOfLatestToast() shouldNotBe unavailableText
    }

    @Test
    fun `other user, package present for that user, is not blocked`() {
        val otherHandle = otherUserHandle()
        shadowOf(context.getSystemService(LauncherApps::class.java))
            .addApplicationInfo(otherHandle, TEST_PKG, ApplicationInfo())
        val vm = createVM(userHandleId = otherHandle.hashCode())

        vm.onGoSettings()

        ShadowToast.getTextOfLatestToast() shouldNotBe unavailableText
    }

    @Test
    fun `other user, package absent for that user, shows the unavailable toast`() {
        val otherHandle = otherUserHandle()
        val vm = createVM(userHandleId = otherHandle.hashCode())

        vm.onGoSettings()

        ShadowToast.getTextOfLatestToast() shouldBe unavailableText
    }

    companion object {
        private const val TEST_PKG = "eu.darken.test.pkg"
        private const val OTHER_USER_ID = 10
    }
}
