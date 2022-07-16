package eu.darken.myperm.permissions.core.types

import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.permissions.core.Permission

sealed class BasePermission : Permission {

    abstract val label: String?
    abstract val description: String?
    abstract val requestingPkgs: Collection<BaseApp>
    abstract val grantingPkgs: Collection<BaseApp>
    abstract val declaringPkgs: Collection<BaseApp>

}