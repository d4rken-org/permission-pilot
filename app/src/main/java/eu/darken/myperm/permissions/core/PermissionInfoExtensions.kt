package eu.darken.myperm.permissions.core

import android.content.pm.PermissionInfo

val PermissionInfo.id: Permission.Id
    get() = Permission.Id(name)