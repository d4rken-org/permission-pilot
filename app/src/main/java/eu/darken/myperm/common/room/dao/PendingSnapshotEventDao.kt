package eu.darken.myperm.common.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import eu.darken.myperm.common.room.entity.PendingSnapshotEventEntity

@Dao
interface PendingSnapshotEventDao {

    @Insert
    suspend fun insert(event: PendingSnapshotEventEntity)

    @Query("SELECT * FROM pending_snapshot_events ORDER BY id ASC")
    suspend fun getAll(): List<PendingSnapshotEventEntity>

    @Query("DELETE FROM pending_snapshot_events WHERE id <= :maxId")
    suspend fun deleteByMaxId(maxId: Long)

    @Query("SELECT MIN(createdAt) FROM pending_snapshot_events")
    suspend fun getOldestCreatedAt(): Long?

    @Query("DELETE FROM pending_snapshot_events WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
