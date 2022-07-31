package eu.darken.myperm.permissions.ui.list

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.permissions.core.container.BasePermission
import eu.darken.myperm.permissions.core.features.HasTags
import eu.darken.myperm.permissions.core.known.toKnownPermission
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class PermsFilterOptions(
    @Json(name = "filters") val keys: Set<Filter> = setOf(
        Filter.MANIFEST,
        Filter.SYSTEM,
        Filter.RUNTIME,
        Filter.SPECIAL_ACCESS
    )
) : Parcelable {

    @JsonClass(generateAdapter = false)
    enum class Filter(
        @StringRes val labelRes: Int,
        val matches: (BasePermission) -> Boolean
    ) {
        MANIFEST(
            labelRes = R.string.permissions_filter_manifest_label,
            matches = { it.declaringPkgs.any { pkg -> pkg.id == AKnownPkg.AndroidSystem.id } }
        ),
        SYSTEM(
            labelRes = R.string.permissions_filter_system_label,
            matches = { pkg -> pkg.declaringPkgs.any { it.isSystemApp } }
        ),
        USER(
            labelRes = R.string.permissions_filter_user_label,
            matches = { pkg -> pkg.declaringPkgs.none { it.isSystemApp } }
        ),

        RUNTIME(
            labelRes = R.string.permissions_filter_runtime_label,
            matches = { it is HasTags || it.id.toKnownPermission() is HasTags }
        ),
        DEFAULT_GRANTED(
            labelRes = R.string.permissions_filter_default_label,
            matches = { it is HasTags || it.id.toKnownPermission() is HasTags }
        ),
        SPECIAL_ACCESS(
            labelRes = R.string.permissions_filter_special_label,
            matches = { it is HasTags || it.id.toKnownPermission() is HasTags }
        );
    }
}