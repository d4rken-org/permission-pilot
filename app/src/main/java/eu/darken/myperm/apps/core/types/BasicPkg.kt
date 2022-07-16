package eu.darken.myperm.apps.core.types

import android.content.Context

class BasicPkg(
    override val id: Pkg.Id,
    val label: String?
) : Pkg {

    override fun getLabel(context: Context): String? {
        return label ?: super.getLabel(context)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BasicPkg) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()
}