package eu.darken.myperm.export.core.formatter

import eu.darken.myperm.apps.core.AppInfo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.PermissionUse
import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.room.entity.PkgType
import eu.darken.myperm.export.core.AppExportConfig
import eu.darken.myperm.export.core.AppExportConfig.PermissionDetailLevel
import eu.darken.myperm.export.core.PermissionExportConfig
import eu.darken.myperm.export.core.PermissionType
import eu.darken.myperm.export.core.ResolvedAppRef
import eu.darken.myperm.export.core.ResolvedPermissionInfo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import java.time.Instant

class MarkdownFormatterTest : BaseTest() {

    private val formatter = MarkdownFormatter()

    private fun testApp(
        pkgName: String = "com.example.app",
        label: String = "Test App",
        permissions: List<PermissionUse> = emptyList(),
    ) = AppInfo(
        pkgName = Pkg.Name(pkgName),
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
        installedAt = Instant.parse("2025-01-15T00:00:00Z"),
        updatedAt = Instant.parse("2025-06-01T00:00:00Z"),
        requestedPermissions = permissions,
        declaredPermissionCount = 0,
        pkgType = PkgType.PRIMARY,
        twinCount = 0,
        siblingCount = 0,
        hasAccessibilityServices = false,
        hasDeviceAdmin = false,
        allInstallerPkgNames = emptyList(),
    )

    private fun testPermissionInfo(
        id: String = "android.permission.CAMERA",
        label: String? = "Camera",
    ) = ResolvedPermissionInfo(
        id = id,
        label = label,
        description = "Take photos",
        type = PermissionType.RUNTIME,
        protectionType = "DANGEROUS",
        requestingAppCount = 5,
        grantedAppCount = 3,
        requestingApps = listOf(
            ResolvedAppRef("com.example.a", "App A", true),
            ResolvedAppRef("com.example.b", "App B", false),
        ),
    )

    @Test
    fun `app export includes header`() {
        val result = formatter.formatApps(
            listOf(testApp()),
            AppExportConfig(includeMetaInfo = false, permissionDetailLevel = PermissionDetailLevel.NONE),
        ) { null }

        result shouldContain "# App Info Export"
        result shouldContain "## Test App (com.example.app)"
    }

    @Test
    fun `app export includes meta info when enabled`() {
        val result = formatter.formatApps(
            listOf(testApp()),
            AppExportConfig(includeMetaInfo = true, permissionDetailLevel = PermissionDetailLevel.NONE),
        ) { null }

        result shouldContain "| Target SDK | 34 |"
        result shouldContain "| Min SDK | 24 |"
        result shouldContain "| Version | 1.0.0 (1) |"
    }

    @Test
    fun `app export excludes meta info when disabled`() {
        val result = formatter.formatApps(
            listOf(testApp()),
            AppExportConfig(includeMetaInfo = false, permissionDetailLevel = PermissionDetailLevel.NONE),
        ) { null }

        result shouldNotContain "Target SDK"
        result shouldNotContain "Version"
    }

    @Test
    fun `app export includes permissions at basic level`() {
        val app = testApp(
            permissions = listOf(
                PermissionUse("android.permission.CAMERA", UsesPermission.Status.GRANTED),
                PermissionUse("android.permission.RECORD_AUDIO", UsesPermission.Status.DENIED),
            ),
        )
        val result = formatter.formatApps(
            listOf(app),
            AppExportConfig(includeMetaInfo = false, permissionDetailLevel = PermissionDetailLevel.NAME_AND_STATUS),
        ) { id -> if (id == "android.permission.CAMERA") testPermissionInfo() else null }

        result shouldContain "### Permissions (2 total)"
        result shouldContain "| Camera | Granted |"
        result shouldContain "| android.permission.RECORD_AUDIO | Denied |"
    }

    @Test
    fun `app export sorts apps alphabetically`() {
        val apps = listOf(
            testApp(pkgName = "com.z.app", label = "Zebra"),
            testApp(pkgName = "com.a.app", label = "Alpha"),
        )
        val result = formatter.formatApps(
            apps,
            AppExportConfig(includeMetaInfo = false, permissionDetailLevel = PermissionDetailLevel.NONE),
        ) { null }

        val alphaIndex = result.indexOf("Alpha")
        val zebraIndex = result.indexOf("Zebra")
        (alphaIndex < zebraIndex) shouldBe true
    }

    @Test
    fun `markdown escapes pipe characters`() {
        val app = testApp(label = "App | With Pipe")
        val result = formatter.formatApps(
            listOf(app),
            AppExportConfig(includeMetaInfo = false, permissionDetailLevel = PermissionDetailLevel.NONE),
        ) { null }

        result shouldContain "App \\| With Pipe"
    }

    @Test
    fun `permission export includes summary counts`() {
        val result = formatter.formatPermissions(
            listOf(testPermissionInfo()),
            PermissionExportConfig(includeSummaryCounts = true, includeRequestingApps = false),
        )

        result shouldContain "**3 granted** of 5 requesting apps"
    }

    @Test
    fun `permission export filters to granted only`() {
        val result = formatter.formatPermissions(
            listOf(testPermissionInfo()),
            PermissionExportConfig(includeRequestingApps = true, grantedOnly = true),
        )

        result shouldContain "App A"
        result shouldNotContain "App B"
    }

    @Test
    fun `permission export shows all apps when not filtered`() {
        val result = formatter.formatPermissions(
            listOf(testPermissionInfo()),
            PermissionExportConfig(includeRequestingApps = true, grantedOnly = false),
        )

        result shouldContain "App A"
        result shouldContain "App B"
    }
}
