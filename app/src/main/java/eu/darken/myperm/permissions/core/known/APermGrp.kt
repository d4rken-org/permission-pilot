package eu.darken.myperm.permissions.core.known

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.PermissionGroup
import kotlin.reflect.full.isSubclassOf

@Keep
sealed class APermGrp constructor(override val id: PermissionGroup.Id) : PermissionGroup {

    @get:DrawableRes abstract val iconRes: Int
    @get:StringRes abstract val labelRes: Int
    @get:StringRes abstract val descriptionRes: Int

    constructor(rawPermissionId: String) : this(PermissionGroup.Id(rawPermissionId))

    override fun toString(): String = "APermGrp(${id})"

    object Camera : APermGrp("permission.group.CAMERA") {
        override val iconRes: Int = R.drawable.ic_baseline_camera_24
        override val labelRes: Int = R.string.permission_group_camera_label
        override val descriptionRes: Int = R.string.permission_group_camera_description
    }

    object Audio : APermGrp("permission.group.AUDIO") {
        override val iconRes: Int = R.drawable.ic_baseline_mic_24
        override val labelRes: Int = R.string.permission_group_audio_label
        override val descriptionRes: Int = R.string.permission_group_audio_description
    }

    object Calendar : APermGrp("permission.group.CALENDAR") {
        override val iconRes: Int = R.drawable.ic_baseline_calendar_today_24
        override val labelRes: Int = R.string.permission_group_calendar_label
        override val descriptionRes: Int = R.string.permission_group_calendar_description
    }

    object Contacts : APermGrp("permission.group.CONTACTS") {
        override val iconRes: Int = R.drawable.ic_baseline_contacts_24
        override val labelRes: Int = R.string.permission_group_contacts_label
        override val descriptionRes: Int = R.string.permission_group_contacts_description
    }

    object Files : APermGrp("permission.group.FILES") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val labelRes: Int = R.string.permission_group_files_label
        override val descriptionRes: Int = R.string.permission_group_files_description
    }

    object Apps : APermGrp("permission.group.APPS") {
        override val iconRes: Int = R.drawable.ic_baseline_apps_24
        override val labelRes: Int = R.string.permission_group_apps_label
        override val descriptionRes: Int = R.string.permission_group_apps_description
    }

    object Location : APermGrp("permission.group.LOCATION") {
        override val iconRes: Int = R.drawable.ic_location_fine_24
        override val labelRes: Int = R.string.permission_group_location_label
        override val descriptionRes: Int = R.string.permission_group_location_description
    }

    object Calls : APermGrp("permission.group.CALLS") {
        override val iconRes: Int = R.drawable.ic_baseline_call_24
        override val labelRes: Int = R.string.permission_group_calls_label
        override val descriptionRes: Int = R.string.permission_group_calls_description
    }

    object Sensors : APermGrp("permission.group.SENSORS") {
        override val iconRes: Int = R.drawable.ic_baseline_sensors_24
        override val labelRes: Int = R.string.permission_group_sensors_label
        override val descriptionRes: Int = R.string.permission_group_sensors_description
    }

    object Messaging : APermGrp("permission.group.MESSAGING") {
        override val iconRes: Int = R.drawable.ic_baseline_sms_24
        override val labelRes: Int = R.string.permission_group_messaging_label
        override val descriptionRes: Int = R.string.permission_group_messaging_description
    }

    object Connectivity : APermGrp("permission.group.CONNECTIVITY") {
        override val iconRes: Int = R.drawable.ic_connectivity_24
        override val labelRes: Int = R.string.permission_group_connectivity_label
        override val descriptionRes: Int = R.string.permission_group_connectivity_description
    }

    object Other : APermGrp("permission.group.OTHER") {
        override val iconRes: Int = R.drawable.ic_baseline_more_24
        override val labelRes: Int = R.string.permission_group_other_label
        override val descriptionRes: Int = R.string.permission_group_other_description
    }

    companion object {
        val values: List<APermGrp> by lazy {
            APermGrp::class.nestedClasses
                .filter { clazz -> clazz.isSubclassOf(APermGrp::class) }
                .map { clazz -> clazz.objectInstance }
                .filterIsInstance<APermGrp>()
        }
    }
}

fun PermissionGroup.Id.toKnownGroup(): APermGrp? =
    APermGrp.values.singleOrNull { it.id == this@toKnownGroup }
