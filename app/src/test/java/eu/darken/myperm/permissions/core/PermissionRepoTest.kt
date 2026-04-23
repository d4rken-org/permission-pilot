package eu.darken.myperm.permissions.core

import eu.darken.myperm.apps.core.AppRepo
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider
import testhelper.coroutine.runTest2

class PermissionRepoTest : BaseTest() {

    private fun createRepo(scope: TestScope, appRepo: AppRepo): PermissionRepo = PermissionRepo(
        context = mockk(relaxed = true),
        appScope = scope,
        dispatcherProvider = TestDispatcherProvider(),
        appRepo = appRepo,
        ipcFunnel = mockk(relaxed = true),
    )

    @Test
    fun `scanError with NoSnapshot emits State Error (cascades AppRepo failure)`() = runTest2(autoCancel = true) {
        // This is the central bug fix: a failed initial scan must reach PermissionRepo
        // consumers as State.Error, not as perpetual Loading. The flow emits Loading first
        // via onStart, then Error from the combineTransform — we skip the initial Loading.
        val appRepo: AppRepo = mockk()
        val scanFailure = RuntimeException("scan boom")
        every { appRepo.appData } returns flowOf(AppRepo.AppDataState.NoSnapshot)
        every { appRepo.scanError } returns MutableStateFlow(scanFailure)

        val repo = createRepo(this, appRepo)
        val errorState = repo.state.filter { it !is PermissionRepo.State.Loading }.first()

        errorState.shouldBeInstanceOf<PermissionRepo.State.Error>()
    }

    @Test
    fun `no scanError with NoSnapshot stays in Loading (scan still running)`() = runTest2(autoCancel = true) {
        val appRepo: AppRepo = mockk()
        every { appRepo.appData } returns flowOf(AppRepo.AppDataState.NoSnapshot)
        every { appRepo.scanError } returns MutableStateFlow(null)

        val repo = createRepo(this, appRepo)
        // With no data and no error, the stream only emits Loading — no Ready, no Error.
        // Take the first two emissions and assert both are Loading.
        val emissions = repo.state.take(2).toList()
        emissions.forEach { it.shouldBeInstanceOf<PermissionRepo.State.Loading>() }
    }
}
