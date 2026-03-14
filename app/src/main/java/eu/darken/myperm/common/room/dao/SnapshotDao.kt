package eu.darken.myperm.common.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import eu.darken.myperm.common.room.entity.SnapshotEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM snapshots WHERE createdAt > (SELECT createdAt FROM snapshots WHERE snapshotId = :afterSnapshotId) ORDER BY createdAt ASC")
    suspend fun getSnapshotsAfter(afterSnapshotId: String): List<SnapshotEntity>

    @Query("SELECT snapshotId FROM snapshots WHERE createdAt < (SELECT createdAt FROM snapshots WHERE snapshotId = :anchorId)")
    suspend fun getSnapshotIdsBefore(anchorId: String): List<String>

    @Query("UPDATE snapshots SET pkgCount = :newCount WHERE snapshotId = :snapshotId")
    suspend fun updatePkgCount(snapshotId: String, newCount: Int)

    @Query("SELECT snapshotId FROM snapshots ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestSnapshotId(): Flow<String?>
}
