package eu.darken.myperm.permissions.core

import android.content.pm.PermissionInfo

val PermissionInfo.id: PermissionId
    get() = PermissionId(name)