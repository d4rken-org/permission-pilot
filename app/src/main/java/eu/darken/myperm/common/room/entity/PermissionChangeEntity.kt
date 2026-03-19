package eu.darken.myperm.common.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.watcher.core.WatcherEventType

@Entity(
    tableName = "permission_change_reports",
    indices = [
        Index("isSeen", "detectedAt"),
        Index("packageName", "userHandleId", "sourceSnapshotId", unique = true),
    ],
)
data class PermissionChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: Pkg.Name,
    val userHandleId: Int,
    val appLabel: String?,
    val versionCode: Long,
    val versionName: String?,
    val eventType: WatcherEventType,
    val changesJson: String,
    val previousVersionCode: Long? = null,
    val previousVersionName: String? = null,
    val detectedAt: Long,
    val isSeen: Boolean = false,
    val sourceSnapshotId: String? = null,
)
