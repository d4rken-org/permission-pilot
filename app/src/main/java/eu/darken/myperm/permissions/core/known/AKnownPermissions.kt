package eu.darken.myperm.permissions.core.known

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionAction
import eu.darken.myperm.permissions.core.features.Highlighted
import kotlin.reflect.full.isSubclassOf

@Keep
sealed class AKnownPermissions constructor(override val id: Permission.Id) : Permission {

    @get:DrawableRes open val iconRes: Int? = null
    @get:StringRes open val labelRes: Int? = null
    @get:StringRes open val descriptionRes: Int? = null

    constructor(rawPermissionId: String) : this(Permission.Id(rawPermissionId))

    override fun getAction(context: Context): PermissionAction = PermissionAction.None(this)

    object INTERNET : AKnownPermissions("android.permission.INTERNET") {
        override val iconRes: Int = R.drawable.ic_baseline_internet_24
    }

    object BOOT_COMPLETED : AKnownPermissions("android.permission.RECEIVE_BOOT_COMPLETED") {
        override val iconRes: Int = R.drawable.ic_baseline_start_24
    }

    object WRITE_EXTERNAL_STORAGE : AKnownPermissions("android.permission.WRITE_EXTERNAL_STORAGE"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
    }

    object READ_EXTERNAL_STORAGE : AKnownPermissions("android.permission.READ_EXTERNAL_STORAGE"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
    }

    object MANAGE_EXTERNAL_STORAGE : AKnownPermissions("android.permission.MANAGE_EXTERNAL_STORAGE"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
    }

    object WAKE_LOCK : AKnownPermissions("android.permission.WAKE_LOCK") {
        override val iconRes: Int = R.drawable.ic_baseline_coffee_24
    }

    object VIBRATE : AKnownPermissions("android.permission.VIBRATE") {
        override val iconRes: Int = R.drawable.ic_baseline_vibration_24
    }

    object CAMERA : AKnownPermissions("android.permission.CAMERA"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
    }

    object RECORD_AUDIO : AKnownPermissions("android.permission.RECORD_AUDIO"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_mic_24
    }

    object READ_CONTACTS : AKnownPermissions("android.permission.READ_CONTACTS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
    }

    object WRITE_CONTACTS : AKnownPermissions("android.permission.WRITE_CONTACTS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
    }

    object LOCATION_FINE : AKnownPermissions("android.permission.ACCESS_FINE_LOCATION"), Highlighted {
        override val iconRes: Int = R.drawable.ic_location_fine_24
    }

    object LOCATION_COARSE : AKnownPermissions("android.permission.ACCESS_COARSE_LOCATION"), Highlighted {
        override val iconRes: Int = R.drawable.ic_location_coarse_24
    }

    object BLUETOOTH : AKnownPermissions("android.permission.BLUETOOTH") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
    }

    object BLUETOOTH_ADMIN : AKnownPermissions("android.permission.BLUETOOTH_ADMIN") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
    }

    object BLUETOOTH_CONNECT : AKnownPermissions("android.permission.BLUETOOTH_CONNECT") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
    }

    object BLUETOOTH_SCAN : AKnownPermissions("android.permission.BLUETOOTH_SCAN"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
    }

    object SMS_READ : AKnownPermissions("android.permission.RECEIVE_SMS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object SMS_RECEIVE : AKnownPermissions("android.permission.SMS_RECEIVE"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object SMS_SEND : AKnownPermissions("android.permission.SEND_SMS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object READ_SMS : AKnownPermissions("android.permission.READ_SMS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object RECEIVE_WAP_PUSH : AKnownPermissions("android.permission.RECEIVE_WAP_PUSH") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object RECEIVE_MMS : AKnownPermissions("android.permission.RECEIVE_MMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object PHONE_CALL : AKnownPermissions("android.permission.CALL_PHONE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
    }

    object ANSWER_PHONE_CALLS : AKnownPermissions("android.permission.ANSWER_PHONE_CALLS") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
    }

    object PHONE_STATE : AKnownPermissions("android.permission.PHONE_STATE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
    }

    object READ_PHONE_STATE : AKnownPermissions("android.permission.READ_PHONE_STATE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
    }

    object READ_PHONE_NUMBERS : AKnownPermissions("android.permission.READ_PHONE_NUMBERS") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
    }

    object NETWORK_STATE : AKnownPermissions("android.permission.ACCESS_NETWORK_STATE") {
        override val iconRes: Int = R.drawable.ic_network_state_24
    }

    object WIFI_STATE : AKnownPermissions("android.permission.ACCESS_WIFI_STATE") {
        override val iconRes: Int = R.drawable.ic_wifi_state_24
    }

    object QUERY_ALL_PACKAGES : AKnownPermissions("android.permission.QUERY_ALL_PACKAGES") {
        override val iconRes: Int = R.drawable.ic_query_all_packages_24
    }

    object FOREGROUND_SERVICE : AKnownPermissions("android.permission.FOREGROUND_SERVICE") {
        override val iconRes: Int = R.drawable.ic_foreground_service_24
    }

    object REBOOT : AKnownPermissions("android.permission.REBOOT") {
        override val iconRes: Int = R.drawable.ic_reboot_permission_24
        override val labelRes: Int = R.string.permission_reboot_label
        override val descriptionRes: Int = R.string.permission_reboot_description
    }

    object BODY_SENSORS : AKnownPermissions("android.permission.BODY_SENSORS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_body_sensors_24
    }

    object BODY_SENSORS_BACKGROUND : AKnownPermissions("android.permission.BODY_SENSORS_BACKGROUND"), Highlighted {
        override val iconRes: Int = R.drawable.ic_body_sensors_24
    }

    object READ_CALENDAR : AKnownPermissions("android.permission.READ_CALENDAR"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_calendar_today_24
    }

    object WRITE_CALENDAR : AKnownPermissions("android.permission.WRITE_CALENDAR"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_edit_calendar_24
    }

    object READ_CALL_LOG : AKnownPermissions("android.permission.READ_CALL_LOG"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
    }

    object WRITE_CALL_LOG : AKnownPermissions("android.permission.WRITE_CALL_LOG"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
    }

    object ACTIVITY_RECOGNITION : AKnownPermissions("android.permission.ACTIVITY_RECOGNITION"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_directions_run_24
    }

    object SYSTEM_ALERT_WINDOW : AKnownPermissions("android.permission.SYSTEM_ALERT_WINDOW"), Highlighted {
        override val iconRes: Int = R.drawable.ic_system_alert_window_24
    }

    object NFC : AKnownPermissions("android.permission.NFC") {
        override val iconRes: Int = R.drawable.ic_nfc_24
        override val labelRes: Int = R.string.permission_nfc_label
        override val descriptionRes: Int = R.string.permission_nfc_description
    }

    object MANAGE_MEDIA : AKnownPermissions("android.permission.WRITE_MEDIA_STORAGE") {
        override val iconRes: Int = R.drawable.ic_manage_media_24
        override val labelRes: Int = R.string.permission_write_media_storage_label
        override val descriptionRes: Int = R.string.permission_write_media_storage_description
    }

    object USAGE_DATA_ACCESS : AKnownPermissions("android.permission.PACKAGE_USAGE_STATS") {
        override val iconRes: Int = R.drawable.ic_usage_data_access_24
        override val labelRes: Int = R.string.permission_package_usage_stats_label
        override val descriptionRes: Int = R.string.permission_package_usage_stats_description
    }
    
    object MODIFY_SYSTEM_SETTINGS : AKnownPermissions("android.permission.WRITE_SETTINGS") {
        override val iconRes: Int = R.drawable.ic_modify_system_settings_24
        override val labelRes: Int = R.string.permission_write_settings_label
        override val descriptionRes: Int = R.string.permission_write_settings_description
    }

    object BLUETOOTH_ADVERTISE : AKnownPermissions("android.permission.BLUETOOTH_ADVERTISE") {
        override val iconRes: Int = R.drawable.ic_bluetooth_advertise_24
        override val labelRes: Int = R.string.permission_bluetooth_advertise_label
        override val descriptionRes: Int = R.string.permission_bluetooth_advertise_description
    }

    object GET_ACCOUNTS : AKnownPermissions("android.permission.GET_ACCOUNTS") {
        override val iconRes: Int = R.drawable.ic_get_accounts_24
        override val labelRes: Int = R.string.permission_get_accounts_label
        override val descriptionRes: Int = R.string.permission_get_accounts_description
    }

    object ACCESS_BACKGROUND_LOCATION : AKnownPermissions("android.permission.ACCESS_BACKGROUND_LOCATION") {
        override val iconRes: Int = R.drawable.ic_access_background_location_24
        override val labelRes: Int = R.string.permission_access_background_location_label
        override val descriptionRes: Int = R.string.permission_access_background_location_description
    }

    object ACCESS_MEDIA_LOCATION : AKnownPermissions("android.permission.ACCESS_MEDIA_LOCATION") {
        override val iconRes: Int = R.drawable.ic_access_media_location_24
        override val labelRes: Int = R.string.permission_access_media_location_label
        override val descriptionRes: Int = R.string.permission_access_media_location_description
    }

    object MANAGE_ACCOUNTS : AKnownPermissions("android.permission.MANAGE_ACCOUNTS") {
        override val iconRes: Int = R.drawable.ic_manage_accounts_24
        override val labelRes: Int = R.string.permission_manage_accounts_label
        override val descriptionRes: Int = R.string.permission_manage_accounts_description
    }

    companion object {
        val values: List<AKnownPermissions> by lazy {
            AKnownPermissions::class.nestedClasses
                .filter { clazz -> clazz.isSubclassOf(AKnownPermissions::class) }
                .map { clazz -> clazz.objectInstance }
                .filterIsInstance<AKnownPermissions>()
        }
    }
}

fun Permission.Id.toKnownPermission(): AKnownPermissions? =
    AKnownPermissions.values.singleOrNull { it.id == this@toKnownPermission }
