package eu.darken.myperm.permissions.core.known

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.PermissionGroup
import eu.darken.myperm.permissions.core.features.NotNormalPerm
import eu.darken.myperm.permissions.core.features.PermissionTag
import eu.darken.myperm.permissions.core.grpIds
import kotlin.reflect.full.isSubclassOf

sealed class AExtraPerm constructor(val id: Permission.Id) {

    @get:DrawableRes open val iconRes: Int? = null
    @get:StringRes open val labelRes: Int? = null
    @get:StringRes open val descriptionRes: Int? = null

    open val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Other)
    open val tags: Collection<PermissionTag> = emptySet()

    constructor(rawPermissionId: String) : this(Permission.Id(rawPermissionId))

    object PICTURE_IN_PICTURE : AExtraPerm("android:picture_in_picture") {
        override val iconRes: Int = R.drawable.ic_baseline_picture_in_picture_24
        override val labelRes: Int = R.string.permission_picture_in_picture_label
        override val groupIds: Set<PermissionGroup.Id> = grpIds(APermGrp.Other)
        override val tags = setOf(NotNormalPerm)
    }


    companion object {
        val values: List<AExtraPerm> by lazy {
            AExtraPerm::class.nestedClasses
                .filter { clazz -> clazz.isSubclassOf(AExtraPerm::class) }
                .map { clazz -> clazz.objectInstance }
                .filterIsInstance<AExtraPerm>()
        }
    }
}
