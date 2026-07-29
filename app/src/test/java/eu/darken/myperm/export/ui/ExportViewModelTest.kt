package eu.darken.myperm.export.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import eu.darken.myperm.apps.core.AppRepo
import eu.darken.myperm.common.navigation.Nav
import eu.darken.myperm.common.upgrade.UpgradeRepo
import eu.darken.myperm.export.core.ExportEngine
import eu.darken.myperm.export.core.ExportFormat
import eu.darken.myperm.export.core.ExportSelectionStore
import eu.darken.myperm.export.core.ExportWriter
import eu.darken.myperm.export.core.PermissionExportConfig
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionRepo
import eu.darken.myperm.permissions.core.container.BasePermission
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider

class ExportViewModelTest : BaseTest() {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val context: Context = mockk(relaxed = true)
    private val appRepo: AppRepo = mockk(relaxed = true)
    private val permissionRepo: PermissionRepo = mockk(relaxed = true)
    private val exportEngine: ExportEngine = mockk(relaxed = true)
    private val exportWriter: ExportWriter = mockk(relaxed = true)
    private val exportSelectionStore: ExportSelectionStore = mockk()
    private val upgradeRepo: UpgradeRepo = mockk(relaxed = true)
    private val uri: Uri = mockk(relaxed = true)

    private val permissions = (1..10).map { index ->
        mockk<BasePermission>(relaxed = true).also { every { it.id } returns Permission.Id("perm.$index") }
    }

    /**
     * Hot flow, never a finite `flowOf`: `isProSettled` waits for a pro emission, and a finished
     * flow would push the export gate onto its fail-open timeout path instead of the real decision.
     */
    private val upgradeInfo = MutableStateFlow(info(isPro = false))

    private val route = Nav.Export.Config(token = "token", mode = "permissions")

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { appRepo.appData } returns MutableStateFlow(AppRepo.AppDataState.Ready(emptyList()))
        every { appRepo.scanError } returns MutableStateFlow(null)
        every { permissionRepo.state } returns MutableStateFlow(PermissionRepo.State.Ready(permissions = permissions))
        every { exportSelectionStore.consume("token") } returns permissions.map { it.id.value }
        every { exportEngine.previewPermissions(any(), any()) } returns "preview"
        coEvery { exportEngine.exportPermissions(any(), any()) } returns Result.success("content")
        coEvery { exportWriter.write(any(), any()) } returns Result.success(Unit)
        every { upgradeRepo.upgradeInfo } returns upgradeInfo
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createVM(): ExportViewModel = ExportViewModel(
        handle = SavedStateHandle(),
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        context = context,
        appRepo = appRepo,
        permissionRepo = permissionRepo,
        exportEngine = exportEngine,
        exportWriter = exportWriter,
        exportSelectionStore = exportSelectionStore,
        upgradeRepo = upgradeRepo,
    ).also { it.init(route) }

    private fun exportedPermissions(): Pair<Collection<BasePermission>, PermissionExportConfig> {
        val perms = slot<Collection<BasePermission>>()
        val config = slot<PermissionExportConfig>()
        coVerify(exactly = 1) { exportEngine.exportPermissions(capture(perms), capture(config)) }
        return perms.captured to config.captured
    }

    // --- export execution gate ------------------------------------------------------------------

    @Test
    fun `a settled free user's export is truncated and format-locked`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)
        val vm = createVM()
        vm.updatePermConfig { it.copy(format = ExportFormat.JSON) }

        vm.onSafResult(uri)
        advanceUntilIdle()

        val (exported, config) = exportedPermissions()
        exported.size shouldBe 5
        config.format shouldBe ExportFormat.MARKDOWN
    }

    @Test
    fun `a paying user's cold-start export is neither truncated nor format-locked`() = runTest(testDispatcher) {
        // The screen may be opened before billing settles; the write boundary reconciles instead of
        // silently truncating a paying user's export.
        upgradeInfo.value = info(isPro = false, isSettled = false)
        coEvery { upgradeRepo.refresh() } answers { upgradeInfo.value = info(isPro = true) }
        val vm = createVM()
        vm.updatePermConfig { it.copy(format = ExportFormat.JSON) }

        vm.onSafResult(uri)
        advanceUntilIdle()

        val (exported, config) = exportedPermissions()
        exported.size shouldBe 10
        config.format shouldBe ExportFormat.JSON
    }

    @Test
    fun `a known pro user's export is complete`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = true)
        val vm = createVM()
        vm.updatePermConfig { it.copy(format = ExportFormat.JSON) }

        vm.onSafResult(uri)
        advanceUntilIdle()

        val (exported, config) = exportedPermissions()
        exported.size shouldBe 10
        config.format shouldBe ExportFormat.JSON
    }

    // --- preview presentation -------------------------------------------------------------------

    @Test
    fun `the preview is not truncated during the unsettled cold start`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = false)
        val vm = createVM()
        advanceUntilIdle()

        vm.isPro.first() shouldBe true
        verify { exportEngine.previewPermissions(match { it.size == 10 }, any()) }
    }

    @Test
    fun `the preview is truncated for a settled free user`() = runTest(testDispatcher) {
        upgradeInfo.value = info(isPro = false, isSettled = true)
        val vm = createVM()
        advanceUntilIdle()

        vm.isPro.first() shouldBe false
        verify { exportEngine.previewPermissions(match { it.size == 5 }, any()) }
    }

    @Test
    fun `billing settling free after the first preview re-locks preview and state`() = runTest(testDispatcher) {
        // The initial presentation is optimistic (unlocked while unsettled). Once billing settles
        // free, the presentation has to catch up with the execution gate instead of keeping the
        // unlocked UI for the rest of the screen's life.
        upgradeInfo.value = info(isPro = false, isSettled = false)
        val vm = createVM()
        vm.updatePermConfig { it.copy(format = ExportFormat.JSON) }
        // One uninterrupted subscription across the transition: re-subscribing would recompute the
        // state anyway and hide a non-reactive pro input.
        val states = mutableListOf<ExportViewModel.State?>()
        backgroundScope.launch { vm.state.toList(states) }
        advanceUntilIdle()

        verify { exportEngine.previewPermissions(match { it.size == 10 }, any()) }
        val optimistic = states.last()!!
        optimistic.isPro shouldBe true
        optimistic.effectiveItemCount shouldBe 10
        optimistic.isFreeLimited shouldBe false
        optimistic.permConfig!!.format shouldBe ExportFormat.JSON

        upgradeInfo.value = info(isPro = false, isSettled = true)
        advanceUntilIdle()

        vm.isPro.first() shouldBe false
        verify { exportEngine.previewPermissions(match { it.size == 5 }, any()) }
        val locked = states.last()!!
        locked.isPro shouldBe false
        locked.effectiveItemCount shouldBe 5
        locked.isFreeLimited shouldBe true
        locked.permConfig!!.format shouldBe ExportFormat.MARKDOWN
    }

    companion object {
        private fun info(
            isPro: Boolean,
            isSettled: Boolean = true,
            error: Throwable? = null,
        ): UpgradeRepo.Info = mockk<UpgradeRepo.Info>(relaxed = true).also {
            every { it.isPro } returns isPro
            every { it.isSettled } returns isSettled
            every { it.error } returns error
            every { it.type } returns UpgradeRepo.Type.GPLAY
        }
    }
}
