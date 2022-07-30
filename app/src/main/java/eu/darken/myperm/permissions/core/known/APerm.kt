@file:Suppress("unused", "ClassName")

package eu.darken.myperm.permissions.core.known

import android.Manifest
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.CommonPerm
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionAction
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.features.HasGroup
import eu.darken.myperm.permissions.core.features.Highlighted
import kotlin.reflect.full.isSubclassOf

@Keep
sealed class APerm constructor(override val id: Permission.Id) : Permission, HasGroup {

    @get:DrawableRes open val iconRes: Int? = null
    @get:StringRes open val labelRes: Int? = null
    @get:StringRes open val descriptionRes: Int? = null

    override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Other)

    constructor(rawPermissionId: String) : this(Permission.Id(rawPermissionId))

    override fun getAction(context: Context): PermissionAction = PermissionAction.None(this)

    /**
     * FILES
     */

    object WRITE_MEDIA_STORAGE : APerm("android.permission.WRITE_MEDIA_STORAGE"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object READ_MEDIA_STORAGE : APerm("android.permission.READ_MEDIA_STORAGE"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object WRITE_EXTERNAL_STORAGE : APerm("android.permission.WRITE_EXTERNAL_STORAGE"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object READ_EXTERNAL_STORAGE : APerm("android.permission.READ_EXTERNAL_STORAGE"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object MANAGE_EXTERNAL_STORAGE : APerm("android.permission.MANAGE_EXTERNAL_STORAGE"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object MANAGE_DOCUMENTS : APerm("android.permission.MANAGE_DOCUMENTS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object MOUNT_FORMAT_FILESYSTEMS : APerm("android.permission.MOUNT_FORMAT_FILESYSTEMS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object MOUNT_UNMOUNT_FILESYSTEMS : APerm("android.permission.MOUNT_UNMOUNT_FILESYSTEMS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object MANAGE_MEDIA : APerm("android.permission.WRITE_MEDIA_STORAGE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_manage_media_24
        override val labelRes: Int = R.string.permission_write_media_storage_label
        override val descriptionRes: Int = R.string.permission_write_media_storage_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object ACCESS_MEDIA_LOCATION : APerm("android.permission.ACCESS_MEDIA_LOCATION") {
        override val iconRes: Int = R.drawable.ic_access_media_location_24
        override val labelRes: Int = R.string.permission_access_media_location_label
        override val descriptionRes: Int = R.string.permission_access_media_location_description
    }

    /**
     * CONTACTS
     */

    object READ_CONTACTS : APerm("android.permission.READ_CONTACTS"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Contacts)
    }

    object WRITE_CONTACTS : APerm("android.permission.WRITE_CONTACTS"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Contacts)
    }

    /**
     * LOCATION
     */

    object ACCESS_FINE_LOCATION : APerm("android.permission.ACCESS_FINE_LOCATION"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_location_fine_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object ACCESS_COARSE_LOCATION : APerm("android.permission.ACCESS_COARSE_LOCATION"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_location_coarse_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object ACCESS_BACKGROUND_LOCATION : APerm("android.permission.ACCESS_BACKGROUND_LOCATION"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_access_background_location_24
        override val labelRes: Int = R.string.permission_access_background_location_label
        override val descriptionRes: Int = R.string.permission_access_background_location_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object ACCESS_LOCATION_EXTRA_COMMANDS : APerm("android.permission.ACCESS_LOCATION_EXTRA_COMMANDS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object CONTROL_LOCATION_UPDATES : APerm("android.permission.CONTROL_LOCATION_UPDATES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object INSTALL_LOCATION_PROVIDER : APerm("android.permission.INSTALL_LOCATION_PROVIDER") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object LOCATION_HARDWARE : APerm("android.permission.LOCATION_HARDWARE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    /**
     * CONNECTIVITY
     */

    object BLUETOOTH : APerm("android.permission.BLUETOOTH"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity, APermGrp.Location)
    }

    object BLUETOOTH_ADMIN : APerm("android.permission.BLUETOOTH_ADMIN"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object BLUETOOTH_CONNECT : APerm("android.permission.BLUETOOTH_CONNECT"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object BLUETOOTH_SCAN : APerm("android.permission.BLUETOOTH_SCAN"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity, APermGrp.Location)
    }

    object BLUETOOTH_ADVERTISE : APerm("android.permission.BLUETOOTH_ADVERTISE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_bluetooth_advertise_24
        override val labelRes: Int = R.string.permission_bluetooth_advertise_label
        override val descriptionRes: Int = R.string.permission_bluetooth_advertise_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object BLUETOOTH_PRIVILEGED : APerm("android.permission.BLUETOOTH_PRIVILEGED") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    /**
     * MESSAGING
     */

    object RECEIVE_SMS : APerm("android.permission.RECEIVE_SMS"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object SEND_SMS : APerm("android.permission.SEND_SMS"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object READ_SMS : APerm("android.permission.READ_SMS"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object RECEIVE_WAP_PUSH : APerm("android.permission.RECEIVE_WAP_PUSH") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object RECEIVE_MMS : APerm("android.permission.RECEIVE_MMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object BROADCAST_SMS : APerm("android.permission.BROADCAST_SMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object SMS_FINANCIAL_TRANSACTIONS : APerm("android.permission.SMS_FINANCIAL_TRANSACTIONS"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object SEND_RESPOND_VIA_MESSAGE : APerm("android.permission.SEND_RESPOND_VIA_MESSAGE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    /**
     * CALLS
     */

    object PHONE_CALL : APerm("android.permission.CALL_PHONE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object ANSWER_PHONE_CALLS : APerm("android.permission.ANSWER_PHONE_CALLS"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_CALL_LOG : APerm("android.permission.READ_CALL_LOG"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object WRITE_CALL_LOG : APerm("android.permission.WRITE_CALL_LOG"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object PHONE_STATE : APerm("android.permission.PHONE_STATE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object MODIFY_PHONE_STATE : APerm("android.permission.MODIFY_PHONE_STATE"), CommonPerm {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_PRECISE_PHONE_STATE : APerm("android.permission.READ_PRECISE_PHONE_STATE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_PHONE_STATE : APerm("android.permission.READ_PHONE_STATE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_PHONE_NUMBERS : APerm("android.permission.READ_PHONE_NUMBERS"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object CALL_COMPANION_APP : APerm("android.permission.CALL_COMPANION_APP") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object CALL_PHONE : APerm("android.permission.CALL_PHONE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object CALL_PRIVILEGED : APerm("android.permission.CALL_PRIVILEGED") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object MANAGE_ONGOING_CALLS : APerm("android.permission.MANAGE_ONGOING_CALLS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object MANAGE_OWN_CALLS : APerm("android.permission.MANAGE_OWN_CALLS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object PROCESS_OUTGOING_CALLS : APerm("android.permission.PROCESS_OUTGOING_CALLS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object BIND_CALL_REDIRECTION_SERVICE : APerm("android.permission.BIND_CALL_REDIRECTION_SERVICE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object BIND_INCALL_SERVICE : APerm("android.permission.BIND_INCALL_SERVICE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object ADD_VOICEMAIL : APerm("com.android.voicemail.permission.ADD_VOICEMAIL") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object BIND_VISUAL_VOICEMAIL_SERVICE : APerm("android.permission.BIND_VISUAL_VOICEMAIL_SERVICE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object BIND_VOICE_INTERACTION : APerm("android.permission.BIND_VOICE_INTERACTION") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_VOICEMAIL : APerm("com.android.voicemail.permission.READ_VOICEMAIL"), CommonPerm {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object WRITE_VOICEMAIL : APerm("com.android.voicemail.permission.WRITE_VOICEMAIL") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    /**
     * CONNECTIVITY
     */

    object INTERNET : APerm("android.permission.INTERNET"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_internet_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object ACCESS_NETWORK_STATE : APerm("android.permission.ACCESS_NETWORK_STATE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_network_state_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object WIFI_STATE : APerm("android.permission.ACCESS_WIFI_STATE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_wifi_state_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object CHANGE_WIFI_STATE : APerm("android.permission.CHANGE_WIFI_STATE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_change_wifi_state_24
        override val labelRes: Int = R.string.permission_change_wifi_state_label
        override val descriptionRes: Int = R.string.permission_change_wifi_state_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object ACCESS_WIFI_STATE : APerm("android.permission.ACCESS_WIFI_STATE"), CommonPerm {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object CHANGE_WIFI_MULTICAST_STATE : APerm("android.permission.CHANGE_WIFI_MULTICAST_STATE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object CHANGE_NETWORK_STATE : APerm("android.permission.CHANGE_NETWORK_STATE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_change_network_state_24
        override val labelRes: Int = R.string.permission_change_network_state_label
        override val descriptionRes: Int = R.string.permission_change_network_state_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object REQUEST_COMPANION_USE_DATA_IN_BACKGROUND :
        APerm("android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object NFC : APerm("android.permission.NFC"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_nfc_24
        override val labelRes: Int = R.string.permission_nfc_label
        override val descriptionRes: Int = R.string.permission_nfc_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object BIND_NFC_SERVICE : APerm("android.permission.BIND_NFC_SERVICE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object NFC_PREFERRED_PAYMENT_INFO : APerm("android.permission.NFC_PREFERRED_PAYMENT_INFO") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object NFC_TRANSACTION_EVENT : APerm("android.permission.NFC_TRANSACTION_EVENT") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    /**
     * SENSORS
     */

    object CAMERA : APerm("android.permission.CAMERA"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object RECORD_AUDIO : APerm("android.permission.RECORD_AUDIO"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_mic_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object CAPTURE_AUDIO_OUTPUT : APerm("android.permission.CAPTURE_AUDIO_OUTPUT") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object MODIFY_AUDIO_SETTINGS : APerm("android.permission.MODIFY_AUDIO_SETTINGS"), CommonPerm {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object BODY_SENSORS : APerm("android.permission.BODY_SENSORS"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_body_sensors_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object BODY_SENSORS_BACKGROUND : APerm("android.permission.BODY_SENSORS_BACKGROUND"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_body_sensors_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object ACTIVITY_RECOGNITION : APerm("android.permission.ACTIVITY_RECOGNITION"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_directions_run_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object HIGH_SAMPLING_RATE_SENSORS : APerm("android.permission.HIGH_SAMPLING_RATE_SENSORS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    /**
     * CALENDAR
     */

    object READ_CALENDAR : APerm("android.permission.READ_CALENDAR"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_calendar_today_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calendar)
    }

    object WRITE_CALENDAR : APerm("android.permission.WRITE_CALENDAR"), CommonPerm, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_edit_calendar_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calendar)
    }

    /**
     * APPS
     */

    object REBOOT : APerm("android.permission.REBOOT"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_reboot_permission_24
        override val labelRes: Int = R.string.permission_reboot_label
        override val descriptionRes: Int = R.string.permission_reboot_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object BOOT_COMPLETED : APerm("android.permission.RECEIVE_BOOT_COMPLETED") {
        override val iconRes: Int = R.drawable.ic_baseline_start_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object QUERY_ALL_PACKAGES : APerm("android.permission.QUERY_ALL_PACKAGES"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_query_all_packages_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object FOREGROUND_SERVICE : APerm("android.permission.FOREGROUND_SERVICE") {
        override val iconRes: Int = R.drawable.ic_foreground_service_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object SYSTEM_ALERT_WINDOW : APerm("android.permission.SYSTEM_ALERT_WINDOW"), Highlighted {
        override val iconRes: Int = R.drawable.ic_system_alert_window_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object PACKAGE_USAGE_STATS : APerm("android.permission.PACKAGE_USAGE_STATS"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_usage_data_access_24
        override val labelRes: Int = R.string.permission_package_usage_stats_label
        override val descriptionRes: Int = R.string.permission_package_usage_stats_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object BROADCAST_PACKAGE_REMOVED : APerm("android.permission.BROADCAST_PACKAGE_REMOVED") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object CLEAR_APP_CACHE : APerm("android.permission.CLEAR_APP_CACHE"), CommonPerm {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object DELETE_CACHE_FILES : APerm("android.permission.DELETE_CACHE_FILES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object DELETE_PACKAGES : APerm("android.permission.DELETE_PACKAGES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object GET_PACKAGE_SIZE : APerm("android.permission.GET_PACKAGE_SIZE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object INSTALL_PACKAGES : APerm("android.permission.INSTALL_PACKAGES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object REQUEST_DELETE_PACKAGES : APerm("android.permission.REQUEST_DELETE_PACKAGES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object REQUEST_INSTALL_PACKAGES : APerm("android.permission.REQUEST_INSTALL_PACKAGES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object RESTART_PACKAGES : APerm("android.permission.RESTART_PACKAGES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object UPDATE_PACKAGES_WITHOUT_USER_ACTION : APerm("android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object MODIFY_SYSTEM_SETTINGS : APerm("android.permission.WRITE_SETTINGS") {
        override val iconRes: Int = R.drawable.ic_modify_system_settings_24
        override val labelRes: Int = R.string.permission_write_settings_label
        override val descriptionRes: Int = R.string.permission_write_settings_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object GET_ACCOUNTS : APerm("android.permission.GET_ACCOUNTS") {
        override val iconRes: Int = R.drawable.ic_get_accounts_24
        override val labelRes: Int = R.string.permission_get_accounts_label
        override val descriptionRes: Int = R.string.permission_get_accounts_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object MANAGE_ACCOUNTS : APerm("android.permission.MANAGE_ACCOUNTS") {
        override val iconRes: Int = R.drawable.ic_manage_accounts_24
        override val labelRes: Int = R.string.permission_manage_accounts_label
        override val descriptionRes: Int = R.string.permission_manage_accounts_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object WAKE_LOCK : APerm(Manifest.permission.WAKE_LOCK), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_coffee_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object REQUEST_IGNORE_BATTERY_OPTIMIZATIONS : APerm("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS") {
        override val iconRes: Int = R.drawable.ic_baseline_battery_charging_full_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    /**
     * OTHER
     */
    object READ_SYNC_SETTINGS : APerm("android.permission.READ_SYNC_SETTINGS") {
        override val iconRes: Int = R.drawable.ic_read_sync_settings_24
        override val labelRes: Int = R.string.permission_read_sync_settings_label
        override val descriptionRes: Int = R.string.permission_read_sync_settings_description
    }

    object VIBRATE : APerm("android.permission.VIBRATE"), CommonPerm {
        override val iconRes: Int = R.drawable.ic_baseline_vibration_24
    }

    object ACCEPT_HANDOVER : APerm("android.permission.ACCEPT_HANDOVER")
    object ACCESS_BLOBS_ACROSS_USERS : APerm("android.permission.ACCESS_BLOBS_ACROSS_USERS")
    object ACCESS_CHECKIN_PROPERTIES : APerm("android.permission.ACCESS_CHECKIN_PROPERTIES")

    object ACCESS_NOTIFICATION_POLICY : APerm("android.permission.ACCESS_NOTIFICATION_POLICY")
    object ACCOUNT_MANAGER : APerm("android.permission.ACCOUNT_MANAGER"), CommonPerm
    object BATTERY_STATS : APerm("android.permission.BATTERY_STATS"), CommonPerm
    object BIND_ACCESSIBILITY_SERVICE : APerm("android.permission.BIND_ACCESSIBILITY_SERVICE")
    object BIND_APPWIDGET : APerm("android.permission.BIND_APPWIDGET")
    object BIND_AUTOFILL_SERVICE : APerm("android.permission.BIND_AUTOFILL_SERVICE")

    object BIND_CARRIER_MESSAGING_CLIENT_SERVICE : APerm("android.permission.BIND_CARRIER_MESSAGING_CLIENT_SERVICE")
    object BIND_CARRIER_MESSAGING_SERVICE : APerm("android.permission.BIND_CARRIER_MESSAGING_SERVICE")
    object BIND_CARRIER_SERVICES : APerm("android.permission.BIND_CARRIER_SERVICES")
    object BIND_CHOOSER_TARGET_SERVICE : APerm("android.permission.BIND_CHOOSER_TARGET_SERVICE")
    object BIND_COMPANION_DEVICE_SERVICE : APerm("android.permission.BIND_COMPANION_DEVICE_SERVICE")
    object BIND_CONDITION_PROVIDER_SERVICE : APerm("android.permission.BIND_CONDITION_PROVIDER_SERVICE")
    object BIND_CONTROLS : APerm("android.permission.BIND_CONTROLS")
    object BIND_DEVICE_ADMIN : APerm("android.permission.BIND_DEVICE_ADMIN"), CommonPerm
    object BIND_DREAM_SERVICE : APerm("android.permission.BIND_DREAM_SERVICE")

    object BIND_INPUT_METHOD : APerm("android.permission.BIND_INPUT_METHOD")
    object BIND_MIDI_DEVICE_SERVICE : APerm("android.permission.BIND_MIDI_DEVICE_SERVICE")
    object BIND_NOTIFICATION_LISTENER_SERVICE : APerm("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")
    object BIND_PRINT_SERVICE : APerm("android.permission.BIND_PRINT_SERVICE")
    object BIND_QUICK_ACCESS_WALLET_SERVICE : APerm("android.permission.BIND_QUICK_ACCESS_WALLET_SERVICE")
    object BIND_QUICK_SETTINGS_TILE : APerm("android.permission.BIND_QUICK_SETTINGS_TILE")
    object BIND_REMOTEVIEWS : APerm("android.permission.BIND_REMOTEVIEWS")
    object BIND_SCREENING_SERVICE : APerm("android.permission.BIND_SCREENING_SERVICE")
    object BIND_TELECOM_CONNECTION_SERVICE : APerm("android.permission.BIND_TELECOM_CONNECTION_SERVICE")
    object BIND_TEXT_SERVICE : APerm("android.permission.BIND_TEXT_SERVICE")
    object BIND_TV_INPUT : APerm("android.permission.BIND_TV_INPUT")

    object BIND_VPN_SERVICE : APerm("android.permission.BIND_VPN_SERVICE")
    object BIND_VR_LISTENER_SERVICE : APerm("android.permission.BIND_VR_LISTENER_SERVICE")
    object BIND_WALLPAPER : APerm("android.permission.BIND_WALLPAPER")

    object BROADCAST_STICKY : APerm("android.permission.BROADCAST_STICKY")
    object BROADCAST_WAP_PUSH : APerm("android.permission.BROADCAST_WAP_PUSH")


    object CHANGE_COMPONENT_ENABLED_STATE : APerm("android.permission.CHANGE_COMPONENT_ENABLED_STATE")
    object CHANGE_CONFIGURATION : APerm("android.permission.CHANGE_CONFIGURATION")


    object DIAGNOSTIC : APerm("android.permission.DIAGNOSTIC")
    object DISABLE_KEYGUARD : APerm("android.permission.DISABLE_KEYGUARD")
    object DUMP : APerm("android.permission.DUMP")
    object EXPAND_STATUS_BAR : APerm("android.permission.EXPAND_STATUS_BAR")
    object FACTORY_TEST : APerm("android.permission.FACTORY_TEST")
    object GET_ACCOUNTS_PRIVILEGED : APerm("android.permission.GET_ACCOUNTS_PRIVILEGED")

    object GET_TASKS : APerm("android.permission.GET_TASKS")
    object GLOBAL_SEARCH : APerm("android.permission.GLOBAL_SEARCH")
    object HIDE_OVERLAY_WINDOWS : APerm("android.permission.HIDE_OVERLAY_WINDOWS")

    object INSTALL_SHORTCUT : APerm("com.android.launcher.permission.INSTALL_SHORTCUT"), CommonPerm
    object INSTANT_APP_FOREGROUND_SERVICE : APerm("android.permission.INSTANT_APP_FOREGROUND_SERVICE")
    object INTERACT_ACROSS_PROFILES : APerm("android.permission.INTERACT_ACROSS_PROFILES"), CommonPerm
    object KILL_BACKGROUND_PROCESSES : APerm("android.permission.KILL_BACKGROUND_PROCESSES")
    object LOADER_USAGE_STATS : APerm("android.permission.LOADER_USAGE_STATS")

    object MASTER_CLEAR : APerm("android.permission.MASTER_CLEAR")
    object MEDIA_CONTENT_CONTROL : APerm("android.permission.MEDIA_CONTENT_CONTROL")

    object PERSISTENT_ACTIVITY : APerm("android.permission.PERSISTENT_ACTIVITY")

    object READ_INPUT_STATE : APerm("android.permission.READ_INPUT_STATE")
    object READ_LOGS : APerm("android.permission.READ_LOGS")
    object READ_SYNC_STATS : APerm("android.permission.READ_SYNC_STATS")

    object RECEIVE_BOOT_COMPLETED : APerm("android.permission.RECEIVE_BOOT_COMPLETED")
    object REORDER_TASKS : APerm("android.permission.REORDER_TASKS")
    object REQUEST_COMPANION_PROFILE_WATCH : APerm("android.permission.REQUEST_COMPANION_PROFILE_WATCH")
    object REQUEST_COMPANION_RUN_IN_BACKGROUND : APerm("android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND")
    object REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND :
        APerm("android.permission.REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND")

    object REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE :
        APerm("android.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE")

    object REQUEST_PASSWORD_COMPLEXITY : APerm("android.permission.REQUEST_PASSWORD_COMPLEXITY")

    object SCHEDULE_EXACT_ALARM : APerm("android.permission.SCHEDULE_EXACT_ALARM")
    object SET_ALARM : APerm("com.android.alarm.permission.SET_ALARM"), CommonPerm
    object SET_ALWAYS_FINISH : APerm("android.permission.SET_ALWAYS_FINISH")
    object SET_ANIMATION_SCALE : APerm("android.permission.SET_ANIMATION_SCALE")
    object SET_DEBUG_APP : APerm("android.permission.SET_DEBUG_APP")
    object SET_PREFERRED_APPLICATIONS : APerm("android.permission.SET_PREFERRED_APPLICATIONS")
    object SET_PROCESS_LIMIT : APerm("android.permission.SET_PROCESS_LIMIT")
    object SET_TIME : APerm("android.permission.SET_TIME")
    object SET_TIME_ZONE : APerm("android.permission.SET_TIME_ZONE")
    object SET_WALLPAPER : APerm("android.permission.SET_WALLPAPER"), CommonPerm
    object SET_WALLPAPER_HINTS : APerm("android.permission.SET_WALLPAPER_HINTS")
    object SIGNAL_PERSISTENT_PROCESSES : APerm("android.permission.SIGNAL_PERSISTENT_PROCESSES")
    object START_FOREGROUND_SERVICES_FROM_BACKGROUND :
        APerm("android.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND"), CommonPerm

    object START_VIEW_PERMISSION_USAGE : APerm("android.permission.START_VIEW_PERMISSION_USAGE")
    object STATUS_BAR : APerm("android.permission.STATUS_BAR")
    object TRANSMIT_IR : APerm("android.permission.TRANSMIT_IR")
    object UNINSTALL_SHORTCUT : APerm("com.android.launcher.permission.UNINSTALL_SHORTCUT")
    object UPDATE_DEVICE_STATS : APerm("android.permission.UPDATE_DEVICE_STATS")

    object USE_BIOMETRIC : APerm("android.permission.USE_BIOMETRIC")
    object USE_FINGERPRINT : APerm("android.permission.USE_FINGERPRINT")
    object USE_FULL_SCREEN_INTENT : APerm("android.permission.USE_FULL_SCREEN_INTENT")
    object USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER : APerm("android.permission.USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER")
    object USE_SIP : APerm("android.permission.USE_SIP")
    object UWB_RANGING : APerm("android.permission.UWB_RANGING")
    object WRITE_APN_SETTINGS : APerm("android.permission.WRITE_APN_SETTINGS")
    object WRITE_GSERVICES : APerm("android.permission.WRITE_GSERVICES")
    object WRITE_SECURE_SETTINGS : APerm("android.permission.WRITE_SECURE_SETTINGS"), CommonPerm
    object WRITE_SETTINGS : APerm("android.permission.WRITE_SETTINGS"), CommonPerm
    object WRITE_SYNC_SETTINGS : APerm("android.permission.WRITE_SYNC_SETTINGS")

    companion object {
        val values: List<APerm> by lazy {
            APerm::class.nestedClasses
                .filter { clazz -> clazz.isSubclassOf(APerm::class) }
                .map { clazz -> clazz.objectInstance }
                .filterIsInstance<APerm>()
        }
    }
}

private fun grpIds(vararg groups: PermissionGroup): Set<PermissionGroup.Id> = groups.map { it.id }.toSet()

fun Permission.Id.toKnownPermission(): APerm? =
    APerm.values.singleOrNull { it.id == this@toKnownPermission }

fun Permission.getGroup(): Collection<APermGrp> =
    (this as HasGroup?)?.groupIds?.map { grpId -> APermGrp.values.single { it.id == grpId } } ?: emptySet()