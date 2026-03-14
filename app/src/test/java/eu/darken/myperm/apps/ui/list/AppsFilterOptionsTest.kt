package eu.darken.myperm.apps.ui.list

import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.common.room.entity.PkgType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class AppsFilterOptionsTest : BaseTest() {

    private fun app(
        isSystemApp: Boolean = false,
        allInstallerPkgNames: List<String> = emptyList(),
        internetAccess: String = "DIRECT",
        siblingCount: Int = 0,
        twinCount: Int = 0,
        pkgType: String = PkgType.PRIMARY.name,
        batteryOptimization: String = "MANAGED_BY_SYSTEM",
        hasAccessibilityServices: Boolean = false,
    ) = AppInfo(
        pkgName = "com.example.app",
        userHandleId = 0,
        label = "Test App",
        versionName = "1.0",
        versionCode = 1,
        isSystemApp = isSystemApp,
        installerPkgName = allInstallerPkgNames.firstOrNull(),
        apiTargetLevel = null,
        apiCompileLevel = null,
        apiMinimumLevel = null,
        internetAccess = internetAccess,
        batteryOptimization = batteryOptimization,
        installedAt = null,
        updatedAt = null,
        requestedPermissions = emptyList(),
        declaredPermissionCount = 0,
        pkgType = pkgType,
        twinCount = twinCount,
        siblingCount = siblingCount,
        hasAccessibilityServices = hasAccessibilityServices,
        allInstallerPkgNames = allInstallerPkgNames,
    )

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
        options.matches(app(pkgType = PkgType.PRIMARY.name)) shouldBe true
        options.matches(app(pkgType = PkgType.SECONDARY_PROFILE.name)) shouldBe true
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
            app(isSystemApp = false, internetAccess = "NONE")
        ) shouldBe true
        // User app with internet — fails PROPERTIES group
        options.matches(
            app(isSystemApp = false, internetAccess = "DIRECT")
        ) shouldBe false
        // System app without internet — fails APP_TYPE group
        options.matches(
            app(isSystemApp = true, internetAccess = "NONE")
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
            app(pkgType = PkgType.PRIMARY.name, twinCount = 2)
        ) shouldBe true
        // Primary profile without twins — fails PROPERTIES group
        options.matches(
            app(pkgType = PkgType.PRIMARY.name, twinCount = 0)
        ) shouldBe false
        // Secondary profile with twins — fails PROFILE group
        options.matches(
            app(pkgType = PkgType.SECONDARY_PROFILE.name, twinCount = 2)
        ) shouldBe false
    }
}
