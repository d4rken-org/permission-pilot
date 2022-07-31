package eu.darken.myperm.apps.core.features

interface PermissionState {
    enum class Status {
        GRANTED,
        GRANTED_IN_USE,
        DENIED,
        UNKNOWN,
    }

    val status: Status
}

val UsesPermission.isGranted: Boolean
    get() = this is PermissionState && status == PermissionState.Status.GRANTED