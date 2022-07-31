@file:Suppress("unused", "ClassName")

package eu.darken.myperm.permissions.core.known

import android.Manifest
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionAction
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.features.HasGroup
import eu.darken.myperm.permissions.core.features.HasTags
import eu.darken.myperm.permissions.core.features.Highlighted
import eu.darken.myperm.permissions.core.features.SpecialAccessPerm
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

    object WRITE_MEDIA_STORAGE : APerm("android.permission.WRITE_MEDIA_STORAGE"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object READ_MEDIA_STORAGE : APerm("android.permission.READ_MEDIA_STORAGE"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object WRITE_EXTERNAL_STORAGE : APerm("android.permission.WRITE_EXTERNAL_STORAGE"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object READ_EXTERNAL_STORAGE : APerm("android.permission.READ_EXTERNAL_STORAGE"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object MANAGE_EXTERNAL_STORAGE : APerm("android.permission.MANAGE_EXTERNAL_STORAGE"),
        HasTags, Highlighted, SpecialAccessPerm {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object MANAGE_DOCUMENTS : APerm("android.permission.MANAGE_DOCUMENTS"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object MOUNT_FORMAT_FILESYSTEMS : APerm("android.permission.MOUNT_FORMAT_FILESYSTEMS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object MOUNT_UNMOUNT_FILESYSTEMS : APerm("android.permission.MOUNT_UNMOUNT_FILESYSTEMS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object MANAGE_MEDIA : APerm("android.permission.WRITE_MEDIA_STORAGE"), HasTags {
        override val iconRes: Int = R.drawable.ic_manage_media_24
        override val labelRes: Int = R.string.permission_write_media_storage_label
        override val descriptionRes: Int = R.string.permission_write_media_storage_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
    }

    object ACCESS_MEDIA_LOCATION : APerm("android.permission.ACCESS_MEDIA_LOCATION"), HasTags {
        override val iconRes: Int = R.drawable.ic_access_media_location_24
        override val labelRes: Int = R.string.permission_access_media_location_label
        override val descriptionRes: Int = R.string.permission_access_media_location_description
    }

    /**
     * CONTACTS
     */

    object READ_CONTACTS : APerm("android.permission.READ_CONTACTS"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Contacts)
    }

    object WRITE_CONTACTS : APerm("android.permission.WRITE_CONTACTS"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Contacts)
    }

    /**
     * LOCATION
     */

    object ACCESS_FINE_LOCATION : APerm("android.permission.ACCESS_FINE_LOCATION"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_location_fine_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object ACCESS_COARSE_LOCATION : APerm("android.permission.ACCESS_COARSE_LOCATION"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_location_coarse_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object ACCESS_BACKGROUND_LOCATION : APerm("android.permission.ACCESS_BACKGROUND_LOCATION"), HasTags {
        override val iconRes: Int = R.drawable.ic_access_background_location_24
        override val labelRes: Int = R.string.permission_access_background_location_label
        override val descriptionRes: Int = R.string.permission_access_background_location_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object ACCESS_LOCATION_EXTRA_COMMANDS : APerm("android.permission.ACCESS_LOCATION_EXTRA_COMMANDS"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object CONTROL_LOCATION_UPDATES : APerm("android.permission.CONTROL_LOCATION_UPDATES"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object INSTALL_LOCATION_PROVIDER : APerm("android.permission.INSTALL_LOCATION_PROVIDER"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object LOCATION_HARDWARE : APerm("android.permission.LOCATION_HARDWARE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    /**
     * CONNECTIVITY
     */

    object BLUETOOTH : APerm("android.permission.BLUETOOTH"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity, APermGrp.Location)
    }

    object BLUETOOTH_ADMIN : APerm("android.permission.BLUETOOTH_ADMIN"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object BLUETOOTH_CONNECT : APerm("android.permission.BLUETOOTH_CONNECT"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object BLUETOOTH_SCAN : APerm("android.permission.BLUETOOTH_SCAN"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity, APermGrp.Location)
    }

    object BLUETOOTH_ADVERTISE : APerm("android.permission.BLUETOOTH_ADVERTISE"), HasTags {
        override val iconRes: Int = R.drawable.ic_bluetooth_advertise_24
        override val labelRes: Int = R.string.permission_bluetooth_advertise_label
        override val descriptionRes: Int = R.string.permission_bluetooth_advertise_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object BLUETOOTH_PRIVILEGED : APerm("android.permission.BLUETOOTH_PRIVILEGED"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    /**
     * MESSAGING
     */

    object RECEIVE_SMS : APerm("android.permission.RECEIVE_SMS"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object SEND_SMS : APerm("android.permission.SEND_SMS"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object READ_SMS : APerm("android.permission.READ_SMS"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object RECEIVE_WAP_PUSH : APerm("android.permission.RECEIVE_WAP_PUSH"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object RECEIVE_MMS : APerm("android.permission.RECEIVE_MMS"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object BROADCAST_SMS : APerm("android.permission.BROADCAST_SMS"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object SMS_FINANCIAL_TRANSACTIONS : APerm("android.permission.SMS_FINANCIAL_TRANSACTIONS"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    object SEND_RESPOND_VIA_MESSAGE : APerm("android.permission.SEND_RESPOND_VIA_MESSAGE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
    }

    /**
     * CALLS
     */

    object PHONE_CALL : APerm("android.permission.CALL_PHONE"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object ANSWER_PHONE_CALLS : APerm("android.permission.ANSWER_PHONE_CALLS"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_CALL_LOG : APerm("android.permission.READ_CALL_LOG"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object WRITE_CALL_LOG : APerm("android.permission.WRITE_CALL_LOG"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object PHONE_STATE : APerm("android.permission.PHONE_STATE"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object MODIFY_PHONE_STATE : APerm("android.permission.MODIFY_PHONE_STATE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_PRECISE_PHONE_STATE : APerm("android.permission.READ_PRECISE_PHONE_STATE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_PHONE_STATE : APerm("android.permission.READ_PHONE_STATE"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_PHONE_NUMBERS : APerm("android.permission.READ_PHONE_NUMBERS"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object CALL_COMPANION_APP : APerm("android.permission.CALL_COMPANION_APP"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object CALL_PHONE : APerm("android.permission.CALL_PHONE"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object CALL_PRIVILEGED : APerm("android.permission.CALL_PRIVILEGED"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object MANAGE_ONGOING_CALLS : APerm("android.permission.MANAGE_ONGOING_CALLS"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object MANAGE_OWN_CALLS : APerm("android.permission.MANAGE_OWN_CALLS"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object PROCESS_OUTGOING_CALLS : APerm("android.permission.PROCESS_OUTGOING_CALLS"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object BIND_CALL_REDIRECTION_SERVICE : APerm("android.permission.BIND_CALL_REDIRECTION_SERVICE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object BIND_INCALL_SERVICE : APerm("android.permission.BIND_INCALL_SERVICE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object ADD_VOICEMAIL : APerm("com.android.voicemail.permission.ADD_VOICEMAIL"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object BIND_VISUAL_VOICEMAIL_SERVICE : APerm("android.permission.BIND_VISUAL_VOICEMAIL_SERVICE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object BIND_VOICE_INTERACTION : APerm("android.permission.BIND_VOICE_INTERACTION"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_VOICEMAIL : APerm("com.android.voicemail.permission.READ_VOICEMAIL"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object WRITE_VOICEMAIL : APerm("com.android.voicemail.permission.WRITE_VOICEMAIL"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    /**
     * CONNECTIVITY
     */

    object INTERNET : APerm("android.permission.INTERNET"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_internet_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object ACCESS_NETWORK_STATE : APerm("android.permission.ACCESS_NETWORK_STATE"), HasTags {
        override val iconRes: Int = R.drawable.ic_network_state_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object WIFI_STATE : APerm("android.permission.ACCESS_WIFI_STATE"), HasTags {
        override val iconRes: Int = R.drawable.ic_wifi_state_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object CHANGE_WIFI_STATE : APerm("android.permission.CHANGE_WIFI_STATE"), HasTags {
        override val iconRes: Int = R.drawable.ic_change_wifi_state_24
        override val labelRes: Int = R.string.permission_change_wifi_state_label
        override val descriptionRes: Int = R.string.permission_change_wifi_state_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object ACCESS_WIFI_STATE : APerm("android.permission.ACCESS_WIFI_STATE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object CHANGE_WIFI_MULTICAST_STATE : APerm("android.permission.CHANGE_WIFI_MULTICAST_STATE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object CHANGE_NETWORK_STATE : APerm("android.permission.CHANGE_NETWORK_STATE"), HasTags {
        override val iconRes: Int = R.drawable.ic_change_network_state_24
        override val labelRes: Int = R.string.permission_change_network_state_label
        override val descriptionRes: Int = R.string.permission_change_network_state_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object REQUEST_COMPANION_USE_DATA_IN_BACKGROUND :
        APerm("android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object NFC : APerm("android.permission.NFC"), HasTags {
        override val iconRes: Int = R.drawable.ic_nfc_24
        override val labelRes: Int = R.string.permission_nfc_label
        override val descriptionRes: Int = R.string.permission_nfc_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object BIND_NFC_SERVICE : APerm("android.permission.BIND_NFC_SERVICE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object NFC_PREFERRED_PAYMENT_INFO : APerm("android.permission.NFC_PREFERRED_PAYMENT_INFO"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object NFC_TRANSACTION_EVENT : APerm("android.permission.NFC_TRANSACTION_EVENT"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    /**
     * SENSORS
     */

    object CAMERA : APerm("android.permission.CAMERA"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object RECORD_AUDIO : APerm("android.permission.RECORD_AUDIO"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_mic_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object CAPTURE_AUDIO_OUTPUT : APerm("android.permission.CAPTURE_AUDIO_OUTPUT"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object MODIFY_AUDIO_SETTINGS : APerm("android.permission.MODIFY_AUDIO_SETTINGS"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object BODY_SENSORS : APerm("android.permission.BODY_SENSORS"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_body_sensors_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object BODY_SENSORS_BACKGROUND : APerm("android.permission.BODY_SENSORS_BACKGROUND"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_body_sensors_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object ACTIVITY_RECOGNITION : APerm("android.permission.ACTIVITY_RECOGNITION"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_directions_run_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object HIGH_SAMPLING_RATE_SENSORS : APerm("android.permission.HIGH_SAMPLING_RATE_SENSORS"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    /**
     * CALENDAR
     */

    object READ_CALENDAR : APerm("android.permission.READ_CALENDAR"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_calendar_today_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calendar)
    }

    object WRITE_CALENDAR : APerm("android.permission.WRITE_CALENDAR"), HasTags, Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_edit_calendar_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calendar)
    }

    /**
     * APPS
     */

    object REBOOT : APerm("android.permission.REBOOT"), HasTags {
        override val iconRes: Int = R.drawable.ic_reboot_permission_24
        override val labelRes: Int = R.string.permission_reboot_label
        override val descriptionRes: Int = R.string.permission_reboot_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object BOOT_COMPLETED : APerm("android.permission.RECEIVE_BOOT_COMPLETED"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_start_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object QUERY_ALL_PACKAGES : APerm("android.permission.QUERY_ALL_PACKAGES"), HasTags {
        override val iconRes: Int = R.drawable.ic_query_all_packages_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object FOREGROUND_SERVICE : APerm("android.permission.FOREGROUND_SERVICE"), HasTags {
        override val iconRes: Int = R.drawable.ic_foreground_service_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object SYSTEM_ALERT_WINDOW : APerm("android.permission.SYSTEM_ALERT_WINDOW"), Highlighted {
        override val iconRes: Int = R.drawable.ic_system_alert_window_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object PACKAGE_USAGE_STATS : APerm("android.permission.PACKAGE_USAGE_STATS"), HasTags {
        override val iconRes: Int = R.drawable.ic_usage_data_access_24
        override val labelRes: Int = R.string.permission_package_usage_stats_label
        override val descriptionRes: Int = R.string.permission_package_usage_stats_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object BROADCAST_PACKAGE_REMOVED : APerm("android.permission.BROADCAST_PACKAGE_REMOVED"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object CLEAR_APP_CACHE : APerm("android.permission.CLEAR_APP_CACHE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object DELETE_CACHE_FILES : APerm("android.permission.DELETE_CACHE_FILES"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object DELETE_PACKAGES : APerm("android.permission.DELETE_PACKAGES"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object GET_PACKAGE_SIZE : APerm("android.permission.GET_PACKAGE_SIZE"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object INSTALL_PACKAGES : APerm("android.permission.INSTALL_PACKAGES"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object REQUEST_DELETE_PACKAGES : APerm("android.permission.REQUEST_DELETE_PACKAGES"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object REQUEST_INSTALL_PACKAGES : APerm("android.permission.REQUEST_INSTALL_PACKAGES"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object RESTART_PACKAGES : APerm("android.permission.RESTART_PACKAGES"), HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object UPDATE_PACKAGES_WITHOUT_USER_ACTION : APerm("android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION"),
        HasTags {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object MODIFY_SYSTEM_SETTINGS : APerm("android.permission.WRITE_SETTINGS"), HasTags {
        override val iconRes: Int = R.drawable.ic_modify_system_settings_24
        override val labelRes: Int = R.string.permission_write_settings_label
        override val descriptionRes: Int = R.string.permission_write_settings_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object GET_ACCOUNTS : APerm("android.permission.GET_ACCOUNTS"), HasTags {
        override val iconRes: Int = R.drawable.ic_get_accounts_24
        override val labelRes: Int = R.string.permission_get_accounts_label
        override val descriptionRes: Int = R.string.permission_get_accounts_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object MANAGE_ACCOUNTS : APerm("android.permission.MANAGE_ACCOUNTS"), HasTags {
        override val iconRes: Int = R.drawable.ic_manage_accounts_24
        override val labelRes: Int = R.string.permission_manage_accounts_label
        override val descriptionRes: Int = R.string.permission_manage_accounts_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object WAKE_LOCK : APerm(Manifest.permission.WAKE_LOCK), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_coffee_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    object REQUEST_IGNORE_BATTERY_OPTIMIZATIONS : APerm("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"),
        HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_battery_charging_full_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
    }

    /**
     * OTHER
     */
    object READ_SYNC_SETTINGS : APerm("android.permission.READ_SYNC_SETTINGS"), HasTags {
        override val iconRes: Int = R.drawable.ic_read_sync_settings_24
        override val labelRes: Int = R.string.permission_read_sync_settings_label
        override val descriptionRes: Int = R.string.permission_read_sync_settings_description
    }

    object VIBRATE : APerm("android.permission.VIBRATE"), HasTags {
        override val iconRes: Int = R.drawable.ic_baseline_vibration_24
    }

    object ACCEPT_HANDOVER : APerm("android.permission.ACCEPT_HANDOVER"), HasTags
    object ACCESS_BLOBS_ACROSS_USERS : APerm("android.permission.ACCESS_BLOBS_ACROSS_USERS"), HasTags
    object ACCESS_CHECKIN_PROPERTIES : APerm("android.permission.ACCESS_CHECKIN_PROPERTIES"), HasTags

    object ACCESS_NOTIFICATION_POLICY : APerm("android.permission.ACCESS_NOTIFICATION_POLICY"), HasTags
    object ACCOUNT_MANAGER : APerm("android.permission.ACCOUNT_MANAGER"), HasTags
    object BATTERY_STATS : APerm("android.permission.BATTERY_STATS"), HasTags
    object BIND_ACCESSIBILITY_SERVICE : APerm("android.permission.BIND_ACCESSIBILITY_SERVICE"), HasTags
    object BIND_APPWIDGET : APerm("android.permission.BIND_APPWIDGET"), HasTags
    object BIND_AUTOFILL_SERVICE : APerm("android.permission.BIND_AUTOFILL_SERVICE"), HasTags

    object BIND_CARRIER_MESSAGING_CLIENT_SERVICE : APerm("android.permission.BIND_CARRIER_MESSAGING_CLIENT_SERVICE"),
        HasTags

    object BIND_CARRIER_MESSAGING_SERVICE : APerm("android.permission.BIND_CARRIER_MESSAGING_SERVICE"), HasTags
    object BIND_CARRIER_SERVICES : APerm("android.permission.BIND_CARRIER_SERVICES"), HasTags
    object BIND_CHOOSER_TARGET_SERVICE : APerm("android.permission.BIND_CHOOSER_TARGET_SERVICE"), HasTags
    object BIND_COMPANION_DEVICE_SERVICE : APerm("android.permission.BIND_COMPANION_DEVICE_SERVICE"), HasTags
    object BIND_CONDITION_PROVIDER_SERVICE : APerm("android.permission.BIND_CONDITION_PROVIDER_SERVICE"), HasTags
    object BIND_CONTROLS : APerm("android.permission.BIND_CONTROLS"), HasTags
    object BIND_DEVICE_ADMIN : APerm("android.permission.BIND_DEVICE_ADMIN"), HasTags
    object BIND_DREAM_SERVICE : APerm("android.permission.BIND_DREAM_SERVICE"), HasTags

    object BIND_INPUT_METHOD : APerm("android.permission.BIND_INPUT_METHOD"), HasTags
    object BIND_MIDI_DEVICE_SERVICE : APerm("android.permission.BIND_MIDI_DEVICE_SERVICE"), HasTags
    object BIND_NOTIFICATION_LISTENER_SERVICE : APerm("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"),
        HasTags

    object BIND_PRINT_SERVICE : APerm("android.permission.BIND_PRINT_SERVICE"), HasTags
    object BIND_QUICK_ACCESS_WALLET_SERVICE : APerm("android.permission.BIND_QUICK_ACCESS_WALLET_SERVICE"), HasTags
    object BIND_QUICK_SETTINGS_TILE : APerm("android.permission.BIND_QUICK_SETTINGS_TILE"), HasTags
    object BIND_REMOTEVIEWS : APerm("android.permission.BIND_REMOTEVIEWS"), HasTags
    object BIND_SCREENING_SERVICE : APerm("android.permission.BIND_SCREENING_SERVICE"), HasTags
    object BIND_TELECOM_CONNECTION_SERVICE : APerm("android.permission.BIND_TELECOM_CONNECTION_SERVICE"), HasTags
    object BIND_TEXT_SERVICE : APerm("android.permission.BIND_TEXT_SERVICE"), HasTags
    object BIND_TV_INPUT : APerm("android.permission.BIND_TV_INPUT"), HasTags

    object BIND_VPN_SERVICE : APerm("android.permission.BIND_VPN_SERVICE"), HasTags
    object BIND_VR_LISTENER_SERVICE : APerm("android.permission.BIND_VR_LISTENER_SERVICE"), HasTags
    object BIND_WALLPAPER : APerm("android.permission.BIND_WALLPAPER"), HasTags

    object BROADCAST_STICKY : APerm("android.permission.BROADCAST_STICKY"), HasTags
    object BROADCAST_WAP_PUSH : APerm("android.permission.BROADCAST_WAP_PUSH"), HasTags


    object CHANGE_COMPONENT_ENABLED_STATE : APerm("android.permission.CHANGE_COMPONENT_ENABLED_STATE"), HasTags
    object CHANGE_CONFIGURATION : APerm("android.permission.CHANGE_CONFIGURATION"), HasTags


    object DIAGNOSTIC : APerm("android.permission.DIAGNOSTIC"), HasTags
    object DISABLE_KEYGUARD : APerm("android.permission.DISABLE_KEYGUARD"), HasTags
    object DUMP : APerm("android.permission.DUMP"), HasTags
    object EXPAND_STATUS_BAR : APerm("android.permission.EXPAND_STATUS_BAR"), HasTags
    object FACTORY_TEST : APerm("android.permission.FACTORY_TEST"), HasTags
    object GET_ACCOUNTS_PRIVILEGED : APerm("android.permission.GET_ACCOUNTS_PRIVILEGED"), HasTags

    object GET_TASKS : APerm("android.permission.GET_TASKS"), HasTags
    object GLOBAL_SEARCH : APerm("android.permission.GLOBAL_SEARCH"), HasTags
    object HIDE_OVERLAY_WINDOWS : APerm("android.permission.HIDE_OVERLAY_WINDOWS"), HasTags

    object INSTALL_SHORTCUT : APerm("com.android.launcher.permission.INSTALL_SHORTCUT"), HasTags
    object INSTANT_APP_FOREGROUND_SERVICE : APerm("android.permission.INSTANT_APP_FOREGROUND_SERVICE")
    object INTERACT_ACROSS_PROFILES : APerm("android.permission.INTERACT_ACROSS_PROFILES"), HasTags
    object KILL_BACKGROUND_PROCESSES : APerm("android.permission.KILL_BACKGROUND_PROCESSES"), HasTags
    object LOADER_USAGE_STATS : APerm("android.permission.LOADER_USAGE_STATS"), HasTags

    object MASTER_CLEAR : APerm("android.permission.MASTER_CLEAR"), HasTags
    object MEDIA_CONTENT_CONTROL : APerm("android.permission.MEDIA_CONTENT_CONTROL"), HasTags

    object PERSISTENT_ACTIVITY : APerm("android.permission.PERSISTENT_ACTIVITY"), HasTags

    object READ_INPUT_STATE : APerm("android.permission.READ_INPUT_STATE"), HasTags
    object READ_LOGS : APerm("android.permission.READ_LOGS"), HasTags
    object READ_SYNC_STATS : APerm("android.permission.READ_SYNC_STATS"), HasTags

    object RECEIVE_BOOT_COMPLETED : APerm("android.permission.RECEIVE_BOOT_COMPLETED"), HasTags
    object REORDER_TASKS : APerm("android.permission.REORDER_TASKS"), HasTags
    object REQUEST_COMPANION_PROFILE_WATCH : APerm("android.permission.REQUEST_COMPANION_PROFILE_WATCH"), HasTags
    object REQUEST_COMPANION_RUN_IN_BACKGROUND : APerm("android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND"),
        HasTags

    object REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND :
        APerm("android.permission.REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND"), HasTags

    object REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE :
        APerm("android.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE"), HasTags

    object REQUEST_PASSWORD_COMPLEXITY : APerm("android.permission.REQUEST_PASSWORD_COMPLEXITY"), HasTags

    object SCHEDULE_EXACT_ALARM : APerm("android.permission.SCHEDULE_EXACT_ALARM"), HasTags
    object SET_ALARM : APerm("com.android.alarm.permission.SET_ALARM"), HasTags
    object SET_ALWAYS_FINISH : APerm("android.permission.SET_ALWAYS_FINISH"), HasTags
    object SET_ANIMATION_SCALE : APerm("android.permission.SET_ANIMATION_SCALE"), HasTags
    object SET_DEBUG_APP : APerm("android.permission.SET_DEBUG_APP"), HasTags
    object SET_PREFERRED_APPLICATIONS : APerm("android.permission.SET_PREFERRED_APPLICATIONS"), HasTags
    object SET_PROCESS_LIMIT : APerm("android.permission.SET_PROCESS_LIMIT"), HasTags
    object SET_TIME : APerm("android.permission.SET_TIME"), HasTags
    object SET_TIME_ZONE : APerm("android.permission.SET_TIME_ZONE"), HasTags
    object SET_WALLPAPER : APerm("android.permission.SET_WALLPAPER"), HasTags
    object SET_WALLPAPER_HINTS : APerm("android.permission.SET_WALLPAPER_HINTS"), HasTags
    object SIGNAL_PERSISTENT_PROCESSES : APerm("android.permission.SIGNAL_PERSISTENT_PROCESSES"), HasTags
    object START_FOREGROUND_SERVICES_FROM_BACKGROUND :
        APerm("android.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND"), HasTags

    object START_VIEW_PERMISSION_USAGE : APerm("android.permission.START_VIEW_PERMISSION_USAGE"), HasTags
    object STATUS_BAR : APerm("android.permission.STATUS_BAR"), HasTags
    object TRANSMIT_IR : APerm("android.permission.TRANSMIT_IR"), HasTags
    object UNINSTALL_SHORTCUT : APerm("com.android.launcher.permission.UNINSTALL_SHORTCUT"), HasTags
    object UPDATE_DEVICE_STATS : APerm("android.permission.UPDATE_DEVICE_STATS"), HasTags

    object USE_BIOMETRIC : APerm("android.permission.USE_BIOMETRIC"), HasTags
    object USE_FINGERPRINT : APerm("android.permission.USE_FINGERPRINT"), HasTags
    object USE_FULL_SCREEN_INTENT : APerm("android.permission.USE_FULL_SCREEN_INTENT"), HasTags
    object USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER : APerm("android.permission.USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER"),
        HasTags

    object USE_SIP : APerm("android.permission.USE_SIP"), HasTags
    object UWB_RANGING : APerm("android.permission.UWB_RANGING"), HasTags
    object WRITE_APN_SETTINGS : APerm("android.permission.WRITE_APN_SETTINGS"), HasTags
    object WRITE_GSERVICES : APerm("android.permission.WRITE_GSERVICES"), HasTags
    object WRITE_SECURE_SETTINGS : APerm("android.permission.WRITE_SECURE_SETTINGS"), HasTags
    object WRITE_SETTINGS : APerm("android.permission.WRITE_SETTINGS"), HasTags
    object WRITE_SYNC_SETTINGS : APerm("android.permission.WRITE_SYNC_SETTINGS"), HasTags

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