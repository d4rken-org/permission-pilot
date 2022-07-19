package eu.darken.myperm.apps.ui.list

import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.HasInstallData
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.known.AKnownPkg

data class FilterOptions(
    val keys: Set<Filter> = setOf(Filter.USER_APP)
) {
    enum class Filter(
        @StringRes val labelRes: Int,
        val matches: (Pkg) -> Boolean
    ) {
        SYSTEM_APP(
            labelRes = R.string.apps_filter_systemapps_label,
            matches = { it is HasInstallData && it.isSystemApp }
        ),
        USER_APP(
            labelRes = R.string.apps_filter_userapps_label,
            matches = { it is HasInstallData && !it.isSystemApp }
        ),
        NO_INTERNET(
            labelRes = R.string.apps_filter_nointernet_label,
            matches = { it is HasInstallData && it.internetAccess != InternetAccess.DIRECT }
        ),
        GOOGLE_PLAY(
            labelRes = R.string.apps_filter_gplay_label,
            matches = { pkg ->
                pkg is HasInstallData
                        && !pkg.isSystemApp
                        && pkg.installerInfo.allInstallers.any { it.id == AKnownPkg.GooglePlay.id }
            }
        ),
        OEM_STORE(
            labelRes = R.string.apps_filter_oemstore_label,
            matches = { pkg ->
                pkg is HasInstallData && !pkg.isSystemApp && pkg.installerInfo.allInstallers.any { installer ->
                    AKnownPkg.OEM_STORES.map { it.id }.contains(installer.id)
                }
            }
        ),
        SIDELOADED(
            labelRes = R.string.apps_filter_sideloaded_label,
            matches = { pkg ->
                pkg is HasInstallData && !pkg.isSystemApp && pkg.installerInfo.allInstallers.none { installer ->
                    AKnownPkg.APP_STORES.map { it.id }.contains(installer.id)
                }
            }
        ),
        SHARED_ID(
            labelRes = R.string.apps_filter_sharedid_label,
            matches = { it is HasInstallData && it.siblings.isNotEmpty() }
        ),
        MULTI_PROFILE(
            labelRes = R.string.apps_filter_multipleprofiles_label,
            matches = { it is HasInstallData && (it.twins.isNotEmpty() || it is eu.darken.myperm.apps.core.container.ProfilePkg) }
        )
        ;
    }
}