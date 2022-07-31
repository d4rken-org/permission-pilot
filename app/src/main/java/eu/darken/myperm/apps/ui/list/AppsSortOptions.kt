package eu.darken.myperm.apps.ui.list

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.Installed
import eu.darken.myperm.apps.core.features.ReadableApk
import eu.darken.myperm.apps.core.features.isGranted
import eu.darken.myperm.apps.core.known.AKnownPkg
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Parcelize
@JsonClass(generateAdapter = true)
data class AppsSortOptions(
    @Json(name = "mainSort") val mainSort: Sort = Sort.UPDATED_AT,
    @Json(name = "reversed") val reversed: Boolean = false
) : Parcelable {
    @JsonClass(generateAdapter = false)
    enum class Sort(
        @StringRes val labelRes: Int
    ) {

        PERMISSIONS_GRANTED(
            labelRes = R.string.apps_sort_permissions_granted_label,
        ) {
            override fun getComparator(c: Context): Comparator<Pkg> = Comparator.comparing<Pkg, Int> { app ->
                (app as? ReadableApk)?.requestedPermissions?.count { it.isGranted } ?: 0
            }.reversed()
        },
        PERMISSIONS_REQUESTED(
            labelRes = R.string.apps_sort_permissions_requested_label,
        ) {
            override fun getComparator(c: Context): Comparator<Pkg> = Comparator.comparing<Pkg, Int> { app ->
                (app as? ReadableApk)?.requestedPermissions?.size ?: 0
            }.reversed()
        },
        PERMISSIONS_DECLARED(
            labelRes = R.string.apps_sort_permissions_declared_label,
        ) {
            override fun getComparator(c: Context): Comparator<Pkg> = Comparator.comparing<Pkg, Int> { app ->
                (app as? ReadableApk)?.declaredPermissions?.size ?: 0
            }.reversed()
        },
        APP_NAME(
            labelRes = R.string.apps_sort_app_name_label,
        ) {
            override fun getComparator(c: Context): Comparator<Pkg> = Comparator.comparing {
                it.getLabel(c) ?: ""
            }
        },
        INSTALLED_AT(
            labelRes = R.string.apps_sort_install_date_label,
        ) {
            override fun getComparator(c: Context): Comparator<Pkg> = Comparator.comparing<Pkg, Instant> {
                (it as? Installed)?.installedAt ?: Instant.MIN
            }.reversed()
        },
        UPDATED_AT(
            labelRes = R.string.apps_sort_update_date_label
        ) {
            override fun getComparator(c: Context): Comparator<Pkg> = Comparator.comparing<Pkg, Instant> {
                (it as? Installed)?.updatedAt ?: Instant.MIN
            }.reversed()
        },
        INSTALL_SOURCE(
            labelRes = R.string.apps_sort_install_source_label
        ) {
            override fun getComparator(c: Context): Comparator<Pkg> = Comparator.comparing<Pkg, String> { pkg ->
                if (pkg !is Installed) return@comparing "ZZ"
                if (pkg.installerInfo.allInstallers.isEmpty()) return@comparing "Z"

                val byGplay = pkg.installerInfo.allInstallers.any { it.id == AKnownPkg.GooglePlay.id }
                if (byGplay) return@comparing "A" + AKnownPkg.GooglePlay.id.pkgName

                val oemIds = AKnownPkg.OEM_STORES.map { it.id }
                val byOem = pkg.installerInfo.allInstallers.firstOrNull { oemIds.contains(it.id) }
                if (byOem != null) return@comparing "B" + byOem.packageName


                return@comparing "C" + pkg.installerInfo.allInstallers.joinToString()
            }
        }
        ;

        abstract fun getComparator(c: Context): Comparator<Pkg>
    }
}