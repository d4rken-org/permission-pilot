package eu.darken.myperm.apps.core.features

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import eu.darken.myperm.permissions.core.known.AExtraPerm

fun PackageInfo.determineSpecialPermissions(context: Context): Collection<UsesPermission> {
    val pm = context.packageManager
    val permissions = mutableSetOf<UsesPermission>()

    val withActivities = try {
        pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    if (withActivities?.activities?.any { it.flags and 0x400000 != 0 } == true) {
        permissions.add(UsesPermission.Unknown(AExtraPerm.PICTURE_IN_PICTURE.id))
    }

    return permissions
}