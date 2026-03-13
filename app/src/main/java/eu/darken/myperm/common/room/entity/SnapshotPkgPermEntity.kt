package eu.darken.myperm.common.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
    val pkgName: String,
    val userHandleId: Int,
    val permissionId: String,
    val status: String,
)
