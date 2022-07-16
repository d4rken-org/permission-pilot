package eu.darken.myperm.apps.ui.list

import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.types.BaseApp
import java.time.Instant

data class SortOptions(
    val mainSort: Sort = Sort.UPDATED_AT,
    val reversed: Boolean = false
) {
    enum class Sort(
        @StringRes val labelRes: Int,
        val comparator: Comparator<BaseApp>
    ) {
        PERMISSIONS_GRANTED(
            labelRes = R.string.apps_sort_permissions_granted_label,
            comparator = Comparator.comparing<BaseApp?, Int?> { app ->
                app.requestedPermissions.count { it.isGranted }
            }.reversed()
        ),
        PERMISSIONS_REQUESTED(
            labelRes = R.string.apps_sort_permissions_requested_label,
            comparator = Comparator.comparing<BaseApp?, Int?> { app ->
                app.requestedPermissions.size
            }.reversed()
        ),
        PERMISSIONS_DECLARED(
            labelRes = R.string.apps_sort_permissions_declared_label,
            comparator = Comparator.comparing<BaseApp?, Int?> { app ->
                app.declaredPermissions.size
            }.reversed()
        ),
        APP_NAME(
            labelRes = R.string.apps_sort_app_name_label,
            comparator = Comparator.comparing { it.label ?: "" }
        ),
        INSTALLED_AT(
            labelRes = R.string.apps_sort_install_date_label,
            comparator = Comparator.comparing<BaseApp, Instant> { it.installedAt }.reversed()
        ),
        UPDATED_AT(
            labelRes = R.string.apps_sort_update_date_label,
            comparator = Comparator.comparing<BaseApp, Instant> { it.updatedAt }.reversed()
        ),
        ;
    }
}