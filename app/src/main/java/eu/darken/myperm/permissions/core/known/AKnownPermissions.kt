package eu.darken.myperm.permissions.core.known

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.Permission
import kotlin.reflect.full.isSubclassOf

@Keep
sealed class AKnownPermissions constructor(override val id: Permission.Id) : Permission {

    @get:DrawableRes abstract val iconRes: Int?

    constructor(rawPmerissionId: String) : this(Permission.Id(rawPmerissionId))

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
        override val iconRes: Int = R.drawable.ic_baseline_location_on_24
    }

    object LOCATION_COARSE : AKnownPermissions("android.permission.ACCESS_COARSE_LOCATION") {
        override val iconRes: Int = R.drawable.ic_baseline_location_on_24
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

    companion object {
        fun values(): List<AKnownPermissions> = AKnownPermissions::class.nestedClasses
            .filter { clazz -> clazz.isSubclassOf(AKnownPermissions::class) }
            .map { clazz -> clazz.objectInstance }
            .filterIsInstance<AKnownPermissions>()
    }
}

fun Permission.Id.toKnownPermission(): Permission? =
    AKnownPermissions.values().singleOrNull { it.id == this@toKnownPermission }