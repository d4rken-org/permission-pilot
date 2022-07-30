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

    object INTERNET : APerm("android.permission.INTERNET") {
        override val iconRes: Int = R.drawable.ic_baseline_internet_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object BOOT_COMPLETED : APerm("android.permission.RECEIVE_BOOT_COMPLETED") {
        override val iconRes: Int = R.drawable.ic_baseline_start_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
    }

    object WRITE_MEDIA_STORAGE : APerm("android.permission.WRITE_MEDIA_STORAGE"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Storage)
    }

    object READ_MEDIA_STORAGE : APerm("android.permission.READ_MEDIA_STORAGE"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Storage)
    }

    object WRITE_EXTERNAL_STORAGE : APerm("android.permission.WRITE_EXTERNAL_STORAGE"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Storage)
    }

    object READ_EXTERNAL_STORAGE : APerm("android.permission.READ_EXTERNAL_STORAGE"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Storage)
    }

    object MANAGE_EXTERNAL_STORAGE : APerm("android.permission.MANAGE_EXTERNAL_STORAGE"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Storage)
    }

    object MANAGE_MEDIA : APerm("android.permission.WRITE_MEDIA_STORAGE") {
        override val iconRes: Int = R.drawable.ic_manage_media_24
        override val labelRes: Int = R.string.permission_write_media_storage_label
        override val descriptionRes: Int = R.string.permission_write_media_storage_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Storage)
    }

    object ACCESS_MEDIA_LOCATION : APerm("android.permission.ACCESS_MEDIA_LOCATION") {
        override val iconRes: Int = R.drawable.ic_access_media_location_24
        override val labelRes: Int = R.string.permission_access_media_location_label
        override val descriptionRes: Int = R.string.permission_access_media_location_description
    }

    object VIBRATE : APerm("android.permission.VIBRATE") {
        override val iconRes: Int = R.drawable.ic_baseline_vibration_24
    }

    object CAMERA : APerm("android.permission.CAMERA"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object RECORD_AUDIO : APerm("android.permission.RECORD_AUDIO"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_mic_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object READ_CONTACTS : APerm("android.permission.READ_CONTACTS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Contacts)
    }

    object WRITE_CONTACTS : APerm("android.permission.WRITE_CONTACTS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Contacts)
    }

    object LOCATION_FINE : APerm("android.permission.ACCESS_FINE_LOCATION"), Highlighted {
        override val iconRes: Int = R.drawable.ic_location_fine_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object LOCATION_COARSE : APerm("android.permission.ACCESS_COARSE_LOCATION"), Highlighted {
        override val iconRes: Int = R.drawable.ic_location_coarse_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object ACCESS_BACKGROUND_LOCATION : APerm("android.permission.ACCESS_BACKGROUND_LOCATION") {
        override val iconRes: Int = R.drawable.ic_access_background_location_24
        override val labelRes: Int = R.string.permission_access_background_location_label
        override val descriptionRes: Int = R.string.permission_access_background_location_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Location)
    }

    object BLUETOOTH : APerm("android.permission.BLUETOOTH") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity, APermGrp.Location)
    }

    object BLUETOOTH_ADMIN : APerm("android.permission.BLUETOOTH_ADMIN") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity, APermGrp.Connectivity)
    }

    object BLUETOOTH_CONNECT : APerm("android.permission.BLUETOOTH_CONNECT") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object BLUETOOTH_SCAN : APerm("android.permission.BLUETOOTH_SCAN"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity, APermGrp.Location)
    }

    object BLUETOOTH_ADVERTISE : APerm("android.permission.BLUETOOTH_ADVERTISE") {
        override val iconRes: Int = R.drawable.ic_bluetooth_advertise_24
        override val labelRes: Int = R.string.permission_bluetooth_advertise_label
        override val descriptionRes: Int = R.string.permission_bluetooth_advertise_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object SMS_READ : APerm("android.permission.RECEIVE_SMS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messages)
    }

    object SMS_RECEIVE : APerm("android.permission.SMS_RECEIVE"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messages)
    }

    object SMS_SEND : APerm("android.permission.SEND_SMS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messages)
    }

    object READ_SMS : APerm("android.permission.READ_SMS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messages)
    }

    object RECEIVE_WAP_PUSH : APerm("android.permission.RECEIVE_WAP_PUSH") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messages)
    }

    object RECEIVE_MMS : APerm("android.permission.RECEIVE_MMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Messages)
    }

    object PHONE_CALL : APerm("android.permission.CALL_PHONE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object ANSWER_PHONE_CALLS : APerm("android.permission.ANSWER_PHONE_CALLS") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object PHONE_STATE : APerm("android.permission.PHONE_STATE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_PHONE_STATE : APerm("android.permission.READ_PHONE_STATE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_PHONE_NUMBERS : APerm("android.permission.READ_PHONE_NUMBERS") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object READ_CALL_LOG : APerm("android.permission.READ_CALL_LOG"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object WRITE_CALL_LOG : APerm("android.permission.WRITE_CALL_LOG"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calls)
    }

    object NETWORK_STATE : APerm("android.permission.ACCESS_NETWORK_STATE") {
        override val iconRes: Int = R.drawable.ic_network_state_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object WIFI_STATE : APerm("android.permission.ACCESS_WIFI_STATE") {
        override val iconRes: Int = R.drawable.ic_wifi_state_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object CHANGE_WIFI_STATE : APerm("android.permission.CHANGE_WIFI_STATE") {
        override val iconRes: Int = R.drawable.ic_change_wifi_state_24
        override val labelRes: Int = R.string.permission_change_wifi_state_label
        override val descriptionRes: Int = R.string.permission_change_wifi_state_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object CHANGE_NETWORK_STATE : APerm("android.permission.CHANGE_NETWORK_STATE") {
        override val iconRes: Int = R.drawable.ic_change_network_state_24
        override val labelRes: Int = R.string.permission_change_network_state_label
        override val descriptionRes: Int = R.string.permission_change_network_state_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object QUERY_ALL_PACKAGES : APerm("android.permission.QUERY_ALL_PACKAGES") {
        override val iconRes: Int = R.drawable.ic_query_all_packages_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
    }

    object FOREGROUND_SERVICE : APerm("android.permission.FOREGROUND_SERVICE") {
        override val iconRes: Int = R.drawable.ic_foreground_service_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
    }

    object REBOOT : APerm("android.permission.REBOOT") {
        override val iconRes: Int = R.drawable.ic_reboot_permission_24
        override val labelRes: Int = R.string.permission_reboot_label
        override val descriptionRes: Int = R.string.permission_reboot_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
    }

    object BODY_SENSORS : APerm("android.permission.BODY_SENSORS"), Highlighted {
        override val iconRes: Int = R.drawable.ic_body_sensors_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object BODY_SENSORS_BACKGROUND : APerm("android.permission.BODY_SENSORS_BACKGROUND"), Highlighted {
        override val iconRes: Int = R.drawable.ic_body_sensors_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object READ_CALENDAR : APerm("android.permission.READ_CALENDAR"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_calendar_today_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calendar)
    }

    object WRITE_CALENDAR : APerm("android.permission.WRITE_CALENDAR"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_edit_calendar_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Calendar)
    }

    object ACTIVITY_RECOGNITION : APerm("android.permission.ACTIVITY_RECOGNITION"), Highlighted {
        override val iconRes: Int = R.drawable.ic_baseline_directions_run_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Sensors)
    }

    object SYSTEM_ALERT_WINDOW : APerm("android.permission.SYSTEM_ALERT_WINDOW"), Highlighted {
        override val iconRes: Int = R.drawable.ic_system_alert_window_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
    }

    object NFC : APerm("android.permission.NFC") {
        override val iconRes: Int = R.drawable.ic_nfc_24
        override val labelRes: Int = R.string.permission_nfc_label
        override val descriptionRes: Int = R.string.permission_nfc_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Connectivity)
    }

    object USAGE_DATA_ACCESS : APerm("android.permission.PACKAGE_USAGE_STATS") {
        override val iconRes: Int = R.drawable.ic_usage_data_access_24
        override val labelRes: Int = R.string.permission_package_usage_stats_label
        override val descriptionRes: Int = R.string.permission_package_usage_stats_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
    }

    object MODIFY_SYSTEM_SETTINGS : APerm("android.permission.WRITE_SETTINGS") {
        override val iconRes: Int = R.drawable.ic_modify_system_settings_24
        override val labelRes: Int = R.string.permission_write_settings_label
        override val descriptionRes: Int = R.string.permission_write_settings_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
    }

    object GET_ACCOUNTS : APerm("android.permission.GET_ACCOUNTS") {
        override val iconRes: Int = R.drawable.ic_get_accounts_24
        override val labelRes: Int = R.string.permission_get_accounts_label
        override val descriptionRes: Int = R.string.permission_get_accounts_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
    }

    object MANAGE_ACCOUNTS : APerm("android.permission.MANAGE_ACCOUNTS") {
        override val iconRes: Int = R.drawable.ic_manage_accounts_24
        override val labelRes: Int = R.string.permission_manage_accounts_label
        override val descriptionRes: Int = R.string.permission_manage_accounts_description
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
    }

    object READ_SYNC_SETTINGS : APerm("android.permission.READ_SYNC_SETTINGS") {
        override val iconRes: Int = R.drawable.ic_read_sync_settings_24
        override val labelRes: Int = R.string.permission_read_sync_settings_label
        override val descriptionRes: Int = R.string.permission_read_sync_settings_description
    }

    object WAKE_LOCK : APerm(Manifest.permission.WAKE_LOCK) {
        override val iconRes: Int = R.drawable.ic_baseline_coffee_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
    }

    object REQUEST_IGNORE_BATTERY_OPTIMIZATIONS : APerm("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS") {
        override val iconRes: Int = R.drawable.ic_baseline_battery_charging_full_24
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.AppInteraction)
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

private fun grpIds(vararg groups: PermissionGroup): Set<PermissionGroup.Id> = groups.map { it.id }.toSet()

fun Permission.Id.toKnownPermission(): APerm? =
    APerm.values.singleOrNull { it.id == this@toKnownPermission }

fun Permission.getGroup(): Collection<APermGrp> =
    (this as HasGroup?)?.groupIds?.map { grpId -> APermGrp.values.single { it.id == grpId } } ?: emptySet()