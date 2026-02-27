package eu.darken.myperm.common.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AccessibilityNew
import androidx.compose.material.icons.twotone.AdminPanelSettings
import androidx.compose.material.icons.twotone.Alarm
import androidx.compose.material.icons.twotone.Apps
import androidx.compose.material.icons.twotone.BatteryChargingFull
import androidx.compose.material.icons.twotone.Bluetooth
import androidx.compose.material.icons.twotone.BluetoothSearching
import androidx.compose.material.icons.twotone.Call
import androidx.compose.material.icons.twotone.CallEnd
import androidx.compose.material.icons.twotone.CalendarToday
import androidx.compose.material.icons.twotone.Contacts
import androidx.compose.material.icons.twotone.Dashboard
import androidx.compose.material.icons.twotone.DataUsage
import androidx.compose.material.icons.twotone.DeleteForever
import androidx.compose.material.icons.twotone.DeleteSweep
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.DirectionsRun
import androidx.compose.material.icons.twotone.DoNotDisturbOn
import androidx.compose.material.icons.twotone.EditCalendar
import androidx.compose.material.icons.twotone.ExpandMore
import androidx.compose.material.icons.twotone.Fingerprint
import androidx.compose.material.icons.twotone.InstallMobile
import androidx.compose.material.icons.twotone.Inventory2
import androidx.compose.material.icons.twotone.Keyboard
import androidx.compose.material.icons.twotone.Language
import androidx.compose.material.icons.twotone.Layers
import androidx.compose.material.icons.twotone.LocalCafe
import androidx.compose.material.icons.twotone.LocalPhone
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.LockOpen
import androidx.compose.material.icons.twotone.ManageAccounts
import androidx.compose.material.icons.twotone.Mic
import androidx.compose.material.icons.twotone.MonitorHeart
import androidx.compose.material.icons.twotone.MoreHoriz
import androidx.compose.material.icons.twotone.MusicNote
import androidx.compose.material.icons.twotone.MyLocation
import androidx.compose.material.icons.twotone.Nfc
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.NotificationsActive
import androidx.compose.material.icons.twotone.People
import androidx.compose.material.icons.twotone.PermMedia
import androidx.compose.material.icons.twotone.PhoneAndroid
import androidx.compose.material.icons.twotone.PhoneForwarded
import androidx.compose.material.icons.twotone.PhotoCamera
import androidx.compose.material.icons.twotone.PhotoLibrary
import androidx.compose.material.icons.twotone.PictureInPicture
import androidx.compose.material.icons.twotone.PowerSettingsNew
import androidx.compose.material.icons.twotone.Print
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material.icons.twotone.Schedule
import androidx.compose.material.icons.twotone.SdCard
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.Sensors
import androidx.compose.material.icons.twotone.SettingsApplications
import androidx.compose.material.icons.twotone.SettingsEthernet
import androidx.compose.material.icons.twotone.SettingsRemote
import androidx.compose.material.icons.twotone.ShareLocation
import androidx.compose.material.icons.twotone.Sms
import androidx.compose.material.icons.twotone.Speed
import androidx.compose.material.icons.twotone.Start
import androidx.compose.material.icons.twotone.Sync
import androidx.compose.material.icons.twotone.Tv
import androidx.compose.material.icons.twotone.Vibration
import androidx.compose.material.icons.twotone.VpnKey
import androidx.compose.material.icons.twotone.Wallpaper
import androidx.compose.material.icons.twotone.Watch
import androidx.compose.material.icons.twotone.Widgets
import androidx.compose.material.icons.twotone.Wifi
import androidx.compose.material.icons.twotone.WifiLock
import androidx.compose.material.icons.twotone.WifiTethering
import androidx.compose.material.icons.twotone.Work
import androidx.compose.ui.graphics.vector.ImageVector
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.AExtraPerm
import eu.darken.myperm.permissions.core.known.APerm
import eu.darken.myperm.permissions.core.known.APermGrp
import eu.darken.myperm.permissions.core.known.toKnownGroup

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
        APerm.MANAGE_DOCUMENTS -> Icons.TwoTone.Description
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
        APerm.BLUETOOTH_PRIVILEGED -> Icons.TwoTone.Bluetooth

        // SMS
        APerm.SEND_SMS -> Icons.TwoTone.Sms
        APerm.RECEIVE_SMS -> Icons.TwoTone.Sms
        APerm.READ_SMS -> Icons.TwoTone.Sms
        APerm.RECEIVE_WAP_PUSH -> Icons.TwoTone.Sms
        APerm.RECEIVE_MMS -> Icons.TwoTone.Sms
        APerm.BROADCAST_SMS -> Icons.TwoTone.Sms
        APerm.SMS_FINANCIAL_TRANSACTIONS -> Icons.TwoTone.Sms
        APerm.SEND_RESPOND_VIA_MESSAGE -> Icons.TwoTone.Sms
        APerm.BIND_CARRIER_MESSAGING_CLIENT_SERVICE -> Icons.TwoTone.Sms
        APerm.BIND_CARRIER_MESSAGING_SERVICE -> Icons.TwoTone.Sms
        APerm.BIND_CARRIER_SERVICES -> Icons.TwoTone.Sms

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
        APerm.MODIFY_PHONE_STATE -> Icons.TwoTone.LocalPhone
        APerm.READ_PRECISE_PHONE_STATE -> Icons.TwoTone.LocalPhone
        APerm.CALL_COMPANION_APP -> Icons.TwoTone.LocalPhone
        APerm.BIND_SCREENING_SERVICE -> Icons.TwoTone.CallEnd
        APerm.BIND_TELECOM_CONNECTION_SERVICE -> Icons.TwoTone.LocalPhone
        APerm.ACCEPT_HANDOVER -> Icons.TwoTone.PhoneForwarded
        APerm.USE_SIP -> Icons.TwoTone.LocalPhone

        // Connectivity
        APerm.INTERNET -> Icons.TwoTone.Language
        APerm.CHANGE_WIFI_STATE -> Icons.TwoTone.WifiLock
        APerm.WIFI_STATE -> Icons.TwoTone.Wifi
        APerm.ACCESS_NETWORK_STATE -> Icons.TwoTone.SettingsEthernet
        APerm.CHANGE_NETWORK_STATE -> Icons.TwoTone.SettingsEthernet
        APerm.ACCESS_WIFI_STATE -> Icons.TwoTone.Wifi
        APerm.CHANGE_WIFI_MULTICAST_STATE -> Icons.TwoTone.WifiTethering
        APerm.NFC -> Icons.TwoTone.Nfc
        APerm.BIND_NFC_SERVICE -> Icons.TwoTone.Nfc
        APerm.NFC_PREFERRED_PAYMENT_INFO -> Icons.TwoTone.Nfc
        APerm.NFC_TRANSACTION_EVENT -> Icons.TwoTone.Nfc

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
        APerm.HIGH_SAMPLING_RATE_SENSORS -> Icons.TwoTone.Speed

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
        APerm.INSTALL_PACKAGES -> Icons.TwoTone.InstallMobile
        APerm.DELETE_PACKAGES -> Icons.TwoTone.DeleteForever
        APerm.REQUEST_DELETE_PACKAGES -> Icons.TwoTone.DeleteForever
        APerm.BROADCAST_PACKAGE_REMOVED -> Icons.TwoTone.DeleteForever
        APerm.CLEAR_APP_CACHE -> Icons.TwoTone.DeleteSweep
        APerm.DELETE_CACHE_FILES -> Icons.TwoTone.DeleteSweep
        APerm.RESTART_PACKAGES -> Icons.TwoTone.RestartAlt
        APerm.UPDATE_PACKAGES_WITHOUT_USER_ACTION -> Icons.TwoTone.InstallMobile

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

        // Biometric / Security
        APerm.USE_BIOMETRIC -> Icons.TwoTone.Fingerprint
        APerm.USE_FINGERPRINT -> Icons.TwoTone.Fingerprint
        APerm.DISABLE_KEYGUARD -> Icons.TwoTone.LockOpen
        APerm.REQUEST_PASSWORD_COMPLEXITY -> Icons.TwoTone.Lock

        // Service bindings
        APerm.BIND_NOTIFICATION_LISTENER_SERVICE -> Icons.TwoTone.NotificationsActive
        APerm.BIND_VPN_SERVICE -> Icons.TwoTone.VpnKey
        APerm.BIND_WALLPAPER -> Icons.TwoTone.Wallpaper
        APerm.BIND_INPUT_METHOD -> Icons.TwoTone.Keyboard
        APerm.BIND_DEVICE_ADMIN -> Icons.TwoTone.AdminPanelSettings
        APerm.BIND_PRINT_SERVICE -> Icons.TwoTone.Print
        APerm.BIND_AUTOFILL_SERVICE -> Icons.TwoTone.Fingerprint
        APerm.BIND_QUICK_SETTINGS_TILE -> Icons.TwoTone.Dashboard
        APerm.BIND_APPWIDGET -> Icons.TwoTone.Widgets
        APerm.BIND_CONTROLS -> Icons.TwoTone.SettingsRemote
        APerm.BIND_DREAM_SERVICE -> Icons.TwoTone.PhoneAndroid
        APerm.BIND_TV_INPUT -> Icons.TwoTone.Tv
        APerm.BIND_MIDI_DEVICE_SERVICE -> Icons.TwoTone.MusicNote
        APerm.BIND_TEXT_SERVICE -> Icons.TwoTone.Description
        APerm.BIND_REMOTEVIEWS -> Icons.TwoTone.Widgets
        APerm.BIND_VR_LISTENER_SERVICE -> Icons.TwoTone.Sensors

        // Settings / System
        APerm.WRITE_SYNC_SETTINGS -> Icons.TwoTone.Sync
        APerm.READ_SYNC_STATS -> Icons.TwoTone.Sync
        APerm.SET_ALARM -> Icons.TwoTone.Alarm
        APerm.SET_WALLPAPER -> Icons.TwoTone.Wallpaper
        APerm.SET_WALLPAPER_HINTS -> Icons.TwoTone.Wallpaper
        APerm.SET_TIME -> Icons.TwoTone.Schedule
        APerm.SET_TIME_ZONE -> Icons.TwoTone.Schedule
        APerm.WRITE_SECURE_SETTINGS -> Icons.TwoTone.SettingsApplications
        APerm.WRITE_APN_SETTINGS -> Icons.TwoTone.SettingsApplications
        APerm.WRITE_GSERVICES -> Icons.TwoTone.SettingsApplications
        APerm.CHANGE_CONFIGURATION -> Icons.TwoTone.SettingsApplications
        APerm.ACCOUNT_MANAGER -> Icons.TwoTone.ManageAccounts
        APerm.GET_ACCOUNTS_PRIVILEGED -> Icons.TwoTone.People

        // Media / IO
        APerm.MEDIA_CONTENT_CONTROL -> Icons.TwoTone.MusicNote
        APerm.TRANSMIT_IR -> Icons.TwoTone.SettingsRemote

        // Process / System
        APerm.RECEIVE_BOOT_COMPLETED -> Icons.TwoTone.Start
        APerm.START_FOREGROUND_SERVICES_FROM_BACKGROUND -> Icons.TwoTone.NotificationsActive
        APerm.INSTANT_APP_FOREGROUND_SERVICE -> Icons.TwoTone.NotificationsActive
        APerm.KILL_BACKGROUND_PROCESSES -> Icons.TwoTone.PowerSettingsNew
        APerm.BATTERY_STATS -> Icons.TwoTone.BatteryChargingFull
        APerm.READ_LOGS -> Icons.TwoTone.Description
        APerm.MASTER_CLEAR -> Icons.TwoTone.DeleteForever

        // Companion device
        APerm.REQUEST_COMPANION_PROFILE_WATCH -> Icons.TwoTone.Watch
        APerm.REQUEST_COMPANION_RUN_IN_BACKGROUND -> Icons.TwoTone.Watch
        APerm.REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND -> Icons.TwoTone.Watch
        APerm.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE -> Icons.TwoTone.Watch
        APerm.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND -> Icons.TwoTone.Watch
        APerm.BIND_COMPANION_DEVICE_SERVICE -> Icons.TwoTone.Watch

        // Status bar / UI
        APerm.STATUS_BAR -> Icons.TwoTone.ExpandMore
        APerm.EXPAND_STATUS_BAR -> Icons.TwoTone.ExpandMore
        APerm.HIDE_OVERLAY_WINDOWS -> Icons.TwoTone.Layers
        APerm.INSTALL_SHORTCUT -> Icons.TwoTone.InstallMobile
        APerm.UNINSTALL_SHORTCUT -> Icons.TwoTone.DeleteForever

        // Misc
        APerm.GLOBAL_SEARCH -> Icons.TwoTone.Search
        APerm.UWB_RANGING -> Icons.TwoTone.Sensors
        APerm.DIAGNOSTIC -> Icons.TwoTone.Description

        else -> null
    }

val AExtraPerm.icon: ImageVector?
    get() = when (this) {
        AExtraPerm.PICTURE_IN_PICTURE -> Icons.TwoTone.PictureInPicture
        else -> null
    }

fun Permission.Id.toIcon(): ImageVector? {
    APerm.values.firstOrNull { it.id == this }?.let { perm ->
        return perm.icon ?: perm.groupIds.firstNotNullOfOrNull { grpId ->
            grpId.toKnownGroup()?.icon
        }
    }
    AExtraPerm.values.firstOrNull { it.id == this }?.let { extra ->
        return extra.icon ?: extra.groupIds.firstNotNullOfOrNull { grpId ->
            grpId.toKnownGroup()?.icon
        }
    }
    return null
}
