package eu.darken.myperm.permissions.core.container

import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.permissions.core.Permission

sealed class BasePermission : Permission {

    abstract val requestingPkgs: Collection<BasePkg>
    abstract val grantingPkgs: Collection<BasePkg>
    abstract val declaringPkgs: Collection<BasePkg>

}