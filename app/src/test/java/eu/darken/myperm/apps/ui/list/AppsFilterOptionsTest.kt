package eu.darken.myperm.apps.ui.list

import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.PermissionUse
import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.room.entity.PkgType
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import testhelper.json.toComparableJson

class AppsFilterOptionsTest : BaseTest() {

    private val json = Json { encodeDefaults = true }

    // --- Serialization tests ---

    @Test
    fun `default AppsFilterOptions produces expected JSON`() {
        val serialized = json.encodeToString(AppsFilterOptions())
        serialized.toComparableJson() shouldBe """
            {
                "filters": [
                    "USER_APP"
                ]
            }
        """.toComparableJson()
    }

    @Test
    fun `all Filter enum values serialize to their SerialName`() {
        AppsFilterOptions.Filter.entries.forEach { filter ->
            val opts = AppsFilterOptions(filters = setOf(filter))
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
        AppsSortOptions.Sort.entries.forEach { sort ->
            val opts = AppsSortOptions(mainSort = sort)
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
    fun `AppsFilterOptions with all filters produces expected JSON`() {
        val allFilters = AppsFilterOptions(filters = AppsFilterOptions.Filter.entries.toSet())
        val serialized = json.encodeToString(allFilters)
        serialized.toComparableJson() shouldBe """
            {
                "filters": [
                    "SYSTEM_APP",
                    "USER_APP",
                    "GOOGLE_PLAY",
                    "OEM_STORE",
                    "SIDELOADED",
                    "NO_INTERNET",
                    "SHARED_ID",
                    "MULTI_PROFILE",
                    "BATTERY_OPTIMIZATION",
                    "ACCESSIBILITY",
                    "DEVICE_ADMIN",
                    "INSTALL_PACKAGES",
                    "OVERLAY",
                    "PRIMARY_PROFILE",
                    "SECONDARY_PROFILE",
                    "CAMERA",
                    "LOCATION",
                    "MICROPHONE",
                    "CONTACTS",
                    "OLD_API_TARGET"
                ]
            }
        """.toComparableJson()
    }

    @Test
    fun `AppsFilterOptions round-trips through JSON`() {
        val original = AppsFilterOptions(
            filters = setOf(AppsFilterOptions.Filter.SYSTEM_APP, AppsFilterOptions.Filter.NO_INTERNET)
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<AppsFilterOptions>(serialized)
        deserialized shouldBe original
    }

    // --- Filter logic tests ---

    private fun app(
        isSystemApp: Boolean = false,
        allInstallerPkgNames: List<String> = emptyList(),
        internetAccess: InternetAccess = InternetAccess.DIRECT,
        siblingCount: Int = 0,
        twinCount: Int = 0,
        pkgType: PkgType = PkgType.PRIMARY,
        batteryOptimization: BatteryOptimization = BatteryOptimization.MANAGED_BY_SYSTEM,
        hasAccessibilityServices: Boolean = false,
        apiTargetLevel: Int? = null,
        requestedPermissions: List<PermissionUse> = emptyList(),
    ) = AppInfo(
        pkgName = "com.example.app",
        userHandleId = 0,
        label = "Test App",
        versionName = "1.0",
        versionCode = 1,
        isSystemApp = isSystemApp,
        installerPkgName = allInstallerPkgNames.firstOrNull(),
        apiTargetLevel = apiTargetLevel,
        apiCompileLevel = null,
        apiMinimumLevel = null,
        internetAccess = internetAccess,
        batteryOptimization = batteryOptimization,
        installedAt = null,
        updatedAt = null,
        requestedPermissions = requestedPermissions,
        declaredPermissionCount = 0,
        pkgType = pkgType,
        twinCount = twinCount,
        siblingCount = siblingCount,
        hasAccessibilityServices = hasAccessibilityServices,
        hasDeviceAdmin = false,
        allInstallerPkgNames = allInstallerPkgNames,
    )

    private fun grantedPerm(id: String) = PermissionUse(id, UsesPermission.Status.GRANTED)
    private fun deniedPerm(id: String) = PermissionUse(id, UsesPermission.Status.DENIED)

    @Test
    fun `empty filters match all apps`() {
        val options = AppsFilterOptions(filters = emptySet())
        options.matches(app()) shouldBe true
        options.matches(app(isSystemApp = true)) shouldBe true
    }

    @Test
    fun `SYSTEM_APP and USER_APP are OR-ed within APP_TYPE group`() {
        val options = AppsFilterOptions(
            filters = setOf(AppsFilterOptions.Filter.SYSTEM_APP, AppsFilterOptions.Filter.USER_APP)
        )
        options.matches(app(isSystemApp = true)) shouldBe true
        options.matches(app(isSystemApp = false)) shouldBe true
    }

    @Test
    fun `PRIMARY and SECONDARY are OR-ed within PROFILE group`() {
        val options = AppsFilterOptions(
            filters = setOf(
                AppsFilterOptions.Filter.PRIMARY_PROFILE,
                AppsFilterOptions.Filter.SECONDARY_PROFILE,
            )
        )
        options.matches(app(pkgType = PkgType.PRIMARY)) shouldBe true
        options.matches(app(pkgType = PkgType.SECONDARY_PROFILE)) shouldBe true
    }

    @Test
    fun `USER_APP and NO_INTERNET are AND-ed across APP_TYPE and PROPERTIES groups`() {
        val options = AppsFilterOptions(
            filters = setOf(
                AppsFilterOptions.Filter.USER_APP,
                AppsFilterOptions.Filter.NO_INTERNET,
            )
        )
        // User app without internet — matches both groups
        options.matches(
            app(isSystemApp = false, internetAccess = InternetAccess.NONE)
        ) shouldBe true
        // User app with internet — fails PROPERTIES group
        options.matches(
            app(isSystemApp = false, internetAccess = InternetAccess.DIRECT)
        ) shouldBe false
        // System app without internet — fails APP_TYPE group
        options.matches(
            app(isSystemApp = true, internetAccess = InternetAccess.NONE)
        ) shouldBe false
    }

    @Test
    fun `MULTI_PROFILE and PRIMARY_PROFILE are AND-ed across PROPERTIES and PROFILE groups`() {
        val options = AppsFilterOptions(
            filters = setOf(
                AppsFilterOptions.Filter.MULTI_PROFILE,
                AppsFilterOptions.Filter.PRIMARY_PROFILE,
            )
        )
        // Primary profile with twins — matches both groups
        options.matches(
            app(pkgType = PkgType.PRIMARY, twinCount = 2)
        ) shouldBe true
        // Primary profile without twins — fails PROPERTIES group
        options.matches(
            app(pkgType = PkgType.PRIMARY, twinCount = 0)
        ) shouldBe false
        // Secondary profile with twins — fails PROFILE group
        options.matches(
            app(pkgType = PkgType.SECONDARY_PROFILE, twinCount = 2)
        ) shouldBe false
    }

    @Test
    fun `SECONDARY_PROFILE matches both SECONDARY_PROFILE and SECONDARY_USER pkg types`() {
        val options = AppsFilterOptions(filters = setOf(AppsFilterOptions.Filter.SECONDARY_PROFILE))
        options.matches(app(pkgType = PkgType.SECONDARY_PROFILE)) shouldBe true
        options.matches(app(pkgType = PkgType.SECONDARY_USER)) shouldBe true
        options.matches(app(pkgType = PkgType.PRIMARY)) shouldBe false
    }

    @Test
    fun `CAMERA filter matches apps with granted camera permission`() {
        val options = AppsFilterOptions(filters = setOf(AppsFilterOptions.Filter.CAMERA))
        options.matches(
            app(requestedPermissions = listOf(grantedPerm("android.permission.CAMERA")))
        ) shouldBe true
        options.matches(
            app(requestedPermissions = listOf(deniedPerm("android.permission.CAMERA")))
        ) shouldBe false
        options.matches(app()) shouldBe false
    }

    @Test
    fun `LOCATION filter matches fine or coarse location`() {
        val options = AppsFilterOptions(filters = setOf(AppsFilterOptions.Filter.LOCATION))
        options.matches(
            app(requestedPermissions = listOf(grantedPerm("android.permission.ACCESS_FINE_LOCATION")))
        ) shouldBe true
        options.matches(
            app(requestedPermissions = listOf(grantedPerm("android.permission.ACCESS_COARSE_LOCATION")))
        ) shouldBe true
        options.matches(
            app(requestedPermissions = listOf(deniedPerm("android.permission.ACCESS_FINE_LOCATION")))
        ) shouldBe false
    }

    @Test
    fun `MICROPHONE filter matches apps with granted record audio`() {
        val options = AppsFilterOptions(filters = setOf(AppsFilterOptions.Filter.MICROPHONE))
        options.matches(
            app(requestedPermissions = listOf(grantedPerm("android.permission.RECORD_AUDIO")))
        ) shouldBe true
        options.matches(app()) shouldBe false
    }

    @Test
    fun `CONTACTS filter matches apps with granted read contacts`() {
        val options = AppsFilterOptions(filters = setOf(AppsFilterOptions.Filter.CONTACTS))
        options.matches(
            app(requestedPermissions = listOf(grantedPerm("android.permission.READ_CONTACTS")))
        ) shouldBe true
        options.matches(app()) shouldBe false
    }

    @Test
    fun `OLD_API_TARGET filter boundary - null, 28, 29`() {
        val options = AppsFilterOptions(filters = setOf(AppsFilterOptions.Filter.OLD_API_TARGET))
        // null apiTargetLevel — does not match
        options.matches(app(apiTargetLevel = null)) shouldBe false
        // API 28 — matches (below threshold)
        options.matches(app(apiTargetLevel = 28)) shouldBe true
        // API 29 — does not match (at threshold)
        options.matches(app(apiTargetLevel = 29)) shouldBe false
        // API 30 — does not match (above threshold)
        options.matches(app(apiTargetLevel = 30)) shouldBe false
    }
}
