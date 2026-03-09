package eu.darken.myperm.settings.ui.support

import eu.darken.myperm.common.debug.recording.core.DebugSession
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant

class SupportViewModelStateTest {

    @Test
    fun `logSessionCount excludes recording sessions`() {
        val state = SupportViewModel.State(
            sessions = listOf(
                DebugSession.Recording(
                    id = "rec1", displayName = "rec1", createdAt = Instant.now(),
                    diskSize = 100, path = File("/tmp/rec1"), startedAt = 0L,
                ),
                DebugSession.Ready(
                    id = "ready1", displayName = "ready1", createdAt = Instant.now(),
                    diskSize = 200,
                ),
                DebugSession.Failed(
                    id = "fail1", displayName = "fail1", createdAt = Instant.now(),
                    diskSize = 50, path = File("/tmp/fail1"),
                    reason = DebugSession.Failed.Reason.EMPTY_LOG,
                ),
            )
        )
        state.logSessionCount shouldBe 2
    }

    @Test
    fun `logFolderSize excludes recording sessions`() {
        val state = SupportViewModel.State(
            sessions = listOf(
                DebugSession.Recording(
                    id = "rec1", displayName = "rec1", createdAt = Instant.now(),
                    diskSize = 100, path = File("/tmp/rec1"), startedAt = 0L,
                ),
                DebugSession.Ready(
                    id = "ready1", displayName = "ready1", createdAt = Instant.now(),
                    diskSize = 200,
                ),
                DebugSession.Ready(
                    id = "ready2", displayName = "ready2", createdAt = Instant.now(),
                    diskSize = 300,
                ),
            )
        )
        state.logFolderSize shouldBe 500L
    }

    @Test
    fun `failedSessions returns only Failed`() {
        val failed = DebugSession.Failed(
            id = "fail1", displayName = "fail1", createdAt = Instant.now(),
            diskSize = 50, path = File("/tmp/fail1"),
            reason = DebugSession.Failed.Reason.ZIP_FAILED,
        )
        val state = SupportViewModel.State(
            sessions = listOf(
                DebugSession.Ready(
                    id = "ready1", displayName = "ready1", createdAt = Instant.now(),
                    diskSize = 200,
                ),
                failed,
            )
        )
        state.failedSessions shouldBe listOf(failed)
    }

    @Test
    fun `hasAnySessions is false for empty list`() {
        val state = SupportViewModel.State(sessions = emptyList())
        state.hasAnySessions shouldBe false
    }

    @Test
    fun `hasAnySessions is true with sessions`() {
        val state = SupportViewModel.State(
            sessions = listOf(
                DebugSession.Ready(
                    id = "ready1", displayName = "ready1", createdAt = Instant.now(),
                    diskSize = 200,
                ),
            )
        )
        state.hasAnySessions shouldBe true
    }
}
