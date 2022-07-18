package eu.darken.myperm.apps.core.features

import eu.darken.myperm.apps.core.Pkg

interface AppStore : Pkg {

    val urlGenerator: ((Pkg.Id) -> String)?
        get() = null
}