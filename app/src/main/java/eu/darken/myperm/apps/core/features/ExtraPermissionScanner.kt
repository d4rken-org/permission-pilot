package eu.darken.myperm.apps.core.features

import android.app.AppOpsManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.hasApiLevel
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.permissions.core.known.AExtraPerm
import eu.darken.myperm.permissions.core.known.APerm

private val TAG = logTag("ExtraPermissionScanner")

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

private data class SpecialPermissionDef(
    val permission: APerm,
    val appOp: String,
    val minApiLevel: Int
)

private val SPECIAL_PERMISSIONS = listOf(
    // API 1 (always available, minSdk >= 21)
    SpecialPermissionDef(APerm.PACKAGE_USAGE_STATS, AppOpsManager.OPSTR_GET_USAGE_STATS, 1),
    SpecialPermissionDef(APerm.ACCESS_NOTIFICATIONS, "android:access_notifications", 1),
    // API 23 (M)
    SpecialPermissionDef(APerm.SYSTEM_ALERT_WINDOW, AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, Build.VERSION_CODES.M),
    SpecialPermissionDef(APerm.WRITE_SETTINGS, AppOpsManager.OPSTR_WRITE_SETTINGS, Build.VERSION_CODES.M),
    // API 24 (N)
    SpecialPermissionDef(APerm.CHANGE_WIFI_STATE, "android:change_wifi_state", Build.VERSION_CODES.N),
    // API 26 (O)
    SpecialPermissionDef(APerm.REQUEST_INSTALL_PACKAGES, "android:request_install_packages", Build.VERSION_CODES.O),
    SpecialPermissionDef(APerm.TURN_SCREEN_ON, "android:turn_screen_on", Build.VERSION_CODES.O),
    // API 30 (R)
    SpecialPermissionDef(APerm.MANAGE_EXTERNAL_STORAGE, "android:manage_external_storage", Build.VERSION_CODES.R),
    SpecialPermissionDef(APerm.INTERACT_ACROSS_PROFILES, "android:interact_across_profiles", Build.VERSION_CODES.R),
    SpecialPermissionDef(APerm.LOADER_USAGE_STATS, "android:loader_usage_stats", Build.VERSION_CODES.R),
    // API 31 (S)
    SpecialPermissionDef(APerm.SCHEDULE_EXACT_ALARM, "android:schedule_exact_alarm", Build.VERSION_CODES.S),
    SpecialPermissionDef(APerm.MANAGE_MEDIA, "android:manage_media", Build.VERSION_CODES.S),
    SpecialPermissionDef(APerm.USE_FULL_SCREEN_INTENT, "android:use_full_screen_intent", Build.VERSION_CODES.S),
)

/**
 * Checks special permission states via AppOpsManager for permissions that are not
 * reflected in requestedPermissionsFlags.
 *
 * @param uidOverride Optional UID to use instead of applicationInfo.uid (useful for secondary profiles)
 * @return map of Permission.Id to UsesPermission.Status for special permissions that the app has requested.
 */
suspend fun PackageInfo.getSpecialPermissionStatuses(
    ipcFunnel: IPCFunnel,
    uidOverride: Int? = null
): Map<Permission.Id, UsesPermission.Status> {
    val uid = uidOverride ?: applicationInfo?.uid ?: return emptyMap()
    val requestedPerms = requestedPermissions?.toSet() ?: return emptyMap()

    val statuses = mutableMapOf<Permission.Id, UsesPermission.Status>()

    for (def in SPECIAL_PERMISSIONS) {
        if (!hasApiLevel(def.minApiLevel)) continue
        if (!requestedPerms.contains(def.permission.id.value)) continue

        try {
            val result = ipcFunnel.appOpsManager.checkOpNoThrow(def.appOp, uid, packageName)
            statuses[def.permission.id] = mapAppOpResult(result)
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to check ${def.permission.id} for $packageName: ${e.asLog()}" }
        }
    }

    return statuses
}

private fun mapAppOpResult(result: Int): UsesPermission.Status = when (result) {
    AppOpsManager.MODE_ALLOWED -> UsesPermission.Status.GRANTED
    AppOpsManager.MODE_FOREGROUND -> UsesPermission.Status.GRANTED_IN_USE
    else -> UsesPermission.Status.DENIED
}
