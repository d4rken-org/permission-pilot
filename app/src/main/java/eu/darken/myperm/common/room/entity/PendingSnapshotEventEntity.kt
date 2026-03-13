package eu.darken.myperm.common.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_snapshot_events",
    indices = [Index("packageName", "userHandleId")],
)
data class PendingSnapshotEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val eventType: String,
    val userHandleId: Int,
    val createdAt: Long,
)
