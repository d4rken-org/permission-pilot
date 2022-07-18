package eu.darken.myperm.permissions.core.types

import eu.darken.myperm.apps.core.features.ApkPkg
import eu.darken.myperm.permissions.core.Permission

sealed class BasePermission : Permission {

    abstract val requestingPkgs: Collection<ApkPkg>
    abstract val grantingPkgs: Collection<ApkPkg>
    abstract val declaringPkgs: Collection<ApkPkg>

}