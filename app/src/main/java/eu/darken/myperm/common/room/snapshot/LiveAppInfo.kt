package eu.darken.myperm.common.room.snapshot

import android.content.Context
import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.container.PrimaryProfilePkg
import eu.darken.myperm.apps.core.container.SecondaryProfilePkg
import eu.darken.myperm.apps.core.container.SecondaryUserPkg
import eu.darken.myperm.apps.core.container.UninstalledDataPkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.common.room.entity.PkgType
import java.time.Instant

class LiveAppInfo(
    private val pkg: BasePkg,
    private val context: Context,
) : DisplayableApp {

    override val pkgName: String get() = pkg.id.pkgName

    override val userHandleId: Int get() = pkg.userHandle.hashCode()

    override val label: String get() = pkg.getLabel(context) ?: pkg.id.pkgName

    override val versionName: String? get() = pkg.versionName

    override val versionCode: Long get() = pkg.versionCode

    override val isSystemApp: Boolean get() = pkg.isSystemApp

    override val installerPkgName: String? get() = pkg.installerInfo.installingPkg?.packageName

    override val apiTargetLevel: Int? get() = pkg.apiTargetLevel

    override val apiCompileLevel: Int? get() = pkg.apiCompileLevel

    override val apiMinimumLevel: Int? get() = pkg.apiMinimumLevel

    override val internetAccess: String get() = pkg.internetAccess.name

    override val batteryOptimization: String get() = pkg.batteryOptimization.name

    override val installedAt: Instant? get() = pkg.installedAt

    override val updatedAt: Instant? get() = pkg.updatedAt

    override val requestedPermissions: List<LivePermissionUse> by lazy {
        pkg.requestedPermissions.map { LivePermissionUse(it) }
    }

    override val declaredPermissionCount: Int get() = pkg.declaredPermissions.size

    override val pkgType: String get() = when (pkg) {
        is PrimaryProfilePkg -> PkgType.PRIMARY.name
        is SecondaryProfilePkg -> PkgType.SECONDARY_PROFILE.name
        is SecondaryUserPkg -> PkgType.SECONDARY_USER.name
        is UninstalledDataPkg -> PkgType.UNINSTALLED.name
    }

    override val twinCount: Int get() = pkg.twins.size

    override val siblingCount: Int get() = pkg.siblings.size

    override val hasAccessibilityServices: Boolean get() = pkg.accessibilityServices.isNotEmpty()

    override val allInstallerPkgNames: List<String> by lazy {
        pkg.installerInfo.allInstallers.map { it.id.pkgName }
    }
}

class LivePermissionUse(private val use: UsesPermission) : DisplayablePermissionUse {
    override val permissionId: String get() = use.id.value
    override val status: UsesPermission.Status get() = use.status
}
