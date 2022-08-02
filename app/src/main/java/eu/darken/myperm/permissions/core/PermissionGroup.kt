package eu.darken.myperm.permissions.core

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.core.content.ContextCompat
import eu.darken.myperm.permissions.core.known.APermGrp
import kotlinx.parcelize.Parcelize

interface PermissionGroup {
    val id: Id

    fun getLabel(context: Context): String? {
        APermGrp.values.singleOrNull { it.id == id }
            ?.labelRes
            ?.let { return context.getString(it) }
        return null
    }

    fun getDescription(context: Context): String? {
        APermGrp.values.singleOrNull { it.id == id }
            ?.descriptionRes
            ?.let { return context.getString(it) }

        return null
    }

    fun getIcon(context: Context): Drawable? {
        APermGrp.values.singleOrNull { it.id == id }
            ?.iconRes
            ?.let { ContextCompat.getDrawable(context, it) }
            ?.let { return it }

        return null
    }

    @Parcelize
    data class Id(val value: String) : Parcelable

}

fun grpIds(vararg groups: PermissionGroup): Set<PermissionGroup.Id> = groups.map { it.id }.toSet()