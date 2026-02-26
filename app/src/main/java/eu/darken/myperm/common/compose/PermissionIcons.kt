package eu.darken.myperm.common.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AccessibilityNew
import androidx.compose.material.icons.twotone.Alarm
import androidx.compose.material.icons.twotone.Apps
import androidx.compose.material.icons.twotone.BatteryChargingFull
import androidx.compose.material.icons.twotone.Bluetooth
import androidx.compose.material.icons.twotone.BluetoothSearching
import androidx.compose.material.icons.twotone.Call
import androidx.compose.material.icons.twotone.CalendarToday
import androidx.compose.material.icons.twotone.Contacts
import androidx.compose.material.icons.twotone.DataUsage
import androidx.compose.material.icons.twotone.DirectionsRun
import androidx.compose.material.icons.twotone.DoNotDisturbOn
import androidx.compose.material.icons.twotone.EditCalendar
import androidx.compose.material.icons.twotone.InstallMobile
import androidx.compose.material.icons.twotone.Inventory2
import androidx.compose.material.icons.twotone.Language
import androidx.compose.material.icons.twotone.Layers
import androidx.compose.material.icons.twotone.LocalCafe
import androidx.compose.material.icons.twotone.LocalPhone
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material.icons.twotone.ManageAccounts
import androidx.compose.material.icons.twotone.Mic
import androidx.compose.material.icons.twotone.MonitorHeart
import androidx.compose.material.icons.twotone.MoreHoriz
import androidx.compose.material.icons.twotone.MyLocation
import androidx.compose.material.icons.twotone.Nfc
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.NotificationsActive
import androidx.compose.material.icons.twotone.People
import androidx.compose.material.icons.twotone.PermMedia
import androidx.compose.material.icons.twotone.PhoneAndroid
import androidx.compose.material.icons.twotone.PhotoCamera
import androidx.compose.material.icons.twotone.PhotoLibrary
import androidx.compose.material.icons.twotone.PictureInPicture
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material.icons.twotone.SdCard
import androidx.compose.material.icons.twotone.Sensors
import androidx.compose.material.icons.twotone.SettingsApplications
import androidx.compose.material.icons.twotone.SettingsEthernet
import androidx.compose.material.icons.twotone.ShareLocation
import androidx.compose.material.icons.twotone.Sms
import androidx.compose.material.icons.twotone.Start
import androidx.compose.material.icons.twotone.Sync
import androidx.compose.material.icons.twotone.Vibration
import androidx.compose.material.icons.twotone.Wifi
import androidx.compose.material.icons.twotone.WifiLock
import androidx.compose.material.icons.twotone.Work
import androidx.compose.ui.graphics.vector.ImageVector
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.AExtraPerm
import eu.darken.myperm.permissions.core.known.APerm
import eu.darken.myperm.permissions.core.known.APermGrp

val APermGrp.icon: ImageVector
    get() = when (this) {
        APermGrp.Camera -> Icons.TwoTone.PhotoCamera
        APermGrp.Audio -> Icons.TwoTone.Mic
        APermGrp.Calendar -> Icons.TwoTone.CalendarToday
        APermGrp.Contacts -> Icons.TwoTone.Contacts
        APermGrp.Files -> Icons.TwoTone.SdCard
        APermGrp.Apps -> Icons.TwoTone.Apps
        APermGrp.Location -> Icons.TwoTone.MyLocation
        APermGrp.Calls -> Icons.TwoTone.Call
        APermGrp.Sensors -> Icons.TwoTone.Sensors
        APermGrp.Messaging -> Icons.TwoTone.Sms
        APermGrp.Connectivity -> Icons.TwoTone.SettingsEthernet
        APermGrp.Other -> Icons.TwoTone.MoreHoriz
    }

val APerm.icon: ImageVector?
    get() = when (this) {
        // Files
        APerm.MANAGE_EXTERNAL_STORAGE -> Icons.TwoTone.SdCard
        APerm.MANAGE_MEDIA -> Icons.TwoTone.PermMedia
        APerm.WRITE_MEDIA_STORAGE -> Icons.TwoTone.PermMedia
        APerm.READ_MEDIA_STORAGE -> Icons.TwoTone.SdCard
        APerm.WRITE_EXTERNAL_STORAGE -> Icons.TwoTone.SdCard
        APerm.READ_EXTERNAL_STORAGE -> Icons.TwoTone.SdCard
        APerm.ACCESS_MEDIA_LOCATION -> Icons.TwoTone.PhotoLibrary

        // Contacts
        APerm.READ_CONTACTS -> Icons.TwoTone.Contacts
        APerm.WRITE_CONTACTS -> Icons.TwoTone.Contacts

        // Location
        APerm.ACCESS_FINE_LOCATION -> Icons.TwoTone.MyLocation
        APerm.ACCESS_COARSE_LOCATION -> Icons.TwoTone.LocationOn
        APerm.ACCESS_BACKGROUND_LOCATION -> Icons.TwoTone.ShareLocation

        // Bluetooth
        APerm.BLUETOOTH -> Icons.TwoTone.Bluetooth
        APerm.BLUETOOTH_ADMIN -> Icons.TwoTone.Bluetooth
        APerm.BLUETOOTH_CONNECT -> Icons.TwoTone.Bluetooth
        APerm.BLUETOOTH_SCAN -> Icons.TwoTone.Bluetooth
        APerm.BLUETOOTH_ADVERTISE -> Icons.TwoTone.BluetoothSearching

        // SMS
        APerm.SEND_SMS -> Icons.TwoTone.Sms
        APerm.RECEIVE_SMS -> Icons.TwoTone.Sms
        APerm.READ_SMS -> Icons.TwoTone.Sms
        APerm.RECEIVE_WAP_PUSH -> Icons.TwoTone.Sms
        APerm.RECEIVE_MMS -> Icons.TwoTone.Sms
        APerm.BROADCAST_SMS -> Icons.TwoTone.Sms
        APerm.SMS_FINANCIAL_TRANSACTIONS -> Icons.TwoTone.Sms

        // Calls / Phone (PHONE_CALL and CALL_PHONE are separate objects for the same android permission)
        APerm.PHONE_CALL -> Icons.TwoTone.LocalPhone
        APerm.ANSWER_PHONE_CALLS -> Icons.TwoTone.LocalPhone
        APerm.READ_CALL_LOG -> Icons.TwoTone.Call
        APerm.WRITE_CALL_LOG -> Icons.TwoTone.Call
        APerm.PHONE_STATE -> Icons.TwoTone.LocalPhone
        APerm.READ_PHONE_STATE -> Icons.TwoTone.LocalPhone
        APerm.READ_PHONE_NUMBERS -> Icons.TwoTone.PhoneAndroid
        APerm.CALL_PHONE -> Icons.TwoTone.LocalPhone
        APerm.CALL_PRIVILEGED -> Icons.TwoTone.LocalPhone
        APerm.MANAGE_ONGOING_CALLS -> Icons.TwoTone.LocalPhone
        APerm.MANAGE_OWN_CALLS -> Icons.TwoTone.LocalPhone
        APerm.PROCESS_OUTGOING_CALLS -> Icons.TwoTone.LocalPhone
        APerm.BIND_CALL_REDIRECTION_SERVICE -> Icons.TwoTone.LocalPhone
        APerm.BIND_INCALL_SERVICE -> Icons.TwoTone.LocalPhone
        APerm.ADD_VOICEMAIL -> Icons.TwoTone.LocalPhone
        APerm.BIND_VISUAL_VOICEMAIL_SERVICE -> Icons.TwoTone.LocalPhone
        APerm.BIND_VOICE_INTERACTION -> Icons.TwoTone.LocalPhone
        APerm.READ_VOICEMAIL -> Icons.TwoTone.LocalPhone
        APerm.WRITE_VOICEMAIL -> Icons.TwoTone.LocalPhone

        // Connectivity
        APerm.INTERNET -> Icons.TwoTone.Language
        APerm.CHANGE_WIFI_STATE -> Icons.TwoTone.WifiLock
        APerm.WIFI_STATE -> Icons.TwoTone.Wifi
        APerm.ACCESS_NETWORK_STATE -> Icons.TwoTone.SettingsEthernet
        APerm.CHANGE_NETWORK_STATE -> Icons.TwoTone.SettingsEthernet
        APerm.NFC -> Icons.TwoTone.Nfc

        // Camera
        APerm.CAMERA -> Icons.TwoTone.PhotoCamera
        APerm.CAMERA_OPEN_CLOSE_LISTENER -> Icons.TwoTone.PhotoCamera
        APerm.SYSTEM_CAMERA -> Icons.TwoTone.PhotoCamera
        APerm.BACKGROUND_CAMERA -> Icons.TwoTone.PhotoCamera
        APerm.MANAGE_CAMERA -> Icons.TwoTone.PhotoCamera
        APerm.CAMERA_SEND_SYSTEM_EVENTS -> Icons.TwoTone.PhotoCamera
        APerm.CAMERA_INJECT_EXTERNAL_CAMERA -> Icons.TwoTone.PhotoCamera
        APerm.CAMERA_DISABLE_TRANSMIT_LED -> Icons.TwoTone.PhotoCamera

        // Audio
        APerm.RECORD_AUDIO -> Icons.TwoTone.Mic

        // Sensors
        APerm.BODY_SENSORS -> Icons.TwoTone.MonitorHeart
        APerm.BODY_SENSORS_BACKGROUND -> Icons.TwoTone.MonitorHeart
        APerm.ACTIVITY_RECOGNITION -> Icons.TwoTone.DirectionsRun

        // Calendar
        APerm.READ_CALENDAR -> Icons.TwoTone.CalendarToday
        APerm.WRITE_CALENDAR -> Icons.TwoTone.EditCalendar

        // Special access
        APerm.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> Icons.TwoTone.BatteryChargingFull
        APerm.SYSTEM_ALERT_WINDOW -> Icons.TwoTone.Layers
        APerm.ACCESS_NOTIFICATION_POLICY -> Icons.TwoTone.DoNotDisturbOn
        APerm.WRITE_SETTINGS -> Icons.TwoTone.SettingsApplications
        APerm.ACCESS_NOTIFICATIONS -> Icons.TwoTone.Notifications
        APerm.REQUEST_INSTALL_PACKAGES -> Icons.TwoTone.InstallMobile
        APerm.SCHEDULE_EXACT_ALARM -> Icons.TwoTone.Alarm
        APerm.PACKAGE_USAGE_STATS -> Icons.TwoTone.DataUsage
        APerm.GET_ACCOUNTS -> Icons.TwoTone.People
        APerm.MANAGE_ACCOUNTS -> Icons.TwoTone.ManageAccounts
        APerm.QUERY_ALL_PACKAGES -> Icons.TwoTone.Inventory2

        // System / Other
        APerm.WAKE_LOCK -> Icons.TwoTone.LocalCafe
        APerm.REBOOT -> Icons.TwoTone.RestartAlt
        APerm.BOOT_COMPLETED -> Icons.TwoTone.Start
        APerm.FOREGROUND_SERVICE -> Icons.TwoTone.NotificationsActive
        APerm.TURN_SCREEN_ON -> Icons.TwoTone.PhoneAndroid
        APerm.READ_SYNC_SETTINGS -> Icons.TwoTone.Sync
        APerm.VIBRATE -> Icons.TwoTone.Vibration
        APerm.BIND_ACCESSIBILITY_SERVICE -> Icons.TwoTone.AccessibilityNew

        // Profiles / Misc
        APerm.INTERACT_ACROSS_PROFILES -> Icons.TwoTone.Work
        APerm.LOADER_USAGE_STATS -> Icons.TwoTone.DataUsage
        APerm.USE_FULL_SCREEN_INTENT -> Icons.TwoTone.PictureInPicture

        else -> null
    }

val AExtraPerm.icon: ImageVector?
    get() = when (this) {
        AExtraPerm.PICTURE_IN_PICTURE -> Icons.TwoTone.PictureInPicture
        else -> null
    }

fun Permission.Id.toIcon(): ImageVector? =
    APerm.values.firstOrNull { it.id == this }?.icon
        ?: AExtraPerm.values.firstOrNull { it.id == this }?.icon
