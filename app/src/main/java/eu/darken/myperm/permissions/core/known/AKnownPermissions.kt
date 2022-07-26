package eu.darken.myperm.permissions.core.known

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionAction
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

    object WRITE_EXTERNAL_STORAGE : AKnownPermissions("android.permission.WRITE_EXTERNAL_STORAGE") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
    }

    object READ_EXTERNAL_STORAGE : AKnownPermissions("android.permission.READ_EXTERNAL_STORAGE") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
    }

    object MANAGE_EXTERNAL_STORAGE : AKnownPermissions("android.permission.MANAGE_EXTERNAL_STORAGE") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
    }

    object WAKE_LOCK : AKnownPermissions("android.permission.WAKE_LOCK") {
        override val iconRes: Int = R.drawable.ic_baseline_coffee_24
    }

    object VIBRATE : AKnownPermissions("android.permission.VIBRATE") {
        override val iconRes: Int = R.drawable.ic_baseline_vibration_24
    }

    object CAMERA : AKnownPermissions("android.permission.CAMERA") {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
    }

    object RECORD_AUDIO : AKnownPermissions("android.permission.RECORD_AUDIO") {
        override val iconRes: Int = R.drawable.ic_baseline_mic_24
    }

    object CONTACTS : AKnownPermissions("android.permission.READ_CONTACTS") {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
    }

    object LOCATION_FINE : AKnownPermissions("android.permission.ACCESS_FINE_LOCATION") {
        override val iconRes: Int = R.drawable.ic_location_fine_24
    }

    object LOCATION_COARSE : AKnownPermissions("android.permission.ACCESS_COARSE_LOCATION") {
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

    object SMS_READ : AKnownPermissions("android.permission.RECEIVE_SMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object SMS_RECEIVE : AKnownPermissions("android.permission.SMS_RECEIVE") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object SMS_SEND : AKnownPermissions("android.permission.SEND_SMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object PHONE_CALL : AKnownPermissions("android.permission.CALL_PHONE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
    }

    object PHONE_STATE : AKnownPermissions("android.permission.PHONE_STATE") {
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

    object REBOOT : AKnownPermissions("android.permission.REBOOT") {
        override val iconRes: Int = R.drawable.ic_reboot_permission_24
        override val labelRes: Int = R.string.permission_reboot_label
        override val descriptionRes: Int = R.string.permission_reboot_description
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