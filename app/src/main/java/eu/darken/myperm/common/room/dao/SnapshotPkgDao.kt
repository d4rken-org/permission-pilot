package eu.darken.myperm.common.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotPkgDao {

    @Insert
    suspend fun insertPkgs(entities: List<SnapshotPkgEntity>)

    @Insert
    suspend fun insertPermissions(entities: List<SnapshotPkgPermEntity>)

    @Insert
    suspend fun insertDeclaredPermissions(entities: List<SnapshotPkgDeclaredPermEntity>)

    @Query("SELECT * FROM snapshot_pkgs WHERE snapshotId = :snapshotId")
    suspend fun getPkgsForSnapshot(snapshotId: String): List<SnapshotPkgEntity>

    @Query("SELECT * FROM snapshot_pkg_perms WHERE snapshotId = :snapshotId")
    suspend fun getPermsForSnapshot(snapshotId: String): List<SnapshotPkgPermEntity>

    @Query("SELECT * FROM snapshot_pkg_declared_perms WHERE snapshotId = :snapshotId")
    suspend fun getDeclaredPermsForSnapshot(snapshotId: String): List<SnapshotPkgDeclaredPermEntity>

    @Query("SELECT pkgName, userHandleId, COUNT(*) as declaredCount FROM snapshot_pkg_declared_perms WHERE snapshotId = :snapshotId GROUP BY pkgName, userHandleId")
    suspend fun getDeclaredPermCountsForSnapshot(snapshotId: String): List<DeclaredPermCount>

    data class DeclaredPermCount(
        val pkgName: String,
        val userHandleId: Int,
        val declaredCount: Int,
    )

    @Query("SELECT * FROM snapshot_pkg_perms WHERE snapshotId = :snapshotId AND pkgName = :pkgName AND userHandleId = :userHandleId")
    suspend fun getPermsForPkg(snapshotId: String, pkgName: String, userHandleId: Int): List<SnapshotPkgPermEntity>

    @Query("SELECT * FROM snapshot_pkg_declared_perms WHERE snapshotId = :snapshotId AND pkgName = :pkgName AND userHandleId = :userHandleId")
    suspend fun getDeclaredPermsForPkg(snapshotId: String, pkgName: String, userHandleId: Int): List<SnapshotPkgDeclaredPermEntity>

    @Query("SELECT * FROM snapshot_pkgs WHERE snapshotId = :snapshotId AND pkgName = :pkgName AND userHandleId = :userHandleId")
    suspend fun getPkgByName(snapshotId: String, pkgName: String, userHandleId: Int): SnapshotPkgEntity?

    @Query("DELETE FROM snapshot_pkgs WHERE snapshotId = :snapshotId AND pkgName = :pkgName AND userHandleId = :userHandleId")
    suspend fun deletePkg(snapshotId: String, pkgName: String, userHandleId: Int)

    @Query("SELECT * FROM snapshot_pkgs WHERE snapshotId = :snapshotId")
    fun observePkgsForSnapshot(snapshotId: String): Flow<List<SnapshotPkgEntity>>

    @Query("SELECT * FROM snapshot_pkg_perms WHERE snapshotId = :snapshotId")
    fun observePermsForSnapshot(snapshotId: String): Flow<List<SnapshotPkgPermEntity>>

    @Query("SELECT pkgName, userHandleId, COUNT(*) as declaredCount FROM snapshot_pkg_declared_perms WHERE snapshotId = :snapshotId GROUP BY pkgName, userHandleId")
    fun observeDeclaredPermCountsForSnapshot(snapshotId: String): Flow<List<DeclaredPermCount>>
}
