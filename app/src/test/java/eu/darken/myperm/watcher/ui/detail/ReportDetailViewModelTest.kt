package eu.darken.myperm.watcher.ui.detail

import android.content.Context
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.navigation.NavEvent
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.watcher.core.PermissionDiff
import eu.darken.myperm.watcher.core.WatcherEventType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider

class ReportDetailViewModelTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()

    private val changeDao: PermissionChangeDao = mockk(relaxed = true)
    private val snapshotPkgDao: SnapshotPkgDao = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val json: Json = Json { ignoreUnknownKeys = true }

    private val locationId = "android.permission.ACCESS_FINE_LOCATION"

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // ACCESS_FINE_LOCATION resolves via the app's own known-permission table, no PackageManager needed.
        every { context.getString(R.string.permission_access_precise_location_label) } returns "Precise location"
        coEvery { changeDao.getById(REPORT_ID) } returns PermissionChangeEntity(
            id = REPORT_ID,
            packageName = Pkg.Name("com.example.app"),
            userHandleId = 0,
            appLabel = "Example App",
            versionCode = 2L,
            versionName = "2.0",
            eventType = WatcherEventType.UPDATE,
            changesJson = json.encodeToString(PermissionDiff(addedPermissions = listOf(locationId))),
            detectedAt = 1000L,
        )
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createVM() = ReportDetailViewModel(
        reportId = REPORT_ID,
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        changeDao = changeDao,
        snapshotPkgDao = snapshotPkgDao,
        context = context,
        json = json,
    )

    @Test
    fun `viewing a permission carries the enriched label`() = runTest(testDispatcher) {
        val vm = createVM()
        val state = vm.state.first { !it.isLoading }
        state.permissionInfoMap[locationId]?.label shouldBe "Precise location"

        vm.onViewPermission(locationId)

        val event = vm.navEvents.first().shouldBeInstanceOf<NavEvent.GoTo>()
        event.destination shouldBe Nav.Details.PermissionDetails(
            permissionId = locationId,
            permLabel = "Precise location",
        )
    }

    @Test
    fun `viewing an unknown permission has no label`() = runTest(testDispatcher) {
        val vm = createVM()
        vm.state.first { !it.isLoading }

        vm.onViewPermission("com.example.permission.CUSTOM")

        val event = vm.navEvents.first().shouldBeInstanceOf<NavEvent.GoTo>()
        event.destination shouldBe Nav.Details.PermissionDetails(
            permissionId = "com.example.permission.CUSTOM",
            permLabel = null,
        )
    }

    companion object {
        private const val REPORT_ID = 42L
    }
}
