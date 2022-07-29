package eu.darken.myperm.apps.ui.list

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.container.SecondaryProfilePkg
import eu.darken.myperm.apps.core.features.Installed
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.known.AKnownPkg
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class AppsFilterOptions(
    @Json(name = "filters") val keys: Set<Filter> = setOf(Filter.USER_APP)
) : Parcelable {

    @JsonClass(generateAdapter = false)
    enum class Filter(
        @StringRes val labelRes: Int,
        val matches: (Pkg) -> Boolean
    ) {
        SYSTEM_APP(
            labelRes = R.string.apps_filter_systemapps_label,
            matches = { it is Installed && it.isSystemApp }
        ),
        USER_APP(
            labelRes = R.string.apps_filter_userapps_label,
            matches = { it is Installed && !it.isSystemApp }
        ),
        GOOGLE_PLAY(
            labelRes = R.string.apps_filter_gplay_label,
            matches = { pkg ->
                pkg is Installed
                        && !pkg.isSystemApp
                        && pkg.installerInfo.allInstallers.any { it.id == AKnownPkg.GooglePlay.id }
            }
        ),
        OEM_STORE(
            labelRes = R.string.apps_filter_oemstore_label,
            matches = { pkg ->
                pkg is Installed && !pkg.isSystemApp && pkg.installerInfo.allInstallers.any { installer ->
                    AKnownPkg.OEM_STORES.map { it.id }.contains(installer.id)
                }
            }
        ),
        SIDELOADED(
            labelRes = R.string.apps_filter_sideloaded_label,
            matches = { pkg ->
                pkg is Installed && !pkg.isSystemApp && pkg.installerInfo.allInstallers.none { installer ->
                    AKnownPkg.APP_STORES.map { it.id }.contains(installer.id)
                }
            }
        ),
        NO_INTERNET(
            labelRes = R.string.apps_filter_nointernet_label,
            matches = {
                it is Installed
                        && it.internetAccess != InternetAccess.DIRECT
                        && it.internetAccess != InternetAccess.UNKNOWN
            }
        ),
        SHARED_ID(
            labelRes = R.string.apps_filter_sharedid_label,
            matches = { it is Installed && it.siblings.isNotEmpty() }
        ),
        MULTI_PROFILE(
            labelRes = R.string.apps_filter_multipleprofiles_label,
            matches = { it is Installed && (it.twins.isNotEmpty()) }
        ),
        PRIMARY_PROFILE(
            labelRes = R.string.apps_filter_profile_active_label,
            matches = { it is eu.darken.myperm.apps.core.container.PrimaryProfilePkg }
        ),
        SECONDARY_PROFILE(
            labelRes = R.string.apps_filter_profile_secondary_label,
            matches = { it is SecondaryProfilePkg }
        )
        ;
    }
}
