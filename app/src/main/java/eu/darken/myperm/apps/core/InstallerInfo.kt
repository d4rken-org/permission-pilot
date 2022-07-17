package eu.darken.myperm.apps.core

import eu.darken.myperm.apps.core.types.Pkg

data class InstallerInfo(
    val initiatingPkg: Pkg
) {
    fun tryKnownInstaller(): KnownInstaller? = KnownInstaller.values().singleOrNull { it.id == initiatingPkg.id }
}