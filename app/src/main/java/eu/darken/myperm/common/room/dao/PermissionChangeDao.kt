package eu.darken.myperm.common.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionChangeDao {

    @Insert
    suspend fun insert(entity: PermissionChangeEntity): Long

    @Query("SELECT * FROM permission_change_reports WHERE id = :id")
    suspend fun getById(id: Long): PermissionChangeEntity?

    @Query("SELECT * FROM permission_change_reports ORDER BY detectedAt DESC")
    fun getAll(): Flow<List<PermissionChangeEntity>>

    @Query("SELECT * FROM permission_change_reports WHERE isSeen = 0 ORDER BY detectedAt DESC")
    fun getUnseen(): Flow<List<PermissionChangeEntity>>

    @Query("SELECT COUNT(*) FROM permission_change_reports")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM permission_change_reports WHERE isSeen = 0")
    fun getUnseenCount(): Flow<Int>

    @Query("UPDATE permission_change_reports SET isSeen = 1 WHERE id = :id")
    suspend fun markSeen(id: Long)

    @Query("UPDATE permission_change_reports SET isSeen = 1")
    suspend fun markAllSeen()

    @Query("DELETE FROM permission_change_reports WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM permission_change_reports")
    suspend fun deleteAll()

    @Query("DELETE FROM permission_change_reports WHERE detectedAt < :epochMs")
    suspend fun deleteOlderThan(epochMs: Long)
}
