package eu.darken.myperm.apps.ui.list

import android.os.Parcelable
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.room.entity.PkgType
import eu.darken.myperm.common.room.snapshot.DisplayableApp
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AppsFilterOptions(
    @SerialName("filters") val keys: Set<Filter> = setOf(Filter.USER_APP)
) : Parcelable {

    @Serializable
    enum class Filter(
        @StringRes val labelRes: Int,
        val matches: (DisplayableApp) -> Boolean
    ) {
        SYSTEM_APP(
            labelRes = R.string.apps_filter_systemapps_label,
            matches = { it.isSystemApp }
        ),
        USER_APP(
            labelRes = R.string.apps_filter_userapps_label,
            matches = { !it.isSystemApp }
        ),
        GOOGLE_PLAY(
            labelRes = R.string.apps_filter_gplay_label,
            matches = { app ->
                !app.isSystemApp
                        && app.allInstallerPkgNames.any { it == AKnownPkg.GooglePlay.id.pkgName }
            }
        ),
        OEM_STORE(
            labelRes = R.string.apps_filter_oemstore_label,
            matches = { app ->
                val oemPkgNames = AKnownPkg.OEM_STORES.map { it.id.pkgName }.toSet()
                !app.isSystemApp && app.allInstallerPkgNames.any { it in oemPkgNames }
            }
        ),
        SIDELOADED(
            labelRes = R.string.apps_filter_sideloaded_label,
            matches = { app ->
                val storePkgNames = AKnownPkg.APP_STORES.map { it.id.pkgName }.toSet()
                !app.isSystemApp && app.allInstallerPkgNames.none { it in storePkgNames }
            }
        ),
        NO_INTERNET(
            labelRes = R.string.apps_filter_nointernet_label,
            matches = { it.internetAccess != "DIRECT" && it.internetAccess != "UNKNOWN" }
        ),
        SHARED_ID(
            labelRes = R.string.apps_filter_sharedid_label,
            matches = { it.siblingCount > 0 }
        ),
        MULTI_PROFILE(
            labelRes = R.string.apps_filter_multipleprofiles_label,
            matches = { it.twinCount > 0 }
        ),
        PRIMARY_PROFILE(
            labelRes = R.string.apps_filter_profile_active_label,
            matches = { it.pkgType == PkgType.PRIMARY.name }
        ),
        SECONDARY_PROFILE(
            labelRes = R.string.apps_filter_profile_secondary_label,
            matches = { it.pkgType == PkgType.SECONDARY_PROFILE.name }
        ),
        BATTERY_OPTIMIZATION(
            labelRes = R.string.apps_filter_battery_optimization_label,
            matches = { it.batteryOptimization != "MANAGED_BY_SYSTEM" }
        ),
        ACCESSIBILITY(
            labelRes = R.string.apps_filter_accessibility_label,
            matches = { it.hasAccessibilityServices }
        )
        ;
    }
}
