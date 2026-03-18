package eu.darken.myperm.apps.ui.list

import android.os.Parcelable
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.apps.core.AppInfo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Parcelize
@Serializable
data class AppsSortOptions(
    val mainSort: Sort = Sort.UPDATED_AT,
    val reversed: Boolean = false
) : Parcelable {
    @Serializable
    enum class Sort(
        @StringRes val labelRes: Int,
        val showInSheet: Boolean = true,
    ) {

        @SerialName("PERMISSIONS_GRANTED")
        PERMISSIONS_GRANTED(
            labelRes = R.string.apps_sort_permissions_granted_label,
        ) {
            override fun getComparator(): Comparator<AppInfo> = Comparator.comparing<AppInfo, Int> { app ->
                app.requestedPermissions.count { it.status.isGranted }
            }.reversed()
        },
        @SerialName("PERMISSIONS_REQUESTED")
        PERMISSIONS_REQUESTED(
            labelRes = R.string.apps_sort_permissions_requested_label,
        ) {
            override fun getComparator(): Comparator<AppInfo> = Comparator.comparing<AppInfo, Int> { app ->
                app.requestedPermissions.size
            }.reversed()
        },
        @SerialName("PERMISSIONS_DECLARED")
        PERMISSIONS_DECLARED(
            labelRes = R.string.apps_sort_permissions_declared_label,
            showInSheet = false,
        ) {
            override fun getComparator(): Comparator<AppInfo> = Comparator.comparing<AppInfo, Int> { app ->
                app.declaredPermissionCount
            }.reversed()
        },
        @SerialName("APP_NAME")
        APP_NAME(
            labelRes = R.string.apps_sort_app_name_label,
        ) {
            override fun getComparator(): Comparator<AppInfo> = Comparator.comparing {
                it.label
            }
        },
        @SerialName("INSTALLED_AT")
        INSTALLED_AT(
            labelRes = R.string.apps_sort_install_date_label,
        ) {
            override fun getComparator(): Comparator<AppInfo> = Comparator.comparing<AppInfo, Instant> {
                it.installedAt ?: Instant.MIN
            }.reversed()
        },
        @SerialName("UPDATED_AT")
        UPDATED_AT(
            labelRes = R.string.apps_sort_update_date_label
        ) {
            override fun getComparator(): Comparator<AppInfo> = Comparator.comparing<AppInfo, Instant> {
                it.updatedAt ?: Instant.MIN
            }.reversed()
        },
        @SerialName("INSTALL_SOURCE")
        INSTALL_SOURCE(
            labelRes = R.string.apps_sort_install_source_label
        ) {
            override fun getComparator(): Comparator<AppInfo> = Comparator.comparing<AppInfo, String> { app ->
                if (app.allInstallerPkgNames.isEmpty()) return@comparing "Z"

                val gplayPkgName = AKnownPkg.GooglePlay.id.pkgName
                if (gplayPkgName in app.allInstallerPkgNames) return@comparing "A$gplayPkgName"

                val oemPkgNames = AKnownPkg.OEM_STORES.map { it.id.pkgName }.toSet()
                val byOem = app.allInstallerPkgNames.firstOrNull { it in oemPkgNames }
                if (byOem != null) return@comparing "B$byOem"

                return@comparing "C" + app.allInstallerPkgNames.joinToString()
            }
        }
        ;

        abstract fun getComparator(): Comparator<AppInfo>
    }
}
