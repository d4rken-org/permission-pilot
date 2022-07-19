package eu.darken.myperm.permissions.core.types

import eu.darken.myperm.apps.core.features.HasApkData
import eu.darken.myperm.permissions.core.Permission

sealed class BasePermission : Permission {

    abstract val requestingPkgs: Collection<HasApkData>
    abstract val grantingPkgs: Collection<HasApkData>
    abstract val declaringPkgs: Collection<HasApkData>

}