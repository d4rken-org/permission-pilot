package eu.darken.myperm.watcher.ui.dashboard

import android.os.Parcelable
import androidx.annotation.StringRes
import eu.darken.myperm.R
import eu.darken.myperm.watcher.core.WatcherEventType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class WatcherFilterOptions(
    @SerialName("keys") val filters: Set<Filter> = emptySet()
) : Parcelable {

    @Serializable
    enum class Group(@StringRes val labelRes: Int) {
        EVENT_TYPE(R.string.watcher_filter_section_event_type),
        PERMISSION_CHANGES(R.string.watcher_filter_section_permission_changes),
        STATUS(R.string.watcher_filter_section_status),
    }

    @Serializable
    enum class Filter(
        val group: Group,
        @StringRes val labelRes: Int,
    ) {
        INSTALL(Group.EVENT_TYPE, R.string.watcher_event_install),
        UPDATE(Group.EVENT_TYPE, R.string.watcher_event_update),
        REMOVED(Group.EVENT_TYPE, R.string.watcher_event_removed),
        GRANT_CHANGE(Group.EVENT_TYPE, R.string.watcher_event_grant_change),
        HAS_ADDED_PERMISSIONS(Group.PERMISSION_CHANGES, R.string.watcher_filter_has_added_permissions),
        HAS_LOST_PERMISSIONS(Group.PERMISSION_CHANGES, R.string.watcher_filter_has_lost_permissions),
        HAS_GAINED_PERMISSIONS(Group.PERMISSION_CHANGES, R.string.watcher_filter_has_gained_permissions),
        UNSEEN_ONLY(Group.STATUS, R.string.watcher_filter_unseen_only);

        fun matches(item: WatcherReportItem): Boolean = when (this) {
            INSTALL -> item.eventType == WatcherEventType.INSTALL
            UPDATE -> item.eventType == WatcherEventType.UPDATE
            REMOVED -> item.eventType == WatcherEventType.REMOVED
            GRANT_CHANGE -> item.eventType == WatcherEventType.GRANT_CHANGE
            HAS_ADDED_PERMISSIONS -> item.hasAddedPermissions
            HAS_LOST_PERMISSIONS -> item.hasLostPermissions
            HAS_GAINED_PERMISSIONS -> item.gainedCount > 0
            UNSEEN_ONLY -> !item.isSeen
        }
    }

    fun matches(item: WatcherReportItem): Boolean {
        if (filters.isEmpty()) return true
        return filters.groupBy { it.group }.all { (_, filters) ->
            filters.any { it.matches(item) }
        }
    }
}
