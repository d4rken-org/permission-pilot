package eu.darken.myperm.permissions.core.features

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.toPackageUri
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.AExtraPerm
import eu.darken.myperm.permissions.core.known.APerm

sealed class PermissionAction {

    abstract val permission: Permission
    abstract val pkg: Pkg?

    abstract fun execute(activity: Activity)

    data class Runtime(override val permission: Permission, override val pkg: Pkg?) : PermissionAction() {
        override fun execute(activity: Activity) {
            val resolvedIntent = if (pkg == null) {
                Intent().apply {
                    component = ComponentName(
                        "com.google.android.permissioncontroller",
                        "com.android.permissioncontroller.permission.ui.ManagePermissionsActivity"
                    )
                }
            } else {
                val intent = when (permission.id) {
                    else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = pkg.packageName.toPackageUri()
                    }
                }

                intent.resolveToActivity(activity)?.apply {
                    data = pkg.packageName.toPackageUri()
                } ?: throw IllegalArgumentException("Unavailable for ${permission.id.value}")
            }

            activity.startActivity(resolvedIntent)
        }
    }

    data class SpecialAccess(override val permission: Permission, override val pkg: Pkg?) : PermissionAction() {
        override fun execute(activity: Activity) {
            launchSettings(activity, permission.id, pkg)
        }
    }

    data class None(override val permission: Permission, override val pkg: Pkg?) : PermissionAction() {
        override fun execute(activity: Activity) {
            Toast.makeText(activity, R.string.permissions_action_none_msg, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {

        val launchableSpecialIds: Set<Permission.Id> = setOf(
            APerm.SYSTEM_ALERT_WINDOW.id,
            APerm.PACKAGE_USAGE_STATS.id,
            APerm.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND.id,
            APerm.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.id,
            APerm.MANAGE_EXTERNAL_STORAGE.id,
            APerm.WRITE_SETTINGS.id,
            APerm.MANAGE_MEDIA.id,
            APerm.SCHEDULE_EXACT_ALARM.id,
            APerm.SMS_FINANCIAL_TRANSACTIONS.id,
            APerm.USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER.id,
            APerm.ACCESS_NOTIFICATION_POLICY.id,
            APerm.ACCESS_NOTIFICATIONS.id,
            AExtraPerm.PICTURE_IN_PICTURE.id,
            APerm.BIND_ACCESSIBILITY_SERVICE.id,
            APerm.BIND_DEVICE_ADMIN.id,
            APerm.REQUEST_INSTALL_PACKAGES.id,
        )

        fun canLaunchSettings(permId: Permission.Id, apiLevel: Int = Build.VERSION.SDK_INT): Boolean =
            permId in launchableSpecialIds && buildSpecialIntents(permId, pkg = null, apiLevel).isNotEmpty()

        fun launchSettings(activity: Activity, permId: Permission.Id, pkg: Pkg? = null) {
            val candidates = buildSpecialIntents(permId, pkg, Build.VERSION.SDK_INT)
            val resolved = candidates.firstNotNullOfOrNull { it.resolveToActivity(activity) }?.apply {
                pkg?.let { data = it.packageName.toPackageUri() }
            } ?: pkg?.let {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .resolveToActivity(activity)
                    ?.apply { data = it.packageName.toPackageUri() }
            } ?: throw IllegalStateException("No launcher available for ${permId.value}")
            activity.startActivity(resolved)
        }

        internal fun buildSpecialIntents(
            permId: Permission.Id,
            pkg: Pkg?,
            apiLevel: Int,
        ): List<Intent> = when (permId) {
            APerm.SYSTEM_ALERT_WINDOW.id -> listOf(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    pkg?.let { data = it.packageName.toPackageUri() }
                }
            )
            APerm.PACKAGE_USAGE_STATS.id -> listOf(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    pkg?.let { data = it.packageName.toPackageUri() }
                }
            )
            APerm.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND.id -> if (apiLevel >= Build.VERSION_CODES.N) {
                listOf(
                    Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS).apply {
                        pkg?.let { data = it.packageName.toPackageUri() }
                    }
                )
            } else emptyList()
            APerm.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.id -> listOf(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    pkg?.let { data = it.packageName.toPackageUri() }
                }
            )
            APerm.MANAGE_EXTERNAL_STORAGE.id -> if (apiLevel >= Build.VERSION_CODES.R) {
                if (pkg != null) {
                    listOf(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = pkg.packageName.toPackageUri()
                        }
                    )
                } else {
                    listOf(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else emptyList()
            APerm.WRITE_SETTINGS.id -> listOf(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    pkg?.let { data = it.packageName.toPackageUri() }
                }
            )
            APerm.MANAGE_MEDIA.id -> if (apiLevel >= Build.VERSION_CODES.S) {
                listOf(
                    Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA).apply {
                        pkg?.let { data = it.packageName.toPackageUri() }
                    }
                )
            } else emptyList()
            APerm.SCHEDULE_EXACT_ALARM.id -> if (apiLevel >= Build.VERSION_CODES.S) {
                listOf(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        pkg?.let { data = it.packageName.toPackageUri() }
                    }
                )
            } else emptyList()
            APerm.SMS_FINANCIAL_TRANSACTIONS.id -> listOf(
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$PremiumSmsAccessActivity"
                    )
                }
            )
            APerm.USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER.id -> listOf(
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$IccLockSettingsActivity"
                    )
                }
            )
            APerm.ACCESS_NOTIFICATION_POLICY.id -> listOf(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$ZenAccessSettingsActivity"
                    )
                },
            )
            APerm.ACCESS_NOTIFICATIONS.id -> listOf(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$NotificationAccessSettingsActivity"
                    )
                },
            )
            AExtraPerm.PICTURE_IN_PICTURE.id -> listOf(
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$PictureInPictureSettingsActivity"
                    )
                }
            )
            APerm.BIND_ACCESSIBILITY_SERVICE.id -> listOf(
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$AccessibilitySettingsActivity"
                    )
                }
            )
            APerm.BIND_DEVICE_ADMIN.id -> listOf(
                Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.DeviceAdminSettings"
                    )
                }
            )
            APerm.REQUEST_INSTALL_PACKAGES.id -> if (apiLevel >= Build.VERSION_CODES.O) {
                listOf(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        pkg?.let { data = it.packageName.toPackageUri() }
                    }
                )
            } else emptyList()
            else -> emptyList()
        }
    }
}

private fun Intent.resolveToActivity(context: Context): Intent? =
    context.packageManager.resolveActivity(this, 0)?.let { result ->
        Intent(this.action).apply {
            setClassName(result.activityInfo.packageName, result.activityInfo.name)
        }
    }
