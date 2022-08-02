@file:Suppress("unused", "ClassName")

package eu.darken.myperm.permissions.core.known

import android.Manifest
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.features.*
import eu.darken.myperm.permissions.core.grpIds
import kotlin.reflect.full.isSubclassOf

@Keep
sealed class APerm constructor(val id: Permission.Id) {

    @get:DrawableRes open val iconRes: Int? = null
    @get:StringRes open val labelRes: Int? = null
    @get:StringRes open val descriptionRes: Int? = null

    open val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Other)
    open val tags: Collection<PermissionTag> = emptySet()

    constructor(rawPermissionId: String) : this(Permission.Id(rawPermissionId))

    /**
     * FILES
     */

    object MANAGE_EXTERNAL_STORAGE : APerm("android.permission.MANAGE_EXTERNAL_STORAGE") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val labelRes: Int = R.string.permission_all_files_access_label
        override val descriptionRes: Int = R.string.permission_all_files_access_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
        override val tags = setOf(Highlighted, ManifestDoc, SpecialAccess)
    }
    
    object MANAGE_MEDIA : APerm("android.permission.MANAGE_MEDIA") {
        override val iconRes: Int = R.drawable.ic_manage_media_24
        override val labelRes: Int = R.string.permission_manage_media_label
        override val descriptionRes: Int = R.string.permission_manage_media_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
        override val tags = setOf(ManifestDoc)
    }
    
    object WRITE_MEDIA_STORAGE : APerm("android.permission.WRITE_MEDIA_STORAGE") {
        override val iconRes: Int = R.drawable.ic_access_to_media_only_24
        override val labelRes: Int = R.string.permission_access_to_media_only_label
        override val descriptionRes: Int = R.string.permission_access_to_media_only_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
        override val tags = setOf(Highlighted, ManifestDoc)
    }

    object READ_MEDIA_STORAGE : APerm("android.permission.READ_MEDIA_STORAGE") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
        override val tags = setOf(Highlighted, ManifestDoc)
    }

    object WRITE_EXTERNAL_STORAGE : APerm("android.permission.WRITE_EXTERNAL_STORAGE") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val labelRes: Int = R.string.permission_files_and_media_label
        override val descriptionRes: Int = R.string.permission_files_and_media_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
        override val tags = setOf(Highlighted, ManifestDoc)
    }

    object READ_EXTERNAL_STORAGE : APerm("android.permission.READ_EXTERNAL_STORAGE") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
        override val tags = setOf(Highlighted, ManifestDoc)
    }

    object MANAGE_DOCUMENTS : APerm("android.permission.MANAGE_DOCUMENTS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
        override val tags = setOf(ManifestDoc)
    }

    object MOUNT_FORMAT_FILESYSTEMS : APerm("android.permission.MOUNT_FORMAT_FILESYSTEMS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
        override val tags = setOf(ManifestDoc)
    }

    object MOUNT_UNMOUNT_FILESYSTEMS : APerm("android.permission.MOUNT_UNMOUNT_FILESYSTEMS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Files)
        override val tags = setOf(ManifestDoc)
    }

    object ACCESS_MEDIA_LOCATION : APerm("android.permission.ACCESS_MEDIA_LOCATION") {
        override val iconRes: Int = R.drawable.ic_access_media_location_24
        override val labelRes: Int = R.string.permission_access_media_location_label
        override val descriptionRes: Int = R.string.permission_access_media_location_description
        override val tags = setOf(ManifestDoc)
    }

    /**
     * CONTACTS
     */

    object READ_CONTACTS : APerm("android.permission.READ_CONTACTS") {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Contacts)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object WRITE_CONTACTS : APerm("android.permission.WRITE_CONTACTS") {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Contacts)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    /**
     * LOCATION
     */

    object ACCESS_FINE_LOCATION : APerm("android.permission.ACCESS_FINE_LOCATION") {
        override val iconRes: Int = R.drawable.ic_location_fine_24
        override val labelRes: Int = R.string.permission_access_precise_location_label
        override val descriptionRes: Int = R.string.permission_access_precise_location_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object ACCESS_COARSE_LOCATION : APerm("android.permission.ACCESS_COARSE_LOCATION") {
        override val iconRes: Int = R.drawable.ic_location_coarse_24
        override val labelRes: Int = R.string.permission_access_approximate_location_label
        override val descriptionRes: Int = R.string.permission_access_approximate_location_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object ACCESS_BACKGROUND_LOCATION : APerm("android.permission.ACCESS_BACKGROUND_LOCATION") {
        override val iconRes: Int = R.drawable.ic_access_background_location_24
        override val labelRes: Int = R.string.permission_access_background_location_label
        override val descriptionRes: Int = R.string.permission_access_background_location_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
        override val tags = setOf(ManifestDoc)
    }

    object ACCESS_LOCATION_EXTRA_COMMANDS : APerm("android.permission.ACCESS_LOCATION_EXTRA_COMMANDS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object CONTROL_LOCATION_UPDATES : APerm("android.permission.CONTROL_LOCATION_UPDATES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
        override val tags = setOf(ManifestDoc)
    }

    object INSTALL_LOCATION_PROVIDER : APerm("android.permission.INSTALL_LOCATION_PROVIDER") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
        override val tags = setOf(ManifestDoc)
    }

    object LOCATION_HARDWARE : APerm("android.permission.LOCATION_HARDWARE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
        override val tags = setOf(ManifestDoc)
    }

    /**
     * CONNECTIVITY
     */

    object BLUETOOTH : APerm("android.permission.BLUETOOTH") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity, APermGrp.Location)
        override val tags = setOf(ManifestDoc)
    }

    object BLUETOOTH_ADMIN : APerm("android.permission.BLUETOOTH_ADMIN") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object BLUETOOTH_CONNECT : APerm("android.permission.BLUETOOTH_CONNECT") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object BLUETOOTH_SCAN : APerm("android.permission.BLUETOOTH_SCAN") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity, APermGrp.Location)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object BLUETOOTH_ADVERTISE : APerm("android.permission.BLUETOOTH_ADVERTISE") {
        override val iconRes: Int = R.drawable.ic_bluetooth_advertise_24
        override val labelRes: Int = R.string.permission_bluetooth_advertise_label
        override val descriptionRes: Int = R.string.permission_bluetooth_advertise_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object BLUETOOTH_PRIVILEGED : APerm("android.permission.BLUETOOTH_PRIVILEGED") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    /**
     * MESSAGING
     */

    object RECEIVE_SMS : APerm("android.permission.RECEIVE_SMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object SEND_SMS : APerm("android.permission.SEND_SMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object READ_SMS : APerm("android.permission.READ_SMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object RECEIVE_WAP_PUSH : APerm("android.permission.RECEIVE_WAP_PUSH") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
        override val tags = setOf(ManifestDoc)
    }

    object RECEIVE_MMS : APerm("android.permission.RECEIVE_MMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
        override val tags = setOf(ManifestDoc)
    }

    object BROADCAST_SMS : APerm("android.permission.BROADCAST_SMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
        override val tags = setOf(ManifestDoc)
    }

    object SMS_FINANCIAL_TRANSACTIONS : APerm("android.permission.SMS_FINANCIAL_TRANSACTIONS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val labelRes: Int = R.string.permission_premium_sms_services_label
        override val descriptionRes: Int = R.string.permission_premium_sms_services_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
        override val tags = setOf(ManifestDoc)
    }

    object SEND_RESPOND_VIA_MESSAGE : APerm("android.permission.SEND_RESPOND_VIA_MESSAGE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messaging)
        override val tags = setOf(ManifestDoc)
    }

    /**
     * CALLS
     */

    object PHONE_CALL : APerm("android.permission.CALL_PHONE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object ANSWER_PHONE_CALLS : APerm("android.permission.ANSWER_PHONE_CALLS") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object READ_CALL_LOG : APerm("android.permission.READ_CALL_LOG") {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object WRITE_CALL_LOG : APerm("android.permission.WRITE_CALL_LOG") {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object PHONE_STATE : APerm("android.permission.PHONE_STATE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object MODIFY_PHONE_STATE : APerm("android.permission.MODIFY_PHONE_STATE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object READ_PRECISE_PHONE_STATE : APerm("android.permission.READ_PRECISE_PHONE_STATE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object READ_PHONE_STATE : APerm("android.permission.READ_PHONE_STATE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object READ_PHONE_NUMBERS : APerm("android.permission.READ_PHONE_NUMBERS") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object CALL_COMPANION_APP : APerm("android.permission.CALL_COMPANION_APP") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object CALL_PHONE : APerm("android.permission.CALL_PHONE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object CALL_PRIVILEGED : APerm("android.permission.CALL_PRIVILEGED") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object MANAGE_ONGOING_CALLS : APerm("android.permission.MANAGE_ONGOING_CALLS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object MANAGE_OWN_CALLS : APerm("android.permission.MANAGE_OWN_CALLS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object PROCESS_OUTGOING_CALLS : APerm("android.permission.PROCESS_OUTGOING_CALLS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object BIND_CALL_REDIRECTION_SERVICE : APerm("android.permission.BIND_CALL_REDIRECTION_SERVICE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object BIND_INCALL_SERVICE : APerm("android.permission.BIND_INCALL_SERVICE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object ADD_VOICEMAIL : APerm("com.android.voicemail.permission.ADD_VOICEMAIL") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object BIND_VISUAL_VOICEMAIL_SERVICE : APerm("android.permission.BIND_VISUAL_VOICEMAIL_SERVICE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object BIND_VOICE_INTERACTION : APerm("android.permission.BIND_VOICE_INTERACTION") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object READ_VOICEMAIL : APerm("com.android.voicemail.permission.READ_VOICEMAIL") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    object WRITE_VOICEMAIL : APerm("com.android.voicemail.permission.WRITE_VOICEMAIL") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
        override val tags = setOf(ManifestDoc)
    }

    /**
     * CONNECTIVITY
     */

    object INTERNET : APerm("android.permission.INTERNET") {
        override val iconRes: Int = R.drawable.ic_baseline_internet_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }
    
    object REQUEST_COMPANION_USE_DATA_IN_BACKGROUND :
        APerm("android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND") {
        override val iconRes: Int = R.drawable.ic_unlimited_data_access_24
        override val labelRes: Int = R.string.permission_unlimited_data_access_label
        override val descriptionRes: Int = R.string.permission_unlimited_data_access_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object CHANGE_WIFI_STATE : APerm("android.permission.CHANGE_WIFI_STATE") {
        override val iconRes: Int = R.drawable.ic_change_wifi_state_24
        override val labelRes: Int = R.string.permission_wifi_control_label
        override val descriptionRes: Int = R.string.permission_wifi_control_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }
    
    object WIFI_STATE : APerm("android.permission.ACCESS_WIFI_STATE") {
        override val iconRes: Int = R.drawable.ic_wifi_state_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object ACCESS_WIFI_STATE : APerm("android.permission.ACCESS_WIFI_STATE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object CHANGE_WIFI_MULTICAST_STATE : APerm("android.permission.CHANGE_WIFI_MULTICAST_STATE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }
        
    object ACCESS_NETWORK_STATE : APerm("android.permission.ACCESS_NETWORK_STATE") {
        override val iconRes: Int = R.drawable.ic_network_state_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object CHANGE_NETWORK_STATE : APerm("android.permission.CHANGE_NETWORK_STATE") {
        override val iconRes: Int = R.drawable.ic_change_network_state_24
        override val labelRes: Int = R.string.permission_change_network_state_label
        override val descriptionRes: Int = R.string.permission_change_network_state_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object NFC : APerm("android.permission.NFC") {
        override val iconRes: Int = R.drawable.ic_nfc_24
        override val labelRes: Int = R.string.permission_nfc_label
        override val descriptionRes: Int = R.string.permission_nfc_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object BIND_NFC_SERVICE : APerm("android.permission.BIND_NFC_SERVICE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object NFC_PREFERRED_PAYMENT_INFO : APerm("android.permission.NFC_PREFERRED_PAYMENT_INFO") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    object NFC_TRANSACTION_EVENT : APerm("android.permission.NFC_TRANSACTION_EVENT") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
        override val tags = setOf(ManifestDoc)
    }

    /**
     * Camera
     */

    object CAMERA : APerm("android.permission.CAMERA") {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Camera)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object CAMERA_OPEN_CLOSE_LISTENER : APerm("android.permission.CAMERA_OPEN_CLOSE_LISTENER") {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Camera)
    }

    object SYSTEM_CAMERA : APerm("android.permission.SYSTEM_CAMERA") {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Camera)
    }

    object BACKGROUND_CAMERA : APerm("android.permission.BACKGROUND_CAMERA") {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Camera)
    }

    object MANAGE_CAMERA : APerm("android.permission.MANAGE_CAMERA") {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Camera)
    }

    object CAMERA_SEND_SYSTEM_EVENTS : APerm("android.permission.MANAGE_CAMERACAMERA_SEND_SYSTEM_EVENTS") {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Camera)
    }

    object CAMERA_INJECT_EXTERNAL_CAMERA : APerm("android.permission.CAMERA_INJECT_EXTERNAL_CAMERA") {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Camera)
    }

    object CAMERA_DISABLE_TRANSMIT_LED : APerm("android.permission.CAMERA_DISABLE_TRANSMIT_LED") {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Camera)
    }

    /**
     * Record Audio
     */

    object RECORD_AUDIO : APerm("android.permission.RECORD_AUDIO") {
        override val iconRes: Int = R.drawable.ic_baseline_mic_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Audio)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object CAPTURE_AUDIO_OUTPUT : APerm("android.permission.CAPTURE_AUDIO_OUTPUT") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Audio)
        override val tags = setOf(ManifestDoc)
    }

    object MODIFY_AUDIO_SETTINGS : APerm("android.permission.MODIFY_AUDIO_SETTINGS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Audio)
        override val tags = setOf(ManifestDoc)
    }

    object RECORD_BACKGROUND_AUDIO : APerm("android.permission.RECORD_BACKGROUND_AUDIO") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Audio)
        override val tags = setOf(ManifestDoc)
    }

    /**
     * SENSORS
     */

    object BODY_SENSORS : APerm("android.permission.BODY_SENSORS") {
        override val iconRes: Int = R.drawable.ic_body_sensors_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
        override val tags = setOf(ManifestDoc, Highlighted, RuntimeGrant)
    }

    object BODY_SENSORS_BACKGROUND : APerm("android.permission.BODY_SENSORS_BACKGROUND") {
        override val iconRes: Int = R.drawable.ic_body_sensors_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object ACTIVITY_RECOGNITION : APerm("android.permission.ACTIVITY_RECOGNITION") {
        override val iconRes: Int = R.drawable.ic_baseline_directions_run_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object HIGH_SAMPLING_RATE_SENSORS : APerm("android.permission.HIGH_SAMPLING_RATE_SENSORS") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
        override val tags = setOf(ManifestDoc)
    }

    /**
     * CALENDAR
     */

    object READ_CALENDAR : APerm("android.permission.READ_CALENDAR") {
        override val iconRes: Int = R.drawable.ic_baseline_calendar_today_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calendar)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    object WRITE_CALENDAR : APerm("android.permission.WRITE_CALENDAR") {
        override val iconRes: Int = R.drawable.ic_baseline_edit_calendar_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calendar)
        override val tags = setOf(ManifestDoc, Highlighted)
    }

    /**
     * APPS
     */

    object REQUEST_IGNORE_BATTERY_OPTIMIZATIONS : APerm("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS") {
        override val iconRes: Int = R.drawable.ic_baseline_battery_charging_full_24
        override val labelRes: Int = R.string.permission_ignore_battery_optimizations_label
        override val descriptionRes: Int = R.string.permission_ignore_battery_optimizations_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }
   
    object SYSTEM_ALERT_WINDOW : APerm("android.permission.SYSTEM_ALERT_WINDOW") {
        override val iconRes: Int = R.drawable.ic_system_alert_window_24
        override val labelRes: Int = R.string.permission_appear_on_top_label
        override val descriptionRes: Int = R.string.permission_appear_on_top_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc, Highlighted)
    }
    
    object ACCESS_NOTIFICATION_POLICY : APerm("android.permission.ACCESS_NOTIFICATION_POLICY") {
        override val iconRes: Int = R.drawable.ic_access_notification_policy_24
        override val labelRes: Int = R.string.permission_do_not_disturb_permission_label
        override val descriptionRes: Int = R.string.permission_do_not_disturb_permission_description
        override val tags = setOf(ManifestDoc)
    }
    
    object WRITE_SETTINGS : APerm("android.permission.WRITE_SETTINGS") {
        override val iconRes: Int = R.drawable.ic_modify_system_settings_24
        override val labelRes: Int = R.string.permission_modify_system_settings_label
        override val descriptionRes: Int = R.string.permission_modify_system_settings_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object ACCESS_NOTIFICATIONS : APerm("android.permission.ACCESS_NOTIFICATIONS") {
        override val iconRes: Int = R.drawable.ic_access_notifications_24
        override val labelRes: Int = R.string.permission_notification_access_label
        override val descriptionRes: Int = R.string.permission_notification_access_description
        override val tags = setOf(ManifestDoc)
    }
    
    object REQUEST_INSTALL_PACKAGES : APerm("android.permission.REQUEST_INSTALL_PACKAGES") {
        override val iconRes: Int = R.drawable.ic_baseline_install_mobile_24
        override val labelRes: Int = R.string.permission_install_unknown_apps_label
        override val descriptionRes: Int = R.string.permission_install_unknown_apps_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }
    
    object SCHEDULE_EXACT_ALARM : APerm("android.permission.SCHEDULE_EXACT_ALARM") {
        override val iconRes: Int = R.drawable.ic_schedule_exact_alarm_24
        override val labelRes: Int = R.string.permission_alarms_and_reminders_label
        override val descriptionRes: Int = R.string.permission_alarms_and_reminders_description
        override val tags = setOf(ManifestDoc)
    }
    
    object PACKAGE_USAGE_STATS : APerm("android.permission.PACKAGE_USAGE_STATS") {
        override val iconRes: Int = R.drawable.ic_usage_data_access_24
        override val labelRes: Int = R.string.permission_usage_data_access_label
        override val descriptionRes: Int = R.string.permission_usage_data_access_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object BROADCAST_PACKAGE_REMOVED : APerm("android.permission.BROADCAST_PACKAGE_REMOVED") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object CLEAR_APP_CACHE : APerm("android.permission.CLEAR_APP_CACHE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object DELETE_CACHE_FILES : APerm("android.permission.DELETE_CACHE_FILES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object DELETE_PACKAGES : APerm("android.permission.DELETE_PACKAGES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object GET_PACKAGE_SIZE : APerm("android.permission.GET_PACKAGE_SIZE") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object INSTALL_PACKAGES : APerm("android.permission.INSTALL_PACKAGES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object REQUEST_DELETE_PACKAGES : APerm("android.permission.REQUEST_DELETE_PACKAGES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object RESTART_PACKAGES : APerm("android.permission.RESTART_PACKAGES") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object UPDATE_PACKAGES_WITHOUT_USER_ACTION : APerm("android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION") {
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object GET_ACCOUNTS : APerm("android.permission.GET_ACCOUNTS") {
        override val iconRes: Int = R.drawable.ic_get_accounts_24
        override val labelRes: Int = R.string.permission_get_accounts_label
        override val descriptionRes: Int = R.string.permission_get_accounts_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object MANAGE_ACCOUNTS : APerm("android.permission.MANAGE_ACCOUNTS") {
        override val iconRes: Int = R.drawable.ic_manage_accounts_24
        override val labelRes: Int = R.string.permission_manage_accounts_label
        override val descriptionRes: Int = R.string.permission_manage_accounts_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object WAKE_LOCK : APerm(Manifest.permission.WAKE_LOCK) {
        override val iconRes: Int = R.drawable.ic_baseline_coffee_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object REBOOT : APerm("android.permission.REBOOT") {
        override val iconRes: Int = R.drawable.ic_reboot_permission_24
        override val labelRes: Int = R.string.permission_reboot_label
        override val descriptionRes: Int = R.string.permission_reboot_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object BOOT_COMPLETED : APerm("android.permission.RECEIVE_BOOT_COMPLETED") {
        override val iconRes: Int = R.drawable.ic_baseline_start_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object QUERY_ALL_PACKAGES : APerm("android.permission.QUERY_ALL_PACKAGES") {
        override val iconRes: Int = R.drawable.ic_query_all_packages_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    object FOREGROUND_SERVICE : APerm("android.permission.FOREGROUND_SERVICE") {
        override val iconRes: Int = R.drawable.ic_foreground_service_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Apps)
        override val tags = setOf(ManifestDoc)
    }

    
    /**
     * OTHER
     */
    object READ_SYNC_SETTINGS : APerm("android.permission.READ_SYNC_SETTINGS") {
        override val iconRes: Int = R.drawable.ic_read_sync_settings_24
        override val labelRes: Int = R.string.permission_read_sync_settings_label
        override val descriptionRes: Int = R.string.permission_read_sync_settings_description
        override val tags = setOf(ManifestDoc)
    }

    object VIBRATE : APerm("android.permission.VIBRATE") {
        override val iconRes: Int = R.drawable.ic_baseline_vibration_24
        override val tags = setOf(ManifestDoc)
    }

    object ACCEPT_HANDOVER : APerm("android.permission.ACCEPT_HANDOVER") {
        override val tags = setOf(ManifestDoc)
    }

    object ACCESS_BLOBS_ACROSS_USERS : APerm("android.permission.ACCESS_BLOBS_ACROSS_USERS") {
        override val tags = setOf(ManifestDoc)
    }

    object ACCESS_CHECKIN_PROPERTIES : APerm("android.permission.ACCESS_CHECKIN_PROPERTIES") {
        override val tags = setOf(ManifestDoc)
    }

    object ACCOUNT_MANAGER : APerm("android.permission.ACCOUNT_MANAGER") {
        override val tags = setOf(ManifestDoc)
    }

    object BATTERY_STATS : APerm("android.permission.BATTERY_STATS") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_ACCESSIBILITY_SERVICE : APerm("android.permission.BIND_ACCESSIBILITY_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_APPWIDGET : APerm("android.permission.BIND_APPWIDGET") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_AUTOFILL_SERVICE : APerm("android.permission.BIND_AUTOFILL_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_CARRIER_MESSAGING_CLIENT_SERVICE : APerm("android.permission.BIND_CARRIER_MESSAGING_CLIENT_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_CARRIER_MESSAGING_SERVICE : APerm("android.permission.BIND_CARRIER_MESSAGING_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_CARRIER_SERVICES : APerm("android.permission.BIND_CARRIER_SERVICES") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_CHOOSER_TARGET_SERVICE : APerm("android.permission.BIND_CHOOSER_TARGET_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_COMPANION_DEVICE_SERVICE : APerm("android.permission.BIND_COMPANION_DEVICE_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_CONDITION_PROVIDER_SERVICE : APerm("android.permission.BIND_CONDITION_PROVIDER_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_CONTROLS : APerm("android.permission.BIND_CONTROLS") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_DEVICE_ADMIN : APerm("android.permission.BIND_DEVICE_ADMIN") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_DREAM_SERVICE : APerm("android.permission.BIND_DREAM_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_INPUT_METHOD : APerm("android.permission.BIND_INPUT_METHOD") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_MIDI_DEVICE_SERVICE : APerm("android.permission.BIND_MIDI_DEVICE_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_NOTIFICATION_LISTENER_SERVICE : APerm("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_PRINT_SERVICE : APerm("android.permission.BIND_PRINT_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_QUICK_ACCESS_WALLET_SERVICE : APerm("android.permission.BIND_QUICK_ACCESS_WALLET_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_QUICK_SETTINGS_TILE : APerm("android.permission.BIND_QUICK_SETTINGS_TILE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_REMOTEVIEWS : APerm("android.permission.BIND_REMOTEVIEWS") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_SCREENING_SERVICE : APerm("android.permission.BIND_SCREENING_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_TELECOM_CONNECTION_SERVICE : APerm("android.permission.BIND_TELECOM_CONNECTION_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_TEXT_SERVICE : APerm("android.permission.BIND_TEXT_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_TV_INPUT : APerm("android.permission.BIND_TV_INPUT") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_VPN_SERVICE : APerm("android.permission.BIND_VPN_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_VR_LISTENER_SERVICE : APerm("android.permission.BIND_VR_LISTENER_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object BIND_WALLPAPER : APerm("android.permission.BIND_WALLPAPER") {
        override val tags = setOf(ManifestDoc)
    }

    object BROADCAST_STICKY : APerm("android.permission.BROADCAST_STICKY") {
        override val tags = setOf(ManifestDoc)
    }

    object BROADCAST_WAP_PUSH : APerm("android.permission.BROADCAST_WAP_PUSH") {
        override val tags = setOf(ManifestDoc)
    }


    object CHANGE_COMPONENT_ENABLED_STATE : APerm("android.permission.CHANGE_COMPONENT_ENABLED_STATE") {
        override val tags = setOf(ManifestDoc)
    }

    object CHANGE_CONFIGURATION : APerm("android.permission.CHANGE_CONFIGURATION") {
        override val tags = setOf(ManifestDoc)
    }


    object DIAGNOSTIC : APerm("android.permission.DIAGNOSTIC") {
        override val tags = setOf(ManifestDoc)
    }

    object DISABLE_KEYGUARD : APerm("android.permission.DISABLE_KEYGUARD") {
        override val tags = setOf(ManifestDoc)
    }

    object DUMP : APerm("android.permission.DUMP") {
        override val tags = setOf(ManifestDoc)
    }

    object EXPAND_STATUS_BAR : APerm("android.permission.EXPAND_STATUS_BAR") {
        override val tags = setOf(ManifestDoc)
    }

    object FACTORY_TEST : APerm("android.permission.FACTORY_TEST") {
        override val tags = setOf(ManifestDoc)
    }

    object GET_ACCOUNTS_PRIVILEGED : APerm("android.permission.GET_ACCOUNTS_PRIVILEGED") {
        override val tags = setOf(ManifestDoc)
    }

    object GET_TASKS : APerm("android.permission.GET_TASKS") {
        override val tags = setOf(ManifestDoc)
    }

    object GLOBAL_SEARCH : APerm("android.permission.GLOBAL_SEARCH") {
        override val tags = setOf(ManifestDoc)
    }

    object HIDE_OVERLAY_WINDOWS : APerm("android.permission.HIDE_OVERLAY_WINDOWS") {
        override val tags = setOf(ManifestDoc)
    }

    object INSTALL_SHORTCUT : APerm("com.android.launcher.permission.INSTALL_SHORTCUT") {
        override val tags = setOf(ManifestDoc)
    }

    object INSTANT_APP_FOREGROUND_SERVICE : APerm("android.permission.INSTANT_APP_FOREGROUND_SERVICE") {
        override val tags = setOf(ManifestDoc)
    }

    object INTERACT_ACROSS_PROFILES : APerm("android.permission.INTERACT_ACROSS_PROFILES") {
        override val tags = setOf(ManifestDoc)
    }

    object KILL_BACKGROUND_PROCESSES : APerm("android.permission.KILL_BACKGROUND_PROCESSES") {
        override val tags = setOf(ManifestDoc)
    }

    object LOADER_USAGE_STATS : APerm("android.permission.LOADER_USAGE_STATS") {
        override val tags = setOf(ManifestDoc)
    }

    object MASTER_CLEAR : APerm("android.permission.MASTER_CLEAR") {
        override val tags = setOf(ManifestDoc)
    }

    object MEDIA_CONTENT_CONTROL : APerm("android.permission.MEDIA_CONTENT_CONTROL") {
        override val tags = setOf(ManifestDoc)
    }

    object PERSISTENT_ACTIVITY : APerm("android.permission.PERSISTENT_ACTIVITY") {
        override val tags = setOf(ManifestDoc)
    }

    object READ_INPUT_STATE : APerm("android.permission.READ_INPUT_STATE") {
        override val tags = setOf(ManifestDoc)
    }

    object READ_LOGS : APerm("android.permission.READ_LOGS") {
        override val tags = setOf(ManifestDoc)
    }

    object READ_SYNC_STATS : APerm("android.permission.READ_SYNC_STATS") {
        override val tags = setOf(ManifestDoc)
    }

    object RECEIVE_BOOT_COMPLETED : APerm("android.permission.RECEIVE_BOOT_COMPLETED") {
        override val tags = setOf(ManifestDoc)
    }

    object REORDER_TASKS : APerm("android.permission.REORDER_TASKS") {
        override val tags = setOf(ManifestDoc)
    }

    object REQUEST_COMPANION_PROFILE_WATCH : APerm("android.permission.REQUEST_COMPANION_PROFILE_WATCH") {
        override val tags = setOf(ManifestDoc)
    }

    object REQUEST_COMPANION_RUN_IN_BACKGROUND : APerm("android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND") {
        override val tags = setOf(ManifestDoc)
    }

    object REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND :
        APerm("android.permission.REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND") {
        override val tags = setOf(ManifestDoc)
    }

    object REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE :
        APerm("android.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE") {
        override val tags = setOf(ManifestDoc)
    }

    object REQUEST_PASSWORD_COMPLEXITY : APerm("android.permission.REQUEST_PASSWORD_COMPLEXITY") {
        override val tags = setOf(ManifestDoc)
    }

    object SET_ALARM : APerm("com.android.alarm.permission.SET_ALARM") {
        override val tags = setOf(ManifestDoc)
    }

    object SET_ALWAYS_FINISH : APerm("android.permission.SET_ALWAYS_FINISH") {
        override val tags = setOf(ManifestDoc)
    }

    object SET_ANIMATION_SCALE : APerm("android.permission.SET_ANIMATION_SCALE") {
        override val tags = setOf(ManifestDoc)
    }

    object SET_DEBUG_APP : APerm("android.permission.SET_DEBUG_APP") {
        override val tags = setOf(ManifestDoc)
    }

    object SET_PREFERRED_APPLICATIONS : APerm("android.permission.SET_PREFERRED_APPLICATIONS") {
        override val tags = setOf(ManifestDoc)
    }

    object SET_PROCESS_LIMIT : APerm("android.permission.SET_PROCESS_LIMIT") {
        override val tags = setOf(ManifestDoc)
    }

    object SET_TIME : APerm("android.permission.SET_TIME") {
        override val tags = setOf(ManifestDoc)
    }

    object SET_TIME_ZONE : APerm("android.permission.SET_TIME_ZONE") {
        override val tags = setOf(ManifestDoc)
    }

    object SET_WALLPAPER : APerm("android.permission.SET_WALLPAPER") {
        override val tags = setOf(ManifestDoc)
    }

    object SET_WALLPAPER_HINTS : APerm("android.permission.SET_WALLPAPER_HINTS") {
        override val tags = setOf(ManifestDoc)
    }

    object SIGNAL_PERSISTENT_PROCESSES : APerm("android.permission.SIGNAL_PERSISTENT_PROCESSES") {
        override val tags = setOf(ManifestDoc)
    }

    object START_FOREGROUND_SERVICES_FROM_BACKGROUND :
        APerm("android.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND") {
        override val tags = setOf(ManifestDoc)
    }

    object START_VIEW_PERMISSION_USAGE : APerm("android.permission.START_VIEW_PERMISSION_USAGE") {
        override val tags = setOf(ManifestDoc)
    }

    object STATUS_BAR : APerm("android.permission.STATUS_BAR") {
        override val tags = setOf(ManifestDoc)
    }

    object TRANSMIT_IR : APerm("android.permission.TRANSMIT_IR") {
        override val tags = setOf(ManifestDoc)
    }

    object UNINSTALL_SHORTCUT : APerm("com.android.launcher.permission.UNINSTALL_SHORTCUT") {
        override val tags = setOf(ManifestDoc)
    }

    object UPDATE_DEVICE_STATS : APerm("android.permission.UPDATE_DEVICE_STATS") {
        override val tags = setOf(ManifestDoc)
    }

    object USE_BIOMETRIC : APerm("android.permission.USE_BIOMETRIC") {
        override val tags = setOf(ManifestDoc)
    }

    object USE_FINGERPRINT : APerm("android.permission.USE_FINGERPRINT") {
        override val tags = setOf(ManifestDoc)
    }

    object USE_FULL_SCREEN_INTENT : APerm("android.permission.USE_FULL_SCREEN_INTENT") {
        override val tags = setOf(ManifestDoc)
    }

    object USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER : APerm("android.permission.USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER") {
        override val tags = setOf(ManifestDoc)
    }

    object USE_SIP : APerm("android.permission.USE_SIP") {
        override val tags = setOf(ManifestDoc)
    }

    object UWB_RANGING : APerm("android.permission.UWB_RANGING") {
        override val tags = setOf(ManifestDoc)
    }

    object WRITE_APN_SETTINGS : APerm("android.permission.WRITE_APN_SETTINGS") {
        override val tags = setOf(ManifestDoc)
    }

    object WRITE_GSERVICES : APerm("android.permission.WRITE_GSERVICES") {
        override val tags = setOf(ManifestDoc)
    }

    object WRITE_SECURE_SETTINGS : APerm("android.permission.WRITE_SECURE_SETTINGS") {
        override val tags = setOf(ManifestDoc)
    }

    object WRITE_SYNC_SETTINGS : APerm("android.permission.WRITE_SYNC_SETTINGS") {
        override val tags = setOf(ManifestDoc)
    }

    companion object {
        val values: List<APerm> by lazy {
            APerm::class.nestedClasses
                .filter { clazz -> clazz.isSubclassOf(APerm::class) }
                .map { clazz -> clazz.objectInstance }
                .filterIsInstance<APerm>()
        }
    }
}
