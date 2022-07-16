package eu.darken.myperm.permissions.core

import androidx.annotation.DrawableRes
import eu.darken.myperm.R

enum class AndroidPermissions(
    override val id: Permission.Id,
    val label: String? = null,
    @DrawableRes val iconRes: Int,
) : Permission {

    INTERNET(
        id = Permission.Id("android.permission.INTERNET"),
        iconRes = R.drawable.ic_baseline_internet_24,
    ),
    BOOT_COMPLETED(
        id = Permission.Id("android.permission.RECEIVE_BOOT_COMPLETED"),
        iconRes = R.drawable.ic_baseline_start_24,
    ),
    WRITE_EXTERNAL_STORAGE(
        id = Permission.Id("android.permission.WRITE_EXTERNAL_STORAGE"),
        iconRes = R.drawable.ic_baseline_sd_storage_24,
    ),
    READ_EXTERNAL_STORAGE(
        id = Permission.Id("android.permission.READ_EXTERNAL_STORAGE"),
        iconRes = R.drawable.ic_baseline_sd_storage_24,
    ),
    WAKE_LOCK(
        id = Permission.Id("android.permission.WAKE_LOCK"),
        iconRes = R.drawable.ic_baseline_coffee_24,
    ),
    VIBRATE(
        id = Permission.Id("android.permission.VIBRATE"),
        iconRes = R.drawable.ic_baseline_vibration_24,
    ),
    CAMERA(
        id = Permission.Id("android.permission.CAMERA"),
        iconRes = R.drawable.ic_baseline_photo_camera_24,
    ),
    RECORD_AUDIO(
        id = Permission.Id("android.permission.RECORD_AUDIO"),
        iconRes = R.drawable.ic_baseline_mic_24,
    ),
    CONTACTS(
        id = Permission.Id("android.permission.READ_CONTACTS"),
        iconRes = R.drawable.ic_baseline_contacts_24,
    ),
    LOCATION_FINE(
        id = Permission.Id("android.permission.ACCESS_FINE_LOCATION"),
        iconRes = R.drawable.ic_baseline_location_on_24,
    ),
    LOCATION_COARSE(
        id = Permission.Id("android.permission.ACCESS_COARSE_LOCATION"),
        iconRes = R.drawable.ic_baseline_location_on_24,
    ),
    BLUETOOTH(
        id = Permission.Id("android.permission.BLUETOOTH"),
        iconRes = R.drawable.ic_baseline_bluetooth_24,
    ),
    BLUETOOTH_ADMIN(
        id = Permission.Id("android.permission.BLUETOOTH_ADMIN"),
        iconRes = R.drawable.ic_baseline_bluetooth_24,
    ),
    BLUETOOTH_CONNECT(
        id = Permission.Id("android.permission.BLUETOOTH_CONNECT"),
        iconRes = R.drawable.ic_baseline_bluetooth_24,
    ),
    SMS_READ(
        id = Permission.Id("android.permission.RECEIVE_SMS"),
        iconRes = R.drawable.ic_baseline_sms_24,
    ),
    SMS_RECEIVE(
        id = Permission.Id("android.permission.SMS_RECEIVE"),
        iconRes = R.drawable.ic_baseline_sms_24,
    ),
    SMS_SEND(
        id = Permission.Id("android.permission.SEND_SMS"),
        iconRes = R.drawable.ic_baseline_sms_24,
    ),
    PHONE_CALL(
        id = Permission.Id("android.permission.CALL_PHONE"),
        iconRes = R.drawable.ic_baseline_local_phone_24,
    ),
    PHONE_STATE(
        id = Permission.Id("android.permission.PHONE_STATE"),
        iconRes = R.drawable.ic_baseline_local_phone_24,
    ),
    ;
}