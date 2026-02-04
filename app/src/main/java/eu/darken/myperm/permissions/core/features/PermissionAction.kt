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
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.AExtraPerm
import eu.darken.myperm.permissions.core.known.APerm
import androidx.core.net.toUri

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
                        data = "package:${pkg.packageName}".toUri()
                    }
                }

                intent.resolveToActivity(activity)?.apply {
                    data = "package:${pkg.packageName}".toUri()
                } ?: throw IllegalArgumentException("Unavailable for ${permission.id.value}")
            }

            activity.startActivity(resolvedIntent)
        }
    }

    data class SpecialAccess(override val permission: Permission, override val pkg: Pkg?) : PermissionAction() {
        override fun execute(activity: Activity) {
            val intent = when (permission.id) {
                APerm.SYSTEM_ALERT_WINDOW.id -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        pkg?.let { data = "package:${it.packageName}".toUri() }
                    }
                } else null
                APerm.PACKAGE_USAGE_STATS.id -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    pkg?.let { data = "package:${it.packageName}".toUri() }
                }
                APerm.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND.id -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS).apply {
                        pkg?.let { data = "package:${it.packageName}".toUri() }
                    }
                } else null
                APerm.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.id -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        pkg?.let { data = "package:${it.packageName}".toUri() }
                    }
                } else null
                APerm.MANAGE_EXTERNAL_STORAGE.id -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (pkg != null) {
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = "package:${pkg.packageName}".toUri()
                        }
                    } else {
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    }
                } else null
                APerm.WRITE_SETTINGS.id -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        pkg?.let { data = "package:${it.packageName}".toUri() }
                    }
                } else null
                APerm.MANAGE_MEDIA.id -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA).apply {
                        pkg?.let { data = "package:${it.packageName}".toUri() }
                    }
                } else null
                APerm.SCHEDULE_EXACT_ALARM.id -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        pkg?.let { data = "package:${it.packageName}".toUri() }
                    }
                } else null
                // We don't have specific intents for these
//                APerm.MANAGE_ONGOING_CALLS.id -> Intent(Settings.)
//                APerm.INSTANT_APP_FOREGROUND_SERVICE.id -> Intent(Settings.)
//               APerm.LOADER_USAGE_STATS.id -> Intent().apply {
//
//               }
                APerm.SMS_FINANCIAL_TRANSACTIONS.id -> Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$PremiumSmsAccessActivity"
                    )
                }
                APerm.USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER.id -> Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$IccLockSettingsActivity"
                    )
                }
                APerm.ACCESS_NOTIFICATION_POLICY.id -> Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$ZenAccessSettingsActivity"
                    )
                }
                APerm.ACCESS_NOTIFICATIONS.id -> Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$NotificationAccessSettingsActivity"
                    )
                }
                AExtraPerm.PICTURE_IN_PICTURE.id -> Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$PictureInPictureSettingsActivity"
                    )
                }
                APerm.BIND_ACCESSIBILITY_SERVICE.id -> Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$AccessibilitySettingsActivity"
                    )
                }
                APerm.REQUEST_INSTALL_PACKAGES.id -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        pkg?.let { data = "package:${it.packageName}".toUri() }
                    }
                } else null
                else -> null
            }

            val resolvedIntent = intent?.resolveToActivity(activity)?.apply {
                pkg?.let { data = "package:${it.packageName}".toUri() }
            } ?: pkg?.let {
                // Fallback to App Info
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .resolveToActivity(activity)
                    ?.apply { data = "package:${it.packageName}".toUri() }
            } ?: throw IllegalArgumentException("No action available for ${permission.id.value}")

            activity.startActivity(resolvedIntent)
        }
    }

    data class None(override val permission: Permission, override val pkg: Pkg?) : PermissionAction() {
        override fun execute(activity: Activity) {
            Toast.makeText(activity, R.string.permissions_action_none_msg, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun Intent.resolveToActivity(context: Context): Intent? =
    context.packageManager.resolveActivity(this, 0)?.let { result ->
        Intent(this.action).apply {
            setClassName(result.activityInfo.packageName, result.activityInfo.name)
        }
    }