package eu.darken.myperm.permissions.core.features

import eu.darken.myperm.permissions.core.Permission
import eu.darken.myperm.permissions.core.known.toKnownPermission

interface Highlighted

val Permission.isHighlighted
    get() = this is Highlighted || this.id.toKnownPermission() is Highlighted