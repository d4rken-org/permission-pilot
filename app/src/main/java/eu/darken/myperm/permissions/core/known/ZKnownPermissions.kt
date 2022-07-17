package eu.darken.myperm.permissions.core.known

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.Permission
import kotlin.reflect.full.isSubclassOf

@Keep
sealed class ZKnownPermissions constructor(override val id: Permission.Id) : Permission {

    @get:DrawableRes abstract val iconRes: Int?


    constructor(rawPmerissionId: String) : this(Permission.Id(rawPmerissionId))

    object INTERNET : ZKnownPermissions("android.permission.INTERNET") {
        override val iconRes: Int = R.drawable.ic_baseline_internet_24
    }

    object BOOT_COMPLETED : ZKnownPermissions("android.permission.RECEIVE_BOOT_COMPLETED") {
        override val iconRes: Int = R.drawable.ic_baseline_start_24
    }

    object WRITE_EXTERNAL_STORAGE : ZKnownPermissions("android.permission.WRITE_EXTERNAL_STORAGE") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
    }

    object READ_EXTERNAL_STORAGE : ZKnownPermissions("android.permission.READ_EXTERNAL_STORAGE") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
    }

    object WAKE_LOCK : ZKnownPermissions("android.permission.WAKE_LOCK") {
        override val iconRes: Int = R.drawable.ic_baseline_coffee_24
    }

    object VIBRATE : ZKnownPermissions("android.permission.VIBRATE") {
        override val iconRes: Int = R.drawable.ic_baseline_vibration_24
    }

    object CAMERA : ZKnownPermissions("android.permission.CAMERA") {
        override val iconRes: Int = R.drawable.ic_baseline_photo_camera_24
    }

    object RECORD_AUDIO : ZKnownPermissions("android.permission.RECORD_AUDIO") {
        override val iconRes: Int = R.drawable.ic_baseline_mic_24
    }

    object CONTACTS : ZKnownPermissions("android.permission.READ_CONTACTS") {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
    }

    object LOCATION_FINE : ZKnownPermissions("android.permission.ACCESS_FINE_LOCATION") {
        override val iconRes: Int = R.drawable.ic_baseline_location_on_24
    }

    object LOCATION_COARSE : ZKnownPermissions("android.permission.ACCESS_COARSE_LOCATION") {
        override val iconRes: Int = R.drawable.ic_baseline_location_on_24
    }

    object BLUETOOTH : ZKnownPermissions("android.permission.BLUETOOTH") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
    }

    object BLUETOOTH_ADMIN : ZKnownPermissions("android.permission.BLUETOOTH_ADMIN") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
    }

    object BLUETOOTH_CONNECT : ZKnownPermissions("android.permission.BLUETOOTH_CONNECT") {
        override val iconRes: Int = R.drawable.ic_baseline_bluetooth_24
    }

    object SMS_READ : ZKnownPermissions("android.permission.RECEIVE_SMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object SMS_RECEIVE : ZKnownPermissions("android.permission.SMS_RECEIVE") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object SMS_SEND : ZKnownPermissions("android.permission.SEND_SMS") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
    }

    object PHONE_CALL : ZKnownPermissions("android.permission.CALL_PHONE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
    }

    object PHONE_STATE : ZKnownPermissions("android.permission.PHONE_STATE") {
        override val iconRes: Int = R.drawable.ic_baseline_local_phone_24
    }

    companion object {
        fun values(): List<ZKnownPermissions> = ZKnownPermissions::class.nestedClasses
            .filter { clazz -> clazz.isSubclassOf(ZKnownPermissions::class) }
            .map { clazz -> clazz.objectInstance }
            .filterIsInstance<ZKnownPermissions>()
    }
}

fun Permission.Id.toKnownPermission(): Permission? =
    ZKnownPermissions.values().singleOrNull { it.id == this@toKnownPermission }