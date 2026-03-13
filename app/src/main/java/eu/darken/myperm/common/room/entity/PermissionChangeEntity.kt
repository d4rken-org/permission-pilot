package eu.darken.myperm.common.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "permission_change_reports",
    indices = [Index("isSeen", "detectedAt")],
)
data class PermissionChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val userHandleId: Int,
    val appLabel: String?,
    val versionCode: Long,
    val versionName: String?,
    val eventType: String,
    val changesJson: String,
    val detectedAt: Long,
    val isSeen: Boolean = false,
)
