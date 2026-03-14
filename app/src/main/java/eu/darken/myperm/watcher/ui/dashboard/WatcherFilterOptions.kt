package eu.darken.myperm.watcher.ui.dashboard

import android.os.Parcelable
import androidx.annotation.StringRes
import eu.darken.myperm.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class WatcherFilterOptions(
    val keys: Set<Filter> = emptySet()
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
        val matches: (WatcherReportItem) -> Boolean,
    ) {
        INSTALL(Group.EVENT_TYPE, R.string.watcher_event_install, { it.eventType == "INSTALL" }),
        UPDATE(Group.EVENT_TYPE, R.string.watcher_event_update, { it.eventType == "UPDATE" }),
        REMOVED(Group.EVENT_TYPE, R.string.watcher_event_removed, { it.eventType == "REMOVED" }),
        GRANT_CHANGE(Group.EVENT_TYPE, R.string.watcher_event_grant_change, { it.eventType == "GRANT_CHANGE" }),
        HAS_ADDED_PERMISSIONS(Group.PERMISSION_CHANGES, R.string.watcher_filter_has_added_permissions, { it.hasAddedPermissions }),
        HAS_LOST_PERMISSIONS(Group.PERMISSION_CHANGES, R.string.watcher_filter_has_lost_permissions, { it.hasLostPermissions }),
        UNSEEN_ONLY(Group.STATUS, R.string.watcher_filter_unseen_only, { !it.isSeen }),
    }

    fun matches(item: WatcherReportItem): Boolean {
        if (keys.isEmpty()) return true
        return keys.groupBy { it.group }.all { (_, filters) ->
            filters.any { it.matches(item) }
        }
    }
}
