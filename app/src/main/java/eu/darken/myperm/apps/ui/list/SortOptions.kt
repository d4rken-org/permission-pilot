package eu.darken.myperm.apps.ui.list

import android.content.Context
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.ApkPkg
import eu.darken.myperm.apps.core.features.InstalledApp
import eu.darken.myperm.apps.core.tryLabel
import java.time.Instant

data class SortOptions(
    val mainSort: Sort = Sort.UPDATED_AT,
    val reversed: Boolean = false
) {
    enum class Sort(
        @StringRes val labelRes: Int
    ) {

        PERMISSIONS_GRANTED(
            labelRes = R.string.apps_sort_permissions_granted_label,
        ) {
            override fun getComparator(c: Context): Comparator<ApkPkg> = Comparator.comparing<ApkPkg, Int> { app ->
                app.requestedPermissions.count { it.isGranted }
            }.reversed()
        },
        PERMISSIONS_REQUESTED(
            labelRes = R.string.apps_sort_permissions_requested_label,
        ) {
            override fun getComparator(c: Context): Comparator<ApkPkg> = Comparator.comparing<ApkPkg, Int> { app ->
                app.requestedPermissions.size
            }.reversed()
        },
        PERMISSIONS_DECLARED(
            labelRes = R.string.apps_sort_permissions_declared_label,
        ) {
            override fun getComparator(c: Context): Comparator<ApkPkg> = Comparator.comparing<ApkPkg, Int> { app ->
                app.declaredPermissions.size
            }.reversed()
        },
        APP_NAME(
            labelRes = R.string.apps_sort_app_name_label,
        ) {
            override fun getComparator(c: Context): Comparator<ApkPkg> = Comparator.comparing {
                it.tryLabel(c) ?: ""
            }
        },
        INSTALLED_AT(
            labelRes = R.string.apps_sort_install_date_label,
        ) {
            override fun getComparator(c: Context): Comparator<ApkPkg> = Comparator.comparing<ApkPkg, Instant> {
                (it as? InstalledApp)?.installedAt
            }.reversed()
        },
        UPDATED_AT(
            labelRes = R.string.apps_sort_update_date_label
        ) {
            override fun getComparator(c: Context): Comparator<ApkPkg> = Comparator.comparing<ApkPkg, Instant> {
                (it as? InstalledApp)?.updatedAt
            }.reversed()
        }
        ;

        abstract fun getComparator(c: Context): Comparator<ApkPkg>
    }
}