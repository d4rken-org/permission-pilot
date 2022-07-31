package eu.darken.myperm.apps.core.features

import eu.darken.myperm.permissions.core.Permission

interface UsesPermission {
    val id: Permission.Id
}