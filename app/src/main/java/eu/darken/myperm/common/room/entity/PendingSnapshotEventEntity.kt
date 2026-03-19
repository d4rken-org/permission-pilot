package eu.darken.myperm.common.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.watcher.core.WatcherEventType

@Entity(
    tableName = "pending_snapshot_events",
    indices = [Index("packageName", "userHandleId")],
)
data class PendingSnapshotEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: Pkg.Name,
    val eventType: WatcherEventType,
    val userHandleId: Int,
    val createdAt: Long,
)
