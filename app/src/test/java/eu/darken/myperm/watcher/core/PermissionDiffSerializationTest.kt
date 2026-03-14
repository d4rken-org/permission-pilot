package eu.darken.myperm.watcher.core

import eu.darken.myperm.apps.core.features.UsesPermission
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.json.toComparableJson

class PermissionDiffSerializationTest : BaseTest() {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `full PermissionDiff roundtrips with stable JSON format`() {
        val raw = """
            {
              "addedPermissions": ["android.permission.CAMERA", "android.permission.INTERNET"],
              "removedPermissions": ["android.permission.READ_CONTACTS"],
              "grantChanges": [
                {
                  "permissionId": "android.permission.LOCATION",
                  "oldStatus": "DENIED",
                  "newStatus": "GRANTED"
                }
              ],
              "addedDeclared": ["com.example.CUSTOM_PERM"],
              "removedDeclared": ["com.example.OLD_PERM"],
              "gainedCount": 0,
              "lostCount": 0
            }
        """.trimIndent()

        val parsed = json.decodeFromString<PermissionDiff>(raw)

        parsed.addedPermissions shouldBe listOf("android.permission.CAMERA", "android.permission.INTERNET")
        parsed.removedPermissions shouldBe listOf("android.permission.READ_CONTACTS")
        parsed.grantChanges.size shouldBe 1
        parsed.grantChanges[0].permissionId shouldBe "android.permission.LOCATION"
        parsed.grantChanges[0].oldStatus shouldBe UsesPermission.Status.DENIED
        parsed.grantChanges[0].newStatus shouldBe UsesPermission.Status.GRANTED
        parsed.addedDeclared shouldBe listOf("com.example.CUSTOM_PERM")
        parsed.removedDeclared shouldBe listOf("com.example.OLD_PERM")
        parsed.gainedCount shouldBe 0
        parsed.lostCount shouldBe 0

        json.encodeToString(parsed).toComparableJson() shouldBe raw.toComparableJson()
    }

    @Test
    fun `empty PermissionDiff roundtrips with stable JSON format`() {
        val raw = """
            {
              "addedPermissions": [],
              "removedPermissions": [],
              "grantChanges": [],
              "addedDeclared": [],
              "removedDeclared": [],
              "gainedCount": 0,
              "lostCount": 0
            }
        """.trimIndent()

        val parsed = json.decodeFromString<PermissionDiff>(raw)

        parsed.addedPermissions shouldBe emptyList()
        parsed.removedPermissions shouldBe emptyList()
        parsed.grantChanges shouldBe emptyList()
        parsed.addedDeclared shouldBe emptyList()
        parsed.removedDeclared shouldBe emptyList()
        parsed.gainedCount shouldBe 0
        parsed.lostCount shouldBe 0

        json.encodeToString(parsed).toComparableJson() shouldBe raw.toComparableJson()
    }

    @Test
    fun `minimal JSON with defaults omitted deserializes correctly`() {
        val raw = """{}"""

        val parsed = json.decodeFromString<PermissionDiff>(raw)

        parsed.addedPermissions shouldBe emptyList()
        parsed.grantChanges shouldBe emptyList()
        parsed.isEmpty shouldBe true
    }

    @Test
    fun `unknown JSON fields are ignored gracefully`() {
        val raw = """
            {
              "addedPermissions": ["android.permission.CAMERA"],
              "futureField": "some_value",
              "anotherNewThing": 42
            }
        """.trimIndent()

        val parsed = json.decodeFromString<PermissionDiff>(raw)

        parsed.addedPermissions shouldBe listOf("android.permission.CAMERA")
        parsed.removedPermissions shouldBe emptyList()
    }

    @Test
    fun `old JSON without gainedCount and lostCount deserializes to zero defaults`() {
        val raw = """
            {
              "addedPermissions": ["android.permission.CAMERA"],
              "removedPermissions": [],
              "grantChanges": [],
              "addedDeclared": [],
              "removedDeclared": []
            }
        """.trimIndent()

        val parsed = json.decodeFromString<PermissionDiff>(raw)

        parsed.gainedCount shouldBe 0
        parsed.lostCount shouldBe 0
    }

    @Test
    fun `new JSON with gainedCount and lostCount roundtrips correctly`() {
        val diff = PermissionDiff(
            addedPermissions = listOf("android.permission.CAMERA"),
            gainedCount = 1,
            lostCount = 2,
        )

        val serialized = json.encodeToString(diff)
        val restored = json.decodeFromString<PermissionDiff>(serialized)

        restored.gainedCount shouldBe 1
        restored.lostCount shouldBe 2
    }

    @Test
    fun `all UsesPermission Status values survive roundtrip`() {
        val changes = UsesPermission.Status.entries.mapIndexed { i, status ->
            PermissionDiff.GrantChange(
                permissionId = "android.permission.TEST_$i",
                oldStatus = UsesPermission.Status.UNKNOWN,
                newStatus = status,
            )
        }
        val diff = PermissionDiff(grantChanges = changes)

        val serialized = json.encodeToString(diff)
        val restored = json.decodeFromString<PermissionDiff>(serialized)

        restored.grantChanges.size shouldBe UsesPermission.Status.entries.size
        restored.grantChanges.forEachIndexed { i, change ->
            change.newStatus shouldBe UsesPermission.Status.entries[i]
        }
    }
}
