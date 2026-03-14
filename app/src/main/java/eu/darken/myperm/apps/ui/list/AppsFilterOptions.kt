package eu.darken.myperm.apps.ui.list

import android.os.Parcelable
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.room.entity.PkgType
import eu.darken.myperm.apps.core.AppInfo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AppsFilterOptions(
    val filters: Set<Filter> = setOf(Filter.USER_APP)
) : Parcelable {

    enum class Group(@StringRes val labelRes: Int) {
        APP_TYPE(R.string.apps_filter_section_app_type),
        INSTALL_SOURCE(R.string.apps_filter_section_install_source),
        PROPERTIES(R.string.apps_filter_section_properties),
        PROFILE(R.string.apps_filter_section_profile),
    }

    @Serializable
    enum class Filter(
        val group: Group,
        @StringRes val labelRes: Int,
        val matches: (AppInfo) -> Boolean
    ) {
        SYSTEM_APP(
            group = Group.APP_TYPE,
            labelRes = R.string.apps_filter_systemapps_label,
            matches = { it.isSystemApp }
        ),
        USER_APP(
            group = Group.APP_TYPE,
            labelRes = R.string.apps_filter_userapps_label,
            matches = { !it.isSystemApp }
        ),
        GOOGLE_PLAY(
            group = Group.INSTALL_SOURCE,
            labelRes = R.string.apps_filter_gplay_label,
            matches = { app ->
                !app.isSystemApp
                        && app.allInstallerPkgNames.any { it == AKnownPkg.GooglePlay.id.pkgName }
            }
        ),
        OEM_STORE(
            group = Group.INSTALL_SOURCE,
            labelRes = R.string.apps_filter_oemstore_label,
            matches = { app ->
                val oemPkgNames = AKnownPkg.OEM_STORES.map { it.id.pkgName }.toSet()
                !app.isSystemApp && app.allInstallerPkgNames.any { it in oemPkgNames }
            }
        ),
        SIDELOADED(
            group = Group.INSTALL_SOURCE,
            labelRes = R.string.apps_filter_sideloaded_label,
            matches = { app ->
                val storePkgNames = AKnownPkg.APP_STORES.map { it.id.pkgName }.toSet()
                !app.isSystemApp && app.allInstallerPkgNames.none { it in storePkgNames }
            }
        ),
        NO_INTERNET(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_nointernet_label,
            matches = { it.internetAccess != "DIRECT" && it.internetAccess != "UNKNOWN" }
        ),
        SHARED_ID(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_sharedid_label,
            matches = { it.siblingCount > 0 }
        ),
        MULTI_PROFILE(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_multipleprofiles_label,
            matches = { it.twinCount > 0 }
        ),
        BATTERY_OPTIMIZATION(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_battery_optimization_label,
            matches = { it.batteryOptimization != "MANAGED_BY_SYSTEM" }
        ),
        ACCESSIBILITY(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_accessibility_label,
            matches = { it.hasAccessibilityServices }
        ),
        PRIMARY_PROFILE(
            group = Group.PROFILE,
            labelRes = R.string.apps_filter_profile_active_label,
            matches = { it.pkgType == PkgType.PRIMARY.name }
        ),
        SECONDARY_PROFILE(
            group = Group.PROFILE,
            labelRes = R.string.apps_filter_profile_secondary_label,
            matches = { it.pkgType == PkgType.SECONDARY_PROFILE.name }
        ),
        ;
    }

    fun matches(app: AppInfo): Boolean {
        if (filters.isEmpty()) return true
        return filters.groupBy { it.group }.all { (_, groupFilters) ->
            groupFilters.any { it.matches(app) }
        }
    }
}
