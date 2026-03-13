package eu.darken.myperm.common.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "snapshots",
    indices = [Index("createdAt")],
)
data class SnapshotEntity(
    @PrimaryKey val snapshotId: String,
    val createdAt: Long,
    val triggerReason: String,
    val pkgCount: Int,
    val durationMs: Long,
)
