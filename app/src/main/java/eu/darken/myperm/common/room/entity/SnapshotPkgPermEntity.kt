package eu.darken.myperm.common.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import eu.darken.myperm.apps.core.Pkg

@Entity(
    tableName = "snapshot_pkg_perms",
    primaryKeys = ["snapshotId", "pkgName", "userHandleId", "permissionId"],
    foreignKeys = [
        ForeignKey(
            entity = SnapshotPkgEntity::class,
            parentColumns = ["snapshotId", "pkgName", "userHandleId"],
            childColumns = ["snapshotId", "pkgName", "userHandleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("snapshotId", "pkgName", "userHandleId")],
)
data class SnapshotPkgPermEntity(
    val snapshotId: String,
    val pkgName: Pkg.Name,
    val userHandleId: Int,
    val permissionId: String,
    val status: String,
)
