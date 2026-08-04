package eu.darken.myperm.settings.ui.support

import eu.darken.myperm.common.WebpageTool
import eu.darken.myperm.common.debug.recording.core.DebugSession
import eu.darken.myperm.common.debug.recording.core.DebugSessionManager
import eu.darken.myperm.common.debug.recording.core.RecorderModule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.coroutine.TestDispatcherProvider
import java.io.IOException

/**
 * A debug recording that cannot start is the user's own next problem: the toggle has to come back
 * with the error dialog instead of leaving the screen waiting on a recording that never starts.
 */
class SupportViewModelDebugLogTest : BaseTest() {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val debugSessionManager: DebugSessionManager = mockk()
    private val webpageTool: WebpageTool = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { debugSessionManager.recorderState } returns MutableStateFlow(RecorderModule.State())
        every { debugSessionManager.sessions } returns MutableStateFlow(emptyList<DebugSession>())
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createVM() = SupportViewModel(
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        webpageTool = webpageTool,
        debugSessionManager = debugSessionManager,
    )

    @Test
    fun `a failed recording start reaches the error events`() = runTest(testDispatcher) {
        val failure = IOException("Failed to create session directory")
        coEvery { debugSessionManager.startRecording() } throws failure

        val vm = createVM()
        val forwardedError = async { vm.errorEvents.first() }

        vm.startDebugLog()

        forwardedError.await() shouldBe failure
    }
}
