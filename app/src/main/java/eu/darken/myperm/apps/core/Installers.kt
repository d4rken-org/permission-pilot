package eu.darken.myperm.apps.core

import eu.darken.myperm.apps.core.types.Pkg

enum class Installers(
    override val id: Pkg.Id,
    val label: String?
) : Pkg {

    GOOGLE_PLAY(
        id = Pkg.Id("com.android.vending"),
        label = "Google Play Store"
    )
}