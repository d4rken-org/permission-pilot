package eu.darken.myperm.permissions.core

import eu.darken.myperm.permissions.core.container.BasePermission
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class PermissionRepoExtensionsTest : BaseTest() {

    @Test
    fun `permissions flow emits Ready permissions unchanged`() = runTest {
        val repo: PermissionRepo = mockk()
        val permission: BasePermission = mockk()
        every { repo.state } returns flowOf(
            PermissionRepo.State.Ready(permissions = listOf(permission)),
        )

        val emissions = repo.permissions.toList()

        emissions shouldBe listOf(listOf(permission))
    }

    @Test
    fun `permissions flow emits emptyList on Error (does not hang)`() = runTest {
        // Guards downstream consumers (AppDetails, PermissionDetails, Export, IconFetcher)
        // from hanging on .first() / combine when the upstream is in an Error state.
        val repo: PermissionRepo = mockk()
        every { repo.state } returns flowOf(
            PermissionRepo.State.Error(error = RuntimeException("boom")),
        )

        val emissions = repo.permissions.toList()

        emissions shouldBe listOf(emptyList())
    }

    @Test
    fun `permissions flow skips Loading emissions (preserves pre-existing wait-for-Ready semantics)`() = runTest {
        val repo: PermissionRepo = mockk()
        val permission: BasePermission = mockk()
        every { repo.state } returns flowOf(
            PermissionRepo.State.Loading(),
            PermissionRepo.State.Ready(permissions = listOf(permission)),
            PermissionRepo.State.Loading(),
            PermissionRepo.State.Ready(permissions = listOf(permission, mockk())),
        )

        val emissions = repo.permissions.toList()

        // Loading emissions are skipped; Ready emissions pass through.
        emissions.size shouldBe 2
        emissions[0] shouldBe listOf(permission)
        emissions[1].size shouldBe 2
    }
}
