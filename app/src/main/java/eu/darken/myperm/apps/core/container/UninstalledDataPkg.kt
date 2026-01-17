package eu.darken.myperm.apps.core.container

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.Installed
import eu.darken.myperm.apps.core.features.InstallerInfo
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.features.UninstalledPkg
import eu.darken.myperm.apps.core.features.UsesPermission
import eu.darken.myperm.apps.core.getIcon2
import eu.darken.myperm.apps.core.getLabel2
import eu.darken.myperm.apps.core.isSystemApp
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.APerm

class UninstalledDataPkg(
    override val packageInfo: PackageInfo,
    override val installerInfo: InstallerInfo,
    override val userHandle: UserHandle,
    val extraPermissions: Collection<UsesPermission>,
) : BasePkg(), UninstalledPkg {

    override val id: Pkg.Id = Pkg.Id(packageInfo.packageName, userHandle)

    private var _label: String? = null
    override fun getLabel(context: Context): String {
        _label?.let { return it }
        val newLabel = context.packageManager.getLabel2(id)
            ?: twins.firstNotNullOfOrNull { it.getLabel(context) }
            ?: super.getLabel(context)
            ?: id.pkgName
        _label = newLabel
        return newLabel
    }

    override fun getIcon(context: Context): Drawable =
        context.packageManager.getIcon2(id)
            ?: twins.firstNotNullOfOrNull { it.getIcon(context) }
            ?: super.getIcon(context)
            ?: context.getDrawable(R.drawable.ic_default_app_icon_24)!!

    override var siblings: Collection<Pkg> = emptyList()
    override var twins: Collection<Installed> = emptyList()

    override val requestedPermissions: Collection<UsesPermission> by lazy {
        val base = packageInfo.requestedPermissions?.mapIndexed { _, permissionId ->
            UsesPermission.Unknown(id = Permission.Id(permissionId))
        } ?: emptyList()
        val acsPermissions = accessibilityServices.map {
            UsesPermission.WithState(
                id = APerm.BIND_ACCESSIBILITY_SERVICE.id,
                flags = null
            )
        }
        base + acsPermissions
    }

    override val declaredPermissions: Collection<PermissionInfo> by lazy {
        packageInfo.permissions?.toSet() ?: emptyList()
    }

    override val internetAccess: InternetAccess = InternetAccess.UNKNOWN

    override val isSystemApp: Boolean = (applicationInfo?.isSystemApp ?: true) || twins.any { it.isSystemApp }

    override fun toString(): String = "UninstalledDataPkg(packageName=$packageName, userHandle=$userHandle)"
}
