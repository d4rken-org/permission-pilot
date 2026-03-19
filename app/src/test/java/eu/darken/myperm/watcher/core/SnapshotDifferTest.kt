package eu.darken.myperm.watcher.core

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class SnapshotDifferTest : BaseTest() {

    private val differ = SnapshotDiffer()
    private val snapshotId = "snapshot-1"
    private val pkgName = Pkg.Name("com.example.app")
    private val userHandleId = 0

    private fun perm(id: String, status: String) = SnapshotPkgPermEntity(
        snapshotId = snapshotId,
        pkgName = pkgName,
        userHandleId = userHandleId,
        permissionId = id,
        status = status,
    )

    private fun declared(id: String) = SnapshotPkgDeclaredPermEntity(
        snapshotId = snapshotId,
        pkgName = pkgName,
        userHandleId = userHandleId,
        permissionId = id,
        protectionLevel = null,
    )

    private fun current(id: String, status: String) = SnapshotDiffer.CurrentPermission(id, UsesPermission.Status.valueOf(status))

    @Test
    fun `empty previous and empty current produces empty diff`() {
        val diff = differ.diff(
            previousPerms = emptyList(),
            previousDeclared = emptyList(),
            currentPerms = emptyList(),
            currentDeclared = emptyList(),
        )

        diff.isEmpty shouldBe true
        diff.addedPermissions.shouldBeEmpty()
        diff.removedPermissions.shouldBeEmpty()
        diff.grantChanges.shouldBeEmpty()
        diff.addedDeclared.shouldBeEmpty()
        diff.removedDeclared.shouldBeEmpty()
    }

    @Test
    fun `no previous snapshot treated as empty - new permissions show as added`() {
        val diff = differ.diff(
            previousPerms = emptyList(),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.CAMERA", "GRANTED"),
                current("android.permission.INTERNET", "DENIED"),
            ),
            currentDeclared = listOf("com.example.MY_PERM"),
        )

        diff.addedPermissions.shouldContainExactlyInAnyOrder(
            "android.permission.CAMERA",
            "android.permission.INTERNET",
        )
        diff.removedPermissions.shouldBeEmpty()
        diff.grantChanges.shouldBeEmpty()
        diff.addedDeclared.shouldContainExactlyInAnyOrder("com.example.MY_PERM")
        diff.removedDeclared.shouldBeEmpty()
    }

    @Test
    fun `no changes produces empty diff`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "GRANTED"),
                perm("android.permission.INTERNET", "GRANTED"),
            ),
            previousDeclared = listOf(declared("com.example.MY_PERM")),
            currentPerms = listOf(
                current("android.permission.CAMERA", "GRANTED"),
                current("android.permission.INTERNET", "GRANTED"),
            ),
            currentDeclared = listOf("com.example.MY_PERM"),
        )

        diff.isEmpty shouldBe true
    }

    @Test
    fun `detects added permissions`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.INTERNET", "GRANTED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.INTERNET", "GRANTED"),
                current("android.permission.CAMERA", "DENIED"),
                current("android.permission.LOCATION", "GRANTED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.addedPermissions.shouldContainExactlyInAnyOrder(
            "android.permission.CAMERA",
            "android.permission.LOCATION",
        )
        diff.removedPermissions.shouldBeEmpty()
    }

    @Test
    fun `detects removed permissions`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.INTERNET", "GRANTED"),
                perm("android.permission.CAMERA", "GRANTED"),
                perm("android.permission.LOCATION", "DENIED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.INTERNET", "GRANTED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.removedPermissions.shouldContainExactlyInAnyOrder(
            "android.permission.CAMERA",
            "android.permission.LOCATION",
        )
        diff.addedPermissions.shouldBeEmpty()
    }

    @Test
    fun `detects grant status changes`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "DENIED"),
                perm("android.permission.INTERNET", "GRANTED"),
                perm("android.permission.LOCATION", "GRANTED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.CAMERA", "GRANTED"),
                current("android.permission.INTERNET", "DENIED"),
                current("android.permission.LOCATION", "GRANTED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.addedPermissions.shouldBeEmpty()
        diff.removedPermissions.shouldBeEmpty()
        diff.grantChanges.size shouldBe 2

        val cameraChange = diff.grantChanges.first { it.permissionId == "android.permission.CAMERA" }
        cameraChange.oldStatus shouldBe UsesPermission.Status.DENIED
        cameraChange.newStatus shouldBe UsesPermission.Status.GRANTED

        val internetChange = diff.grantChanges.first { it.permissionId == "android.permission.INTERNET" }
        internetChange.oldStatus shouldBe UsesPermission.Status.GRANTED
        internetChange.newStatus shouldBe UsesPermission.Status.DENIED
    }

    @Test
    fun `detects added declared permissions`() {
        val diff = differ.diff(
            previousPerms = emptyList(),
            previousDeclared = listOf(declared("com.example.PERM_A")),
            currentPerms = emptyList(),
            currentDeclared = listOf("com.example.PERM_A", "com.example.PERM_B"),
        )

        diff.addedDeclared.shouldContainExactlyInAnyOrder("com.example.PERM_B")
        diff.removedDeclared.shouldBeEmpty()
    }

    @Test
    fun `detects removed declared permissions`() {
        val diff = differ.diff(
            previousPerms = emptyList(),
            previousDeclared = listOf(
                declared("com.example.PERM_A"),
                declared("com.example.PERM_B"),
            ),
            currentPerms = emptyList(),
            currentDeclared = listOf("com.example.PERM_A"),
        )

        diff.removedDeclared.shouldContainExactlyInAnyOrder("com.example.PERM_B")
        diff.addedDeclared.shouldBeEmpty()
    }

    @Test
    fun `detects combined changes - added, removed, and grant changes together`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "DENIED"),
                perm("android.permission.INTERNET", "GRANTED"),
            ),
            previousDeclared = listOf(declared("com.example.OLD_PERM")),
            currentPerms = listOf(
                perm("android.permission.CAMERA", "GRANTED").let { current(it.permissionId, it.status) },
                current("android.permission.LOCATION", "GRANTED"),
            ),
            currentDeclared = listOf("com.example.NEW_PERM"),
        )

        diff.isEmpty shouldBe false
        diff.addedPermissions.shouldContainExactlyInAnyOrder("android.permission.LOCATION")
        diff.removedPermissions.shouldContainExactlyInAnyOrder("android.permission.INTERNET")
        diff.grantChanges.size shouldBe 1
        diff.grantChanges[0].permissionId shouldBe "android.permission.CAMERA"
        diff.grantChanges[0].oldStatus shouldBe UsesPermission.Status.DENIED
        diff.grantChanges[0].newStatus shouldBe UsesPermission.Status.GRANTED
        diff.addedDeclared.shouldContainExactlyInAnyOrder("com.example.NEW_PERM")
        diff.removedDeclared.shouldContainExactlyInAnyOrder("com.example.OLD_PERM")
    }

    @Test
    fun `install - gainedCount only counts granted permissions`() {
        val diff = differ.diff(
            previousPerms = emptyList(),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.CAMERA", "GRANTED"),
                current("android.permission.INTERNET", "GRANTED"),
                current("android.permission.LOCATION", "DENIED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 2
        diff.lostCount shouldBe 0
    }

    @Test
    fun `install - denied permissions not counted as gained`() {
        val diff = differ.diff(
            previousPerms = emptyList(),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.CAMERA", "DENIED"),
                current("android.permission.INTERNET", "DENIED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 0
        diff.lostCount shouldBe 0
    }

    @Test
    fun `update - added granted permission counted as gained`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.INTERNET", "GRANTED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.INTERNET", "GRANTED"),
                current("android.permission.CAMERA", "GRANTED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 1
        diff.lostCount shouldBe 0
    }

    @Test
    fun `update - added denied permission not counted as gained`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.INTERNET", "GRANTED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.INTERNET", "GRANTED"),
                current("android.permission.CAMERA", "DENIED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 0
        diff.lostCount shouldBe 0
    }

    @Test
    fun `grant change - DENIED to GRANTED counted as gained`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "DENIED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.CAMERA", "GRANTED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 1
        diff.lostCount shouldBe 0
    }

    @Test
    fun `grant change - GRANTED to DENIED counted as lost`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "GRANTED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.CAMERA", "DENIED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 0
        diff.lostCount shouldBe 1
    }

    @Test
    fun `grant change - DENIED to UNKNOWN not counted`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "DENIED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.CAMERA", "UNKNOWN"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 0
        diff.lostCount shouldBe 0
    }

    @Test
    fun `grant change - GRANTED to GRANTED_IN_USE not counted`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "GRANTED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.CAMERA", "GRANTED_IN_USE"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 0
        diff.lostCount shouldBe 0
    }

    @Test
    fun `removed granted permission counted as lost`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "GRANTED"),
                perm("android.permission.INTERNET", "GRANTED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.INTERNET", "GRANTED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 0
        diff.lostCount shouldBe 1
    }

    @Test
    fun `removed denied permission not counted as lost`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "DENIED"),
                perm("android.permission.INTERNET", "GRANTED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.INTERNET", "GRANTED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 0
        diff.lostCount shouldBe 0
    }

    @Test
    fun `mixed - gains and losses combined correctly`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "DENIED"),
                perm("android.permission.INTERNET", "GRANTED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.CAMERA", "GRANTED"),
                current("android.permission.LOCATION", "GRANTED"),
            ),
            currentDeclared = emptyList(),
        )

        diff.gainedCount shouldBe 2
        diff.lostCount shouldBe 1
    }

    @Test
    fun `GRANTED_IN_USE status is preserved in diff`() {
        val diff = differ.diff(
            previousPerms = listOf(
                perm("android.permission.CAMERA", "GRANTED"),
            ),
            previousDeclared = emptyList(),
            currentPerms = listOf(
                current("android.permission.CAMERA", "GRANTED_IN_USE"),
            ),
            currentDeclared = emptyList(),
        )

        diff.grantChanges.size shouldBe 1
        diff.grantChanges[0].oldStatus shouldBe UsesPermission.Status.GRANTED
        diff.grantChanges[0].newStatus shouldBe UsesPermission.Status.GRANTED_IN_USE
    }
}
