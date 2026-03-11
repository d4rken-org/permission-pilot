package eu.darken.myperm.common.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity

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

    @Query("SELECT * FROM snapshot_pkg_perms WHERE snapshotId = :snapshotId AND pkgName = :pkgName AND userHandleId = :userHandleId")
    suspend fun getPermsForPkg(snapshotId: String, pkgName: String, userHandleId: Int): List<SnapshotPkgPermEntity>

    @Query("SELECT * FROM snapshot_pkg_declared_perms WHERE snapshotId = :snapshotId AND pkgName = :pkgName AND userHandleId = :userHandleId")
    suspend fun getDeclaredPermsForPkg(snapshotId: String, pkgName: String, userHandleId: Int): List<SnapshotPkgDeclaredPermEntity>

    @Query("SELECT * FROM snapshot_pkgs WHERE snapshotId = :snapshotId AND pkgName = :pkgName AND userHandleId = :userHandleId")
    suspend fun getPkgByName(snapshotId: String, pkgName: String, userHandleId: Int): SnapshotPkgEntity?
}
