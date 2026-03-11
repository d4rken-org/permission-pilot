package eu.darken.myperm.common.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import eu.darken.myperm.common.room.entity.SnapshotEntity

@Dao
interface SnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(entity: SnapshotEntity)

    @Query("SELECT * FROM snapshots ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSnapshot(): SnapshotEntity?

    @Query("SELECT * FROM snapshots WHERE snapshotId = :id")
    suspend fun getSnapshotById(id: String): SnapshotEntity?

    @Query("SELECT COUNT(*) FROM snapshots")
    suspend fun getSnapshotCount(): Int

    @Query("SELECT snapshotId FROM snapshots ORDER BY createdAt DESC LIMIT -1 OFFSET :keepCount")
    suspend fun getOldSnapshotIds(keepCount: Int): List<String>

    @Query("DELETE FROM snapshots WHERE snapshotId IN (:ids)")
    suspend fun deleteSnapshots(ids: List<String>)
}
