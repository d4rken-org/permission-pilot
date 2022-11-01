package eu.darken.myperm.apps.core.features

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.permissions.core.known.AExtraPerm

suspend fun PackageInfo.determineSpecialPermissions(ipcFunnel: IPCFunnel): Collection<UsesPermission> {
    val permissions = mutableSetOf<UsesPermission>()

    val withActivities = try {
        ipcFunnel.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    if (withActivities?.activities?.any { it.flags and 0x400000 != 0 } == true) {
        permissions.add(UsesPermission.Unknown(AExtraPerm.PICTURE_IN_PICTURE.id))
    }

    return permissions
}