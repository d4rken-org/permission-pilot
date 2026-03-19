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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class JsonFormatterTest : BaseTest() {

    private val formatter = JsonFormatter()

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
    fun `json app export is valid json`() {
        val result = formatter.formatApps(
            listOf(testApp()),
            AppExportConfig(includeMetaInfo = true, permissionDetailLevel = PermissionDetailLevel.NONE),
        ) { null }

        val json = Json.parseToJsonElement(result).jsonObject
        json.containsKey("exportedAt") shouldBe true
        val apps = json["apps"]!!.jsonArray
        apps.size shouldBe 1
    }

    @Test
    fun `json app export includes nested permissions`() {
        val app = testApp(
            permissions = listOf(
                PermissionUse("android.permission.CAMERA", UsesPermission.Status.GRANTED),
            ),
        )
        val result = formatter.formatApps(
            listOf(app),
            AppExportConfig(includeMetaInfo = false, permissionDetailLevel = PermissionDetailLevel.NAME_AND_STATUS),
        ) { null }

        val json = Json.parseToJsonElement(result).jsonObject
        val firstApp = json["apps"]!!.jsonArray[0].jsonObject
        val perms = firstApp["permissions"]!!.jsonArray
        perms[0].jsonObject["id"]!!.jsonPrimitive.content shouldContain "CAMERA"
        perms[0].jsonObject["status"]!!.jsonPrimitive.content shouldContain "granted"
    }

    @Test
    fun `json app export excludes permissions when level is NONE`() {
        val result = formatter.formatApps(
            listOf(testApp()),
            AppExportConfig(includeMetaInfo = false, permissionDetailLevel = PermissionDetailLevel.NONE),
        ) { null }

        result shouldNotContain "\"permissions\""
    }

    @Test
    fun `json app export includes meta when enabled`() {
        val result = formatter.formatApps(
            listOf(testApp()),
            AppExportConfig(includeMetaInfo = true, permissionDetailLevel = PermissionDetailLevel.NONE),
        ) { null }

        result shouldContain "\"targetSdk\""
        result shouldContain "\"versionName\""
    }

    @Test
    fun `json permission export includes requesting apps`() {
        val perm = ResolvedPermissionInfo(
            id = "android.permission.CAMERA",
            label = "Camera",
            description = "Take photos",
            type = PermissionType.RUNTIME,
            protectionType = "DANGEROUS",
            requestingAppCount = 2,
            grantedAppCount = 1,
            requestingApps = listOf(
                ResolvedAppRef("com.a", "App A", true),
                ResolvedAppRef("com.b", "App B", false),
            ),
        )

        val result = formatter.formatPermissions(
            listOf(perm),
            PermissionExportConfig(includeRequestingApps = true, grantedOnly = false),
        )

        val json = Json.parseToJsonElement(result).jsonObject
        val firstPerm = json["permissions"]!!.jsonArray[0].jsonObject
        val apps = firstPerm["apps"]!!.jsonArray
        apps.size shouldBe 2
    }

    @Test
    fun `json permission export filters to granted only`() {
        val perm = ResolvedPermissionInfo(
            id = "android.permission.CAMERA",
            label = "Camera",
            description = null,
            type = PermissionType.RUNTIME,
            protectionType = null,
            requestingAppCount = 2,
            grantedAppCount = 1,
            requestingApps = listOf(
                ResolvedAppRef("com.a", "App A", true),
                ResolvedAppRef("com.b", "App B", false),
            ),
        )

        val result = formatter.formatPermissions(
            listOf(perm),
            PermissionExportConfig(includeRequestingApps = true, grantedOnly = true),
        )

        result shouldContain "App A"
        result shouldNotContain "App B"
    }
}
