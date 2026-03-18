package eu.darken.myperm.common.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import eu.darken.myperm.apps.core.features.BatteryOptimization
import eu.darken.myperm.apps.core.features.InternetAccess
import eu.darken.myperm.apps.core.manifest.ManifestHintEntity
import eu.darken.myperm.common.room.dao.ManifestHintDao
import eu.darken.myperm.common.room.dao.PendingSnapshotEventDao
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import eu.darken.myperm.common.room.entity.PendingSnapshotEventEntity
import eu.darken.myperm.common.room.entity.PermissionChangeEntity
import eu.darken.myperm.common.room.entity.PkgType
import eu.darken.myperm.common.room.entity.SnapshotEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgDeclaredPermEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgEntity
import eu.darken.myperm.common.room.entity.SnapshotPkgPermEntity
import eu.darken.myperm.watcher.core.WatcherEventType

@TypeConverters(
    WatcherEventType.Converter::class,
    InternetAccess.Converter::class,
    BatteryOptimization.Converter::class,
    PkgType.Converter::class,
)
@Database(
    entities = [
        SnapshotEntity::class,
        SnapshotPkgEntity::class,
        SnapshotPkgPermEntity::class,
        SnapshotPkgDeclaredPermEntity::class,
        PermissionChangeEntity::class,
        PendingSnapshotEventEntity::class,
        ManifestHintEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PermPilotDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
    abstract fun snapshotPkgDao(): SnapshotPkgDao
    abstract fun permissionChangeDao(): PermissionChangeDao
    abstract fun pendingSnapshotEventDao(): PendingSnapshotEventDao
    abstract fun manifestHintDao(): ManifestHintDao

    open suspend fun <R> inTransaction(block: suspend () -> R): R = withTransaction(block)
}
