package eu.darken.myperm.permissions.ui.list

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import eu.darken.myperm.R
import eu.darken.myperm.permissions.core.types.BasePermission
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class PermsSortOptions(
    @Json(name = "mainSort") val mainSort: Sort = Sort.APPS_GRANTED,
    @Json(name = "reversed") val reversed: Boolean = false
) : Parcelable {

    @JsonClass(generateAdapter = false)
    enum class Sort(
        @StringRes val labelRes: Int,
    ) {
        APPS_GRANTED(
            labelRes = R.string.permissions_sort_apps_granted_label,
        ) {
            override fun getComparator(c: Context): Comparator<BasePermission> =
                Comparator.comparing<BasePermission, Int> { perm ->
                    perm.grantingPkgs.size
                }.reversed()
        },
        APPS_REQUESTED(
            labelRes = R.string.permissions_sort_apps_requested_label,
        ) {
            override fun getComparator(c: Context): Comparator<BasePermission> =
                Comparator.comparing<BasePermission, Int> { perm ->
                    perm.requestingPkgs.size
                }.reversed()
        },
        ;

        abstract fun getComparator(c: Context): Comparator<BasePermission>
    }
}