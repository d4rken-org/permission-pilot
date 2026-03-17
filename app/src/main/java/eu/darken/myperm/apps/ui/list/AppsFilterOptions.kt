package eu.darken.myperm.apps.ui.list

import android.os.Parcelable
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.room.entity.PkgType
import eu.darken.myperm.apps.core.AppInfo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
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
        @SerialName("SYSTEM_APP")
        SYSTEM_APP(
            group = Group.APP_TYPE,
            labelRes = R.string.apps_filter_systemapps_label,
            matches = { it.isSystemApp }
        ),
        @SerialName("USER_APP")
        USER_APP(
            group = Group.APP_TYPE,
            labelRes = R.string.apps_filter_userapps_label,
            matches = { !it.isSystemApp }
        ),
        @SerialName("GOOGLE_PLAY")
        GOOGLE_PLAY(
            group = Group.INSTALL_SOURCE,
            labelRes = R.string.apps_filter_gplay_label,
            matches = { app ->
                !app.isSystemApp
                        && app.allInstallerPkgNames.any { it == AKnownPkg.GooglePlay.id.pkgName }
            }
        ),
        @SerialName("OEM_STORE")
        OEM_STORE(
            group = Group.INSTALL_SOURCE,
            labelRes = R.string.apps_filter_oemstore_label,
            matches = { app ->
                val oemPkgNames = AKnownPkg.OEM_STORES.map { it.id.pkgName }.toSet()
                !app.isSystemApp && app.allInstallerPkgNames.any { it in oemPkgNames }
            }
        ),
        @SerialName("SIDELOADED")
        SIDELOADED(
            group = Group.INSTALL_SOURCE,
            labelRes = R.string.apps_filter_sideloaded_label,
            matches = { app ->
                val storePkgNames = AKnownPkg.APP_STORES.map { it.id.pkgName }.toSet()
                !app.isSystemApp && app.allInstallerPkgNames.none { it in storePkgNames }
            }
        ),
        @SerialName("NO_INTERNET")
        NO_INTERNET(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_nointernet_label,
            matches = { it.internetAccess != InternetAccess.DIRECT && it.internetAccess != InternetAccess.UNKNOWN }
        ),
        @SerialName("SHARED_ID")
        SHARED_ID(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_sharedid_label,
            matches = { it.siblingCount > 0 }
        ),
        @SerialName("MULTI_PROFILE")
        MULTI_PROFILE(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_multipleprofiles_label,
            matches = { it.twinCount > 0 }
        ),
        @SerialName("BATTERY_OPTIMIZATION")
        BATTERY_OPTIMIZATION(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_battery_optimization_label,
            matches = { it.batteryOptimization != BatteryOptimization.MANAGED_BY_SYSTEM }
        ),
        @SerialName("ACCESSIBILITY")
        ACCESSIBILITY(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_accessibility_label,
            matches = { it.hasAccessibilityServices }
        ),
        @SerialName("DEVICE_ADMIN")
        DEVICE_ADMIN(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_device_admin_label,
            matches = { it.hasDeviceAdmin }
        ),
        @SerialName("INSTALL_PACKAGES")
        INSTALL_PACKAGES(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_install_packages_label,
            matches = { app ->
                app.requestedPermissions.any {
                    it.permissionId == "android.permission.REQUEST_INSTALL_PACKAGES" && it.status.isGranted
                }
            }
        ),
        @SerialName("OVERLAY")
        OVERLAY(
            group = Group.PROPERTIES,
            labelRes = R.string.apps_filter_overlay_label,
            matches = { app ->
                app.requestedPermissions.any {
                    it.permissionId == "android.permission.SYSTEM_ALERT_WINDOW" && it.status.isGranted
                }
            }
        ),
        @SerialName("PRIMARY_PROFILE")
        PRIMARY_PROFILE(
            group = Group.PROFILE,
            labelRes = R.string.apps_filter_profile_active_label,
            matches = { it.pkgType == PkgType.PRIMARY }
        ),
        @SerialName("SECONDARY_PROFILE")
        SECONDARY_PROFILE(
            group = Group.PROFILE,
            labelRes = R.string.apps_filter_profile_secondary_label,
            matches = { it.pkgType == PkgType.SECONDARY_PROFILE }
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
