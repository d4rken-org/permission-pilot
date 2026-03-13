package eu.darken.myperm.common.room

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eu.darken.myperm.common.room.dao.PendingSnapshotEventDao
import eu.darken.myperm.common.room.dao.PermissionChangeDao
import eu.darken.myperm.common.room.dao.SnapshotDao
import eu.darken.myperm.common.room.dao.SnapshotPkgDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RoomModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PermPilotDatabase = Room.databaseBuilder(
        context,
        PermPilotDatabase::class.java,
        "permpilot.db",
    )
        .addMigrations(MIGRATION_1_2)
        .build()

    @Provides
    fun snapshotDao(db: PermPilotDatabase): SnapshotDao = db.snapshotDao()

    @Provides
    fun snapshotPkgDao(db: PermPilotDatabase): SnapshotPkgDao = db.snapshotPkgDao()

    @Provides
    fun permissionChangeDao(db: PermPilotDatabase): PermissionChangeDao = db.permissionChangeDao()

    @Provides
    fun pendingSnapshotEventDao(db: PermPilotDatabase): PendingSnapshotEventDao = db.pendingSnapshotEventDao()
}
