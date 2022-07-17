package eu.darken.myperm.apps.core

import androidx.annotation.DrawableRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.types.Pkg

enum class KnownInstaller(
    override val id: Pkg.Id,
    val label: String?,
    @DrawableRes val iconRes: Int,
    val urlGenerator: (Pkg.Id) -> String,
) : Pkg {

    GOOGLE_PLAY(
        id = Pkg.Id("com.android.vending"),
        label = "Google Play Store",
        iconRes = R.drawable.ic_baseline_gplay_24,
        urlGenerator = { pkgId ->
            "https://play.google.com/store/apps/details?id=${pkgId.value}"
        }
    )
}