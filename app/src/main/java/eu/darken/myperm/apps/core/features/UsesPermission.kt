package eu.darken.myperm.apps.core.features

import android.content.pm.PackageInfo
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.permissions.core.Permission

sealed class UsesPermission {
    abstract val id: Permission.Id
    abstract val status: Status

    enum class Status {
        GRANTED,
        GRANTED_IN_USE,
        DENIED,
        UNKNOWN,
    }

    class WithState(
        override val id: Permission.Id,
        val flags: Int?,
        private val overrideStatus: Status? = null
    ) : UsesPermission() {

        override val status: Status = overrideStatus ?: when {
            flags == null -> Status.UNKNOWN
            flags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 -> Status.GRANTED
            else -> Status.DENIED
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is WithState) return false
            return id == other.id && flags == other.flags
        }

        override fun hashCode(): Int = 31 * id.hashCode() + (flags ?: 0)

        override fun toString(): String = "WithState(id=$id, flags=$flags, status=$status)"
    }

    data class Unknown(override val id: Permission.Id) : UsesPermission() {
        override val status: Status = Status.UNKNOWN
    }
}

val UsesPermission.isGranted: Boolean
    get() = status == UsesPermission.Status.GRANTED

fun Pkg.getPermissionUses(id: Permission.Id): UsesPermission =
    (this as? ReadableApk)?.requestedPermissions
        ?.filterIsInstance<UsesPermission.WithState>()
        ?.singleOrNull { it.id == id }
        ?: UsesPermission.Unknown(id)