package eu.darken.myperm.apps.core.container

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.Installed
import eu.darken.myperm.apps.core.features.ReadableApk

sealed class BasePkg : Pkg, ReadableApk, Installed

fun BasePkg.isOrHasProfiles() = twins.isNotEmpty() || this is SecondaryProfilePkg