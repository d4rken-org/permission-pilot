package eu.darken.myperm.apps.core

import androidx.annotation.DrawableRes
import eu.darken.myperm.apps.core.types.Pkg

enum class AndroidPkgs(
    override val id: Pkg.Id,
    val label: String? = null,
    @DrawableRes val iconRes: Int? = null,
) : Pkg {

    ANDROID(
        id = Pkg.Id("android"),
    ),
    ;
}