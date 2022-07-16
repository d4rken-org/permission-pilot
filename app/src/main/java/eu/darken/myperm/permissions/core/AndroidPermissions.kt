package eu.darken.myperm.permissions.core

import androidx.annotation.DrawableRes
import eu.darken.myperm.R

enum class AndroidPermissions(
    override val id: Permission.Id,
    @DrawableRes val iconRes: Int
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
    ;
}