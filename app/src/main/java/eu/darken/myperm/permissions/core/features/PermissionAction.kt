package eu.darken.myperm.permissions.core.features

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.APerm

sealed class PermissionAction {

    abstract val permission: Permission
    abstract val pkg: Pkg

    abstract fun execute(activity: Activity)

    data class Runtime(override val permission: Permission, override val pkg: Pkg) : PermissionAction() {
        override fun execute(activity: Activity) {
            val intent = when (permission.id) {
                else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${pkg.packageName}")
                }
            }

            val resolvedIntent = intent.resolveToActivity(activity)?.apply {
                data = Uri.parse("package:${pkg.packageName}")
            } ?: throw IllegalArgumentException("Unavailable for ${permission.id.value}")

            activity.startActivity(resolvedIntent)
        }
    }

    data class SpecialAccess(override val permission: Permission, override val pkg: Pkg) : PermissionAction() {
        override fun execute(activity: Activity) {
            val intent = when (permission.id) {
                APerm.SYSTEM_ALERT_WINDOW.id -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${pkg.packageName}")
                }
                APerm.PACKAGE_USAGE_STATS.id -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = Uri.parse("package:${pkg.packageName}")
                }
                APerm.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND.id -> Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS).apply {
                    data = Uri.parse("package:${pkg.packageName}")
                }
                APerm.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.id -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${pkg.packageName}")
                }
                APerm.MANAGE_EXTERNAL_STORAGE.id -> Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${pkg.packageName}")
                }
                APerm.WRITE_SETTINGS.id -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${pkg.packageName}")
                }
                APerm.MANAGE_MEDIA.id -> Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA).apply {
                    data = Uri.parse("package:${pkg.packageName}")
                }
                APerm.SCHEDULE_EXACT_ALARM.id -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${pkg.packageName}")
                }
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
                else -> throw IllegalArgumentException("No action found for: ${permission.id.value}")
            }

            val resolvedIntent = intent.resolveToActivity(activity)?.apply {
                data = Uri.parse("package:${pkg.packageName}")
            } ?: throw IllegalArgumentException("Action unavailable for ${permission.id.value}")

            activity.startActivity(resolvedIntent)
        }
    }

    data class None(override val permission: Permission, override val pkg: Pkg) : PermissionAction() {
        override fun execute(context: Activity) {
            Toast.makeText(context, R.string.permissions_action_none_msg, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun Intent.resolveToActivity(context: Context): Intent? =
    context.packageManager.resolveActivity(this, 0)?.let { result ->
        Intent(this.action).apply {
            setClassName(result.activityInfo.packageName, result.activityInfo.name)
        }
    }