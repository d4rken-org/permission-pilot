package eu.darken.myperm.permissions.ui.list

import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.room.entity.PkgType
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.container.ExtraPermission
import eu.darken.myperm.permissions.core.container.PermissionAppRef
import eu.darken.myperm.permissions.core.features.InstallTimeGrant
import eu.darken.myperm.permissions.core.features.ManifestDoc
import eu.darken.myperm.permissions.core.features.PermissionTag
import eu.darken.myperm.permissions.core.features.RuntimeGrant
import eu.darken.myperm.permissions.core.features.SpecialAccess
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.json.toComparableJson

class PermsFilterOptionsTest : BaseTest() {

    private val json = Json { encodeDefaults = true }

    // --- Serialization tests ---

    @Test
    fun `default PermsFilterOptions produces expected JSON`() {
        val serialized = json.encodeToString(PermsFilterOptions())
        serialized.toComparableJson() shouldBe """
            {
                "filters": [
                    "MANIFEST",
                    "SYSTEM",
                    "NOT_INSTALLTIME"
                ]
            }
        """.toComparableJson()
    }

    @Test
    fun `all Filter enum values serialize to their SerialName`() {
        PermsFilterOptions.Filter.entries.forEach { filter ->
            val opts = PermsFilterOptions(filters = setOf(filter))
            val serialized = json.encodeToString(opts)
            serialized.toComparableJson() shouldBe """
                {
                    "filters": [
                        "${filter.name}"
                    ]
                }
            """.toComparableJson()
        }
    }

    @Test
    fun `all Sort enum values serialize to their SerialName`() {
        PermsSortOptions.Sort.entries.forEach { sort ->
            val opts = PermsSortOptions(mainSort = sort)
            val serialized = json.encodeToString(opts)
            serialized.toComparableJson() shouldBe """
                {
                    "mainSort": "${sort.name}",
                    "reversed": false
                }
            """.toComparableJson()
        }
    }

    @Test
    fun `PermsFilterOptions with all filters produces expected JSON`() {
        val allFilters = PermsFilterOptions(filters = PermsFilterOptions.Filter.entries.toSet())
        val serialized = json.encodeToString(allFilters)
        serialized.toComparableJson() shouldBe """
            {
                "filters": [
                    "MANIFEST",
                    "SYSTEM",
                    "USER",
                    "RUNTIME",
                    "INSTALLTIME",
                    "NOT_INSTALLTIME",
                    "SPECIAL_ACCESS"
                ]
            }
        """.toComparableJson()
    }

    @Test
    fun `PermsFilterOptions round-trips through JSON`() {
        val original = PermsFilterOptions(
            filters = setOf(PermsFilterOptions.Filter.RUNTIME, PermsFilterOptions.Filter.SPECIAL_ACCESS)
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<PermsFilterOptions>(serialized)
        deserialized shouldBe original
    }

    // --- Filter logic tests ---

    private fun perm(
        tags: Collection<PermissionTag> = emptyList(),
        hasSystemDeclaringApp: Boolean = false,
    ) = ExtraPermission(
        id = Permission.Id("android.permission.TEST"),
        tags = tags,
        groupIds = emptyList(),
        declaringApps = if (hasSystemDeclaringApp) {
            listOf(
                PermissionAppRef(
                    pkgName = "com.android.system",
                    userHandleId = 0,
                    label = "System",
                    isSystemApp = true,
                    status = UsesPermission.Status.GRANTED,
                    pkgType = PkgType.PRIMARY,
                )
            )
        } else {
            listOf(
                PermissionAppRef(
                    pkgName = "com.example.app",
                    userHandleId = 0,
                    label = "User App",
                    isSystemApp = false,
                    status = UsesPermission.Status.GRANTED,
                    pkgType = PkgType.PRIMARY,
                )
            )
        },
    )

    @Test
    fun `empty filters match all permissions`() {
        val options = PermsFilterOptions(filters = emptySet())
        options.matches(perm(tags = listOf(ManifestDoc, RuntimeGrant))) shouldBe true
        options.matches(perm(tags = listOf(InstallTimeGrant))) shouldBe true
        options.matches(perm()) shouldBe true
    }

    @Test
    fun `MANIFEST and SYSTEM are OR-ed within SOURCE group`() {
        val options = PermsFilterOptions(
            filters = setOf(PermsFilterOptions.Filter.MANIFEST, PermsFilterOptions.Filter.SYSTEM)
        )
        // ManifestDoc tag, not system declaring → matches MANIFEST
        options.matches(perm(tags = listOf(ManifestDoc))) shouldBe true
        // No ManifestDoc, system declaring → matches SYSTEM
        options.matches(perm(hasSystemDeclaringApp = true)) shouldBe true
        // Neither ManifestDoc nor system declaring → fails SOURCE group
        options.matches(perm()) shouldBe false
    }

    @Test
    fun `RUNTIME and SPECIAL_ACCESS are OR-ed within TYPE group`() {
        val options = PermsFilterOptions(
            filters = setOf(PermsFilterOptions.Filter.RUNTIME, PermsFilterOptions.Filter.SPECIAL_ACCESS)
        )
        options.matches(perm(tags = listOf(RuntimeGrant))) shouldBe true
        options.matches(perm(tags = listOf(SpecialAccess))) shouldBe true
        options.matches(perm(tags = listOf(InstallTimeGrant))) shouldBe false
    }

    @Test
    fun `SOURCE and TYPE groups are AND-ed`() {
        val options = PermsFilterOptions(
            filters = setOf(PermsFilterOptions.Filter.MANIFEST, PermsFilterOptions.Filter.RUNTIME)
        )
        // ManifestDoc + RuntimeGrant → matches both groups
        options.matches(perm(tags = listOf(ManifestDoc, RuntimeGrant))) shouldBe true
        // ManifestDoc only → fails TYPE group
        options.matches(perm(tags = listOf(ManifestDoc))) shouldBe false
        // RuntimeGrant only → fails SOURCE group
        options.matches(perm(tags = listOf(RuntimeGrant))) shouldBe false
    }

    @Test
    fun `default filters work correctly`() {
        val options = PermsFilterOptions() // MANIFEST, SYSTEM, NOT_INSTALLTIME
        // ManifestDoc + RuntimeGrant, system app → matches SOURCE (MANIFEST or SYSTEM) and TYPE (NOT_INSTALLTIME)
        options.matches(perm(tags = listOf(ManifestDoc, RuntimeGrant), hasSystemDeclaringApp = true)) shouldBe true
        // ManifestDoc + InstallTimeGrant → fails TYPE group (NOT_INSTALLTIME requires no InstallTimeGrant tag)
        options.matches(perm(tags = listOf(ManifestDoc, InstallTimeGrant))) shouldBe false
    }
}
