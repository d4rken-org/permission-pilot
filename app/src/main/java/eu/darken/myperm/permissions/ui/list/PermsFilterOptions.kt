package eu.darken.myperm.permissions.ui.list

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.features.HasInstallData
import eu.darken.myperm.permissions.core.container.BasePermission
import kotlinx.parcelize.Parcelize

@Parcelize

@JsonClass(generateAdapter = true)
data class PermsFilterOptions(
    @Json(name = "filters") val keys: Set<Filter> = setOf(Filter.CORE)
) : Parcelable {

    @JsonClass(generateAdapter = false)
    enum class Filter(
        @StringRes val labelRes: Int,
        val matches: (BasePermission) -> Boolean
    ) {
        CORE(
            labelRes = R.string.permissions_filter_system_core_label,
            matches = { it.declaringPkgs.any { pkg -> pkg.id == eu.darken.myperm.apps.core.known.AKnownPkg.AndroidSystem.id } }
        ),
        SYSTEM(
            labelRes = R.string.permissions_filter_system_extra_label,
            matches = { pkg -> pkg.declaringPkgs.any { it is HasInstallData && it.isSystemApp } }
        ),
        USER(
            labelRes = R.string.permissions_filter_custom_label,
            matches = { pkg -> pkg.declaringPkgs.none { it is HasInstallData && it.isSystemApp } }
        ),
        ;
    }
}