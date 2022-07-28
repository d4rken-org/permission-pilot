package eu.darken.myperm.permissions.core.known

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionGroup
import kotlin.reflect.full.isSubclassOf

@Keep
sealed class APermGrp constructor(override val id: PermissionGroup.Id) : PermissionGroup {

    @get:DrawableRes open val iconRes: Int? = null
    @get:StringRes open val labelRes: Int? = null
    @get:StringRes open val descriptionRes: Int? = null

    override val permissionIds: Collection<Permission.Id>
        get() = emptyList()

    constructor(rawPermissionId: String) : this(PermissionGroup.Id(rawPermissionId))

    override fun toString(): String = "APermGrp(${id})"

    object Calendar : APermGrp("permission.group.CALENDAR") {
        override val iconRes: Int = R.drawable.ic_baseline_calendar_today_24
        override val labelRes: Int = R.string.permission_group_calendar_label
        override val descriptionRes: Int = R.string.permission_group_calendar_description

        override val permissionIds: Collection<Permission.Id> = setOf(
            APerm.READ_CALENDAR.id,
            APerm.WRITE_CALENDAR.id,
        )
    }

    object Contacts : APermGrp("permission.group.CONTACTS")

    object Storage : APermGrp("permission.group.STORAGE") {
        override val iconRes: Int = R.drawable.ic_baseline_sd_storage_24
        override val labelRes: Int = R.string.permission_group_storage_label
        override val descriptionRes: Int = R.string.permission_group_storage_description

        override val permissionIds: Collection<Permission.Id> = setOf(
            APerm.MANAGE_EXTERNAL_STORAGE.id,
            APerm.WRITE_EXTERNAL_STORAGE.id,
            APerm.READ_EXTERNAL_STORAGE.id,
            APerm.READ_MEDIA_STORAGE.id,
            APerm.WRITE_MEDIA_STORAGE.id,
        )
    }

    object AppInteraction : APermGrp("permission.group.APP_INTERACTION")

    object Microphone : APermGrp("permission.group.MICROPHONE")

    object Location : APermGrp("permission.group.LOCATION")

    object Calls : APermGrp("permission.group.CALLS")

    object Sensors : APermGrp("permission.group.SENSORS")

    object Messages : APermGrp("permission.group.MESSAGES")

    object Connectivity : APermGrp("permission.group.CONNECTIVITY")

    object Other : APermGrp("permission.group.OTHER")

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

fun Permission.getGroup(): APermGrp? =
    APermGrp.values.singleOrNull { it.permissionIds.contains(this.id) }
