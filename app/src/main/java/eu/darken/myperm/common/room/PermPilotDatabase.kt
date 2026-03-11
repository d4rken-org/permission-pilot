package eu.darken.myperm.common.room

import androidx.room.Database
import androidx.room.RoomDatabase
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.room.entity.SnapshotEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity

@Database(
    entities = [
        SnapshotEntity::class,
        SnapshotPkgEntity::class,
        SnapshotPkgPermEntity::class,
        SnapshotPkgDeclaredPermEntity::class,
        PermissionChangeEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PermPilotDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
    abstract fun snapshotPkgDao(): SnapshotPkgDao
    abstract fun permissionChangeDao(): PermissionChangeDao
}
