package eu.darken.myperm.export.core.formatter

import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.PermissionUse
import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.room.entity.PkgType
import eu.darken.myperm.export.core.AppExportConfig
import eu.darken.myperm.export.core.AppExportConfig.PermissionDetailLevel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class CsvFormatterTest : BaseTest() {

    private val formatter = CsvFormatter()

    private fun testApp(
        pkgName: String = "com.example.app",
        label: String = "Test App",
        permissions: List<PermissionUse> = emptyList(),
    ) = AppInfo(
        pkgName = pkgName,
        userHandleId = 0,
        label = label,
        versionName = "1.0.0",
        versionCode = 1,
        isSystemApp = false,
        installerPkgName = null,
        apiTargetLevel = 34,
        apiCompileLevel = 34,
        apiMinimumLevel = 24,
        internetAccess = InternetAccess.DIRECT,
        batteryOptimization = BatteryOptimization.OPTIMIZED,
        installedAt = null,
        updatedAt = null,
        requestedPermissions = permissions,
        declaredPermissionCount = 0,
        pkgType = PkgType.PRIMARY,
        twinCount = 0,
        siblingCount = 0,
        hasAccessibilityServices = false,
        hasDeviceAdmin = false,
        allInstallerPkgNames = emptyList(),
    )

    @Test
    fun `csv escapes commas in values`() {
        val result = CsvFormatter.escapeCsv("hello, world")
        result shouldBe "\"hello, world\""
    }

    @Test
    fun `csv escapes quotes by doubling`() {
        val result = CsvFormatter.escapeCsv("say \"hello\"")
        result shouldBe "\"say \"\"hello\"\"\""
    }

    @Test
    fun `csv prevents formula injection`() {
        CsvFormatter.escapeCsv("=SUM(A1)") shouldBe "'=SUM(A1)"
        CsvFormatter.escapeCsv("+cmd") shouldBe "'+cmd"
        CsvFormatter.escapeCsv("-cmd") shouldBe "'-cmd"
        CsvFormatter.escapeCsv("@import") shouldBe "'@import"
    }

    @Test
    fun `csv leaves normal values unquoted`() {
        CsvFormatter.escapeCsv("normal value") shouldBe "normal value"
    }

    @Test
    fun `app export with permissions creates one row per permission`() {
        val app = testApp(
            permissions = listOf(
                PermissionUse("android.permission.CAMERA", UsesPermission.Status.GRANTED),
                PermissionUse("android.permission.MIC", UsesPermission.Status.DENIED),
            ),
        )
        val result = formatter.formatApps(
            listOf(app),
            AppExportConfig(includeMetaInfo = false, permissionDetailLevel = PermissionDetailLevel.NAME_AND_STATUS),
        ) { null }

        val lines = result.trim().lines()
        lines.size shouldBe 3 // header + 2 permission rows
        lines[0] shouldContain "package_name"
        lines[0] shouldContain "permission_id"
    }

    @Test
    fun `app export without permissions creates one row per app`() {
        val result = formatter.formatApps(
            listOf(testApp(), testApp(pkgName = "com.other.app", label = "Other")),
            AppExportConfig(includeMetaInfo = false, permissionDetailLevel = PermissionDetailLevel.NONE),
        ) { null }

        val lines = result.trim().lines()
        lines.size shouldBe 3 // header + 2 apps
        lines[0] shouldNotContain "permission_id"
    }

    @Test
    fun `app export includes meta columns when enabled`() {
        val result = formatter.formatApps(
            listOf(testApp()),
            AppExportConfig(includeMetaInfo = true, permissionDetailLevel = PermissionDetailLevel.NONE),
        ) { null }

        val header = result.trim().lines().first()
        header shouldContain "version_name"
        header shouldContain "target_sdk"
        header shouldContain "is_system_app"
    }
}
