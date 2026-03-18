package eu.darken.myperm.common.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import eu.darken.myperm.apps.core.manifest.ManifestHintEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ManifestHintDao {

    @Query("SELECT * FROM manifest_hints")
    fun observeAll(): Flow<List<ManifestHintEntity>>

    @Query("SELECT * FROM manifest_hints")
    suspend fun getAll(): List<ManifestHintEntity>

    @Upsert
    suspend fun upsertHints(hints: List<ManifestHintEntity>)

    @Query(
        """DELETE FROM manifest_hints WHERE pkgName NOT IN (
            SELECT pkgName FROM snapshot_pkgs WHERE snapshotId = (
                SELECT snapshotId FROM snapshots ORDER BY createdAt DESC LIMIT 1
            )
        )"""
    )
    suspend fun pruneStale()
}
