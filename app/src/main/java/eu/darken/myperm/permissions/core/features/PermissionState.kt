package eu.darken.myperm.permissions.core.features

import eu.darken.myperm.permissions.core.Permission

interface PermissionState {
    enum class Status {
        GRANTED,
        GRANTED_IN_USE,
        DENIED,
        UNKNOWN,
    }

    val status: Status
}

val Permission.isGranted: Boolean
    get() = this is PermissionState && status == PermissionState.Status.GRANTED